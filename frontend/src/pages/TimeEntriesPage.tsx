import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  Alert,
  Button,
  Group,
  Loader,
  Modal,
  NativeSelect,
  NumberInput,
  Paper,
  Select,
  Stack,
  Table,
  Text,
  TextInput,
  Title,
} from "@mantine/core";
import { DateInput } from "@mantine/dates";
import { useForm } from "@mantine/form";
import dayjs from "dayjs";
import {
  ApiError,
  createTimeEntry,
  deleteTimeEntry,
  listProjects,
  listTimeEntries,
  updateTimeEntry,
  type ProjectResponse,
  type TimeEntryResponse,
} from "../api/endpoints";

/** minutes -> "7,5 h" */
const fmtHours = (minutes: number) => `${(minutes / 60).toLocaleString("de-DE")} h`;

type EntryModal = { mode: "create" } | { mode: "edit"; entry: TimeEntryResponse } | null;

interface EntryFormValues {
  date: string | null;
  projectId: string;
  durationHours: number | string;
  description: string;
}

const toFormValues = (e?: TimeEntryResponse): EntryFormValues => ({
  date: e ? e.date : dayjs().format("YYYY-MM-DD"),
  projectId: e ? String(e.projectId) : "",
  durationHours: e ? e.durationMinutes / 60 : "",
  description: e?.description ?? "",
});

