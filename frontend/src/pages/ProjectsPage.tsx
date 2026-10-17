import { useCallback, useEffect, useRef, useState } from "react";
import {
  Alert,
  Badge,
  Button,
  Group,
  Loader,
  Modal,
  NumberInput,
  Paper,
  Progress,
  Stack,
  Table,
  Text,
  TextInput,
  Textarea,
  Title,
} from "@mantine/core";
import { useForm } from "@mantine/form";
import {
  ApiError,
  createProject,
  deleteProject,
  listProjects,
  updateProject,
  type ProjectResponse,
} from "../api/endpoints";

/** cents -> "1,234.56 €" */
export const fmtEur = (cents: number) =>
  (cents / 100).toLocaleString("de-DE", { style: "currency", currency: "EUR" });

/** minutes -> "82,5 h" */
export const fmtHours = (minutes: number) => `${(minutes / 60).toLocaleString("de-DE")} h`;

/** progress bar percentage, rounded to 1 decimal */
const pct = (used: number, total: number) => Math.round((used / total) * 1000) / 10;

interface ProjectFormValues {
  name: string;
  description: string;
  timeBudgetHours: number | string;
  costBudgetEur: number | string;
  hourlyRateEur: number | string;
}

const toFormValues = (p?: ProjectResponse): ProjectFormValues => ({
  name: p?.name ?? "",
  description: p?.description ?? "",
  timeBudgetHours: p ? p.timeBudgetMinutes / 60 : "",
  costBudgetEur: p ? p.costBudgetCents / 100 : "",
  hourlyRateEur: p ? p.hourlyRateCents / 100 : "",
});

type ProjectModal = { mode: "create" } | { mode: "edit"; project: ProjectResponse } | null;

function budgetBadge(p: ProjectResponse) {
  if (p.timeBudgetExceeded || p.costBudgetExceeded) {
    return <Badge color="red">exceeded</Badge>;
  }
  if (p.timeBudgetWarning || p.costBudgetWarning) {
    return <Badge color="yellow">≥ 80 %</Badge>;
  }
  return <Badge color="green">ok</Badge>;
}

function budgetColor(p: ProjectResponse, time: boolean) {
  const exceeded = time ? p.timeBudgetExceeded : p.costBudgetExceeded;
  const warning = time ? p.timeBudgetWarning : p.costBudgetWarning;
  return exceeded ? "red" : warning ? "yellow" : "green";
}

function BudgetRow({
  label,
  used,
  total,
  format,
  color,
}: {
  label: string;
  used: number;
  total: number;
  format: (n: number) => string;
  color: string;
}) {
  return (
    <div>
      <Group justify="space-between" gap="xs" mb={2} wrap="nowrap">
        <Text size="sm" c="dimmed">
          {label}
        </Text>
        <Text size="sm" style={{ whiteSpace: "nowrap" }}>
          {format(used)} / {format(total)}
        </Text>
      </Group>
      <Progress value={pct(used, total)} color={color} size="sm" radius="sm" />
    </div>
  );
}

function BudgetCell({
  used,
  total,
  format,
  color,
}: {
  used: number;
  total: number;
  format: (n: number) => string;
  color: string;
}) {
  return (
    <div style={{ minWidth: 140 }}>
      <Text size="sm" c={color} fw={color === "green" ? 400 : 600} style={{ whiteSpace: "nowrap" }} mb={2}>
        {format(used)} / {format(total)}
      </Text>
      <Progress value={pct(used, total)} color={color} size="sm" radius="sm" />
    </div>
  );
}