export default function TimeEntriesPage() {
  const [entries, setEntries] = useState<TimeEntryResponse[]>([]);
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [filterDate, setFilterDate] = useState<string | null>(null);
  const [filterProject, setFilterProject] = useState<string | null>(null);

  const [modal, setModal] = useState<EntryModal>(null);
  const [deleting, setDeleting] = useState<TimeEntryResponse | null>(null);
  const [saving, setSaving] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);

  const form = useForm<EntryFormValues>({
    mode: "uncontrolled",
    validate: {
      date: (v) => (v ? null : "Date is required"),
      projectId: (v) => (v ? null : "Project is required"),
      durationHours: (v) => {
        const n = Number(v);
        if (!n || n <= 0) return "Must be greater than 0";
        if (n > 24) return "A single entry cannot exceed 24 h";
        return null;
      },
    },
  });


  const abortRef = useRef<AbortController | null>(null);

  const reload = useCallback(async () => {
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;
    try {
      setError(null);
      const [entriesData, projectsData] = await Promise.all([
        listTimeEntries(
          {
            date: filterDate ?? undefined,
            projectId: filterProject ? Number(filterProject) : undefined,
          },
          controller.signal,
        ),
        listProjects(controller.signal),
      ]);
      if (controller.signal.aborted) return; 
      setEntries(entriesData);
      setProjects(projectsData);
      setLoading(false);
    } catch (e) {
      if (controller.signal.aborted) return;
      setError(e instanceof Error ? e.message : String(e));
      setLoading(false);
    }
  }, [filterDate, filterProject]);

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

  const openEdit = (e: TimeEntryResponse) => {
    form.setValues(toFormValues(e));
    form.clearErrors();
    setModalError(null);
    setModal({ mode: "edit", entry: e });
  };

  const projectById = useMemo(
    () => new Map(projects.map((p) => [p.id, p])),
    [projects],
  );

  const projectOptions = useMemo(
    () => projects.map((p) => ({ value: String(p.id), label: p.name })),
    [projects],
  );

  const handleSubmit = form.onSubmit(async (values) => {
    setSaving(true);
    setModalError(null);
    const body = {
      date: values.date!,
      projectId: Number(values.projectId),
      durationMinutes: Math.round(Number(values.durationHours) * 60),
      description: values.description.trim() || null,
    };
    try {
      if (modal?.mode === "edit") {
        await updateTimeEntry(modal.entry.id, modal.entry.version, body);
      } else {
        await createTimeEntry(body);
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
      await deleteTimeEntry(deleting.id);
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
        <Title order={2}>Time Entries</Title>
        <Button onClick={openCreate} disabled={projects.length === 0}>
          New entry
        </Button>
      </Group>

      <Group gap="sm" wrap="nowrap">
        <DateInput
          value={filterDate}
          onChange={setFilterDate}
          clearable
          placeholder="Filter by date"
          maxDate={dayjs().format("YYYY-MM-DD")}
          flex="1 1 220px"
          maw={260}
        />
        <Select
          value={filterProject}
          onChange={setFilterProject}
          data={projectOptions}
          placeholder="Filter by project"
          clearable
          flex="1 1 220px"
          maw={260}
        />
      </Group>

      {error && (
        <Alert color="red" title="Error" withCloseButton onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Loader />
      ) : entries.length === 0 ? (
        <Text c="dimmed">
          {projects.length === 0
            ? "Create a project first, then record time against it."
            : "No time entries match the current filters."}
        </Text>
      ) : (
        <>
          {/* Narrow screens: compact entry cards */}
          <Stack gap="sm" hiddenFrom="md">
            {entries.map((e) => (
              <Paper key={e.id} withBorder p="md" radius="md">
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                  <div style={{ minWidth: 0 }}>
                    <Text fw={600}>{e.projectName}</Text>
                    <Text size="sm" c="dimmed">
                      {e.date}
                    </Text>
                  </div>
                  <Text fw={600} style={{ whiteSpace: "nowrap" }}>
                    {fmtHours(e.durationMinutes)}
                  </Text>
                </Group>
                {e.description && (
                  <Text size="sm" mt="xs" lineClamp={2}>
                    {e.description}
                  </Text>
                )}
                <Group gap="xs" mt="md">
                  <Button variant="light" size="xs" onClick={() => openEdit(e)}>
                    Edit
                  </Button>
                  <Button variant="light" color="red" size="xs" onClick={() => setDeleting(e)}>
                    Delete
                  </Button>
                </Group>
              </Paper>
            ))}
          </Stack>

          {/* Wide screens: compact table */}
          <Table.ScrollContainer minWidth={560} visibleFrom="md">
            <Table highlightOnHover verticalSpacing="sm">
              <Table.Thead>
                <Table.Tr>
                  <Table.Th>Date</Table.Th>
                  <Table.Th>Project</Table.Th>
                  <Table.Th>Duration</Table.Th>
                  <Table.Th>Description</Table.Th>
                  <Table.Th style={{ textAlign: "right" }}>Actions</Table.Th>
                </Table.Tr>
              </Table.Thead>
              <Table.Tbody>
                {entries.map((e) => (
                  <Table.Tr key={e.id}>
                    <Table.Td style={{ whiteSpace: "nowrap" }}>{e.date}</Table.Td>
                    <Table.Td>
                      <Text style={{ whiteSpace: "nowrap" }}>{e.projectName}</Text>
                    </Table.Td>
                    <Table.Td style={{ whiteSpace: "nowrap" }}>{fmtHours(e.durationMinutes)}</Table.Td>
                    <Table.Td>
                      <Text size="sm" lineClamp={1}>
                        {e.description ?? "—"}
                      </Text>
                    </Table.Td>
                    <Table.Td>
                      <Group gap="xs" justify="flex-end" wrap="nowrap">
                        <Button variant="light" size="xs" onClick={() => openEdit(e)}>
                          Edit
                        </Button>
                        <Button variant="light" color="red" size="xs" onClick={() => setDeleting(e)}>
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
        title={modal?.mode === "edit" ? "Edit time entry" : "New time entry"}
      >
        <form onSubmit={handleSubmit}>
          <Stack>
            <DateInput
              label="Date"
              value={form.values.date}
              onChange={(v) => form.setFieldValue("date", v)}
              valueFormat="YYYY-MM-DD"
              required
              maxDate={dayjs().format("YYYY-MM-DD")}
              clearable={false}
            />
            <NativeSelect
              label="Project"
              required
              data={[{ value: "", label: "Select project…", disabled: true }, ...projectOptions]}
              key={form.key("projectId")}
              {...form.getInputProps("projectId")}
            />
            <NumberInput
              label="Duration (hours)"
              required
              min={0.25}
              max={24}
              step={0.25}
              key={form.key("durationHours")}
              {...form.getInputProps("durationHours")}
            />
            <TextInput
              label="Description"
              key={form.key("description")}
              {...form.getInputProps("description")}
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
      <Modal opened={deleting !== null} onClose={() => setDeleting(null)} title="Delete time entry" size="sm">
        <Stack>
          <Text>
            Delete the {fmtHours(deleting?.durationMinutes ?? 0)} entry for{" "}
            <strong>{deleting ? projectById.get(deleting.projectId)?.name ?? deleting.projectName : ""}</strong> on{" "}
            {deleting?.date}?
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