export default function ProjectsPage() {
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modal, setModal] = useState<ProjectModal>(null);
  const [deleting, setDeleting] = useState<ProjectResponse | null>(null);
  const [saving, setSaving] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const form = useForm<ProjectFormValues>({
    mode: "uncontrolled",
    validate: {
      name: (v) => (v.trim() ? null : "Name is required"),
      timeBudgetHours: (v) => (Number(v) > 0 ? null : "Must be greater than 0"),
      costBudgetEur: (v) => (Number(v) > 0 ? null : "Must be greater than 0"),
      hourlyRateEur: (v) => (Number(v) > 0 ? null : "Must be greater than 0"),
    },
  });


  const abortRef = useRef<AbortController | null>(null);

  const reload = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    try {
      setError(null);
      setProjects(await listProjects(controller.signal));
      if (!controller.signal.aborted) setLoading(false);
    } catch (e) {
      if (controller.signal.aborted) return;
      setError(e instanceof Error ? e.message : String(e));
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
    return () => abortRef.current?.abort();
  }, [reload]);

  const closeModal = () => {
    setModal(null);
    setModalError(null);
  };

  const openCreate = () => {
    form.setValues(toFormValues());
    form.clearErrors();
    setModalError(null);
    setModal({ mode: "create" });
  };

  const openEdit = (p: ProjectResponse) => {
    form.setValues(toFormValues(p));
    form.clearErrors();
    setModalError(null);
    setModal({ mode: "edit", project: p });
  };

  const handleSubmit = form.onSubmit(async (values) => {
    setSaving(true);
    setModalError(null);
    const body = {
      name: values.name.trim(),
      description: values.description.trim() || null,
      timeBudgetMinutes: Math.round(Number(values.timeBudgetHours) * 60),
      costBudgetCents: Math.round(Number(values.costBudgetEur) * 100),
      hourlyRateCents: Math.round(Number(values.hourlyRateEur) * 100),
    };
    try {
      if (modal?.mode === "edit") {
        await updateProject(modal.project.id, modal.project.version, body);
      } else {
        await createProject(body);
      }
      closeModal();
      await reload();
    } catch (e) {
      if (e instanceof ApiError) {
        setModalError(e.details.length ? e.details.join(" · ") : e.message);
      } else {
        setModalError(String(e));
      }
    } finally {
      setSaving(false);
    }
  });

  const handleDelete = async () => {
    if (!deleting) return;
    setSaving(true);
    try {
      await deleteProject(deleting.id);
      setDeleting(null);
      await reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setDeleting(null);
    } finally {
      setSaving(false);
    }
  };

  return (
    <Stack gap="md">
      <Group justify="space-between" wrap="nowrap">
        <Title order={2}>Projects</Title>
        <Button onClick={openCreate}>New project</Button>
      </Group>

      {error && (
        <Alert color="red" title="Error" withCloseButton onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Loader />
      ) : projects.length === 0 ? (
        <Text c="dimmed">No projects yet — create one to start tracking time.</Text>
      ) : (
        <>
          {/* Narrow screens: cards with budget progress bars */}
          <Stack gap="sm" hiddenFrom="md">
            {projects.map((p) => (
              <Paper key={p.id} withBorder p="md" radius="md">
                <Group justify="space-between" align="flex-start" mb="xs" wrap="nowrap">
                  <div style={{ minWidth: 0 }}>
                    <Text fw={600}>{p.name}</Text>
                    {p.description && (
                      <Text size="sm" c="dimmed" lineClamp={2}>
                        {p.description}
                      </Text>
                    )}
                  </div>
                  {budgetBadge(p)}
                </Group>
                <Stack gap="sm" mb="md">
                  <BudgetRow
                    label="Time"
                    used={p.recordedMinutes}
                    total={p.timeBudgetMinutes}
                    format={fmtHours}
                    color={budgetColor(p, true)}
                  />
                  <BudgetRow
                    label="Cost"
                    used={p.recordedCostCents}
                    total={p.costBudgetCents}
                    format={fmtEur}
                    color={budgetColor(p, false)}
                  />
                </Stack>
                <Group justify="space-between" wrap="nowrap">
                  <Text size="sm" c="dimmed">
                    {fmtEur(p.hourlyRateCents)}/h
                  </Text>
                  <Group gap="xs">
                    <Button variant="light" size="xs" onClick={() => openEdit(p)}>
                      Edit
                    </Button>
                    <Button variant="light" color="red" size="xs" onClick={() => setDeleting(p)}>
                      Delete
                    </Button>
                  </Group>
                </Group>
              </Paper>
            ))}
          </Stack>

          {/* Wide screens: compact table */}
          <Table.ScrollContainer minWidth={640} visibleFrom="md">
            <Table highlightOnHover verticalSpacing="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Project</Table.Th>
                  <Table.Th>Time</Table.Th>
                  <Table.Th>Cost</Table.Th>
                  <Table.Th>Rate</Table.Th>
                  <Table.Th>Status</Table.Th>
                  <Table.Th style={{ textAlign: "right" }}>Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {projects.map((p) => (
                  <Table.Tr key={p.id}>
                    <Table.Td>
                      <Text fw={500} style={{ whiteSpace: "nowrap" }}>
                        {p.name}
                      </Text>
                      {p.description && (
                        <Text size="sm" c="dimmed" lineClamp={1}>
                          {p.description}
                        </Text>
                      )}
                    </Table.Td>
                    <Table.Td>
                      <BudgetCell
                        used={p.recordedMinutes}
                        total={p.timeBudgetMinutes}
                        format={fmtHours}
                        color={budgetColor(p, true)}
                      />
                    </Table.Td>
                    <Table.Td>
                      <BudgetCell
                        used={p.recordedCostCents}
                        total={p.costBudgetCents}
                        format={fmtEur}
                        color={budgetColor(p, false)}
                      />
                    </Table.Td>
                    <Table.Td style={{ whiteSpace: "nowrap" }}>{fmtEur(p.hourlyRateCents)}/h</Table.Td>
                    <Table.Td>{budgetBadge(p)}</Table.Td>
                    <Table.Td>
                      <Group gap="xs" justify="flex-end" wrap="nowrap">
                        <Button variant="light" size="xs" onClick={() => openEdit(p)}>
                          Edit
                        </Button>
                        <Button variant="light" color="red" size="xs" onClick={() => setDeleting(p)}>
                          Delete
                        </Button>
                      </Group>
                    </Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Table.ScrollContainer>
        </>
      )}

      {/* Create / edit modal */}
      <Modal
        opened={modal !== null}
        onClose={closeModal}
        title={modal?.mode === "edit" ? `Edit project: ${modal.project.name}` : "New project"}
      >
        <form onSubmit={handleSubmit} noValidate>
          <Stack>
            <TextInput
              label="Name"
              required
              maxLength={100}
              key={form.key("name")}
              {...form.getInputProps("name")}
            />
            <Textarea
              label="Description"
              autosize
              minRows={2}
              key={form.key("description")}
              {...form.getInputProps("description")}
            />
            <NumberInput
              label="Time budget (hours)"
              required
              min={0.01}
              step={0.5}
              key={form.key("timeBudgetHours")}
              {...form.getInputProps("timeBudgetHours")}
            />
            <NumberInput
              label="Cost budget (EUR)"
              required
              min={0.01}
              step={10}
              key={form.key("costBudgetEur")}
              {...form.getInputProps("costBudgetEur")}
            />
            <NumberInput
              label="Hourly rate (EUR/h)"
              required
              min={0.01}
              step={1}
              key={form.key("hourlyRateEur")}
              {...form.getInputProps("hourlyRateEur")}
            />
            {modalError && <Alert color="red">{modalError}</Alert>}
            <Group justify="flex-end">
              <Button variant="default" onClick={closeModal}>
                Cancel
              </Button>
              <Button type="submit" loading={saving}>
                {modal?.mode === "edit" ? "Save" : "Create"}
              </Button>
            </Group>
          </Stack>
        </form>
      </Modal>

      {/* Delete confirmation */}
      <Modal opened={deleting !== null} onClose={() => setDeleting(null)} title="Delete project" size="sm">
        <Stack>
          <Text>
            Delete <strong>{deleting?.name}</strong>? All of its time entries will be deleted too.
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setDeleting(null)}>
              Cancel
            </Button>
            <Button color="red" loading={saving} onClick={handleDelete}>
              Delete
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Stack>
  );
}
