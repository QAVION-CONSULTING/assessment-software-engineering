import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ProjectsPage from "../pages/ProjectsPage";
import { ApiError } from "../api/endpoints";
import type { ProjectResponse } from "../api/endpoints";
import { renderWithProviders } from "./utils";

vi.mock("../api/endpoints", async (importOriginal) => {
  const mod = await importOriginal<typeof import("../api/endpoints")>();
  return {
    ...mod,
    ApiError: mod.ApiError,
    listProjects: vi.fn(),
    createProject: vi.fn(),
    updateProject: vi.fn(),
    deleteProject: vi.fn(),
  };
});

import { createProject, listProjects } from "../api/endpoints";

const mocked = vi.mocked;

const project: ProjectResponse = {
  id: 1,
  name: "Website Relaunch",
  timeBudgetMinutes: 600,
  costBudgetCents: 100000,
  hourlyRateCents: 8000,
  version: 3,
  recordedMinutes: 120,
  recordedCostCents: 16000,
  timeBudgetRatio: 0.2,
  costBudgetRatio: 0.16,
  timeBudgetWarning: false,
  timeBudgetExceeded: false,
  costBudgetWarning: false,
  costBudgetExceeded: false,
};

beforeEach(() => {
  vi.clearAllMocks();
  mocked(listProjects).mockResolvedValue([project]);
});

const openCreateModal = async (user: ReturnType<typeof userEvent.setup>) => {
  await user.click(screen.getByRole("button", { name: "New project" }));
  await screen.findByLabelText(/^Name/); 
};

describe("ProjectsPage", () => {
  it("create flow: submits converted payloads (hours→minutes, EUR→cents) and reloads", async () => {
    const user = userEvent.setup();
    mocked(createProject).mockResolvedValue(project);

    renderWithProviders(<ProjectsPage />);
    await screen.findAllByText("Website Relaunch");

    await openCreateModal(user);
    await user.type(screen.getByLabelText(/^Name/), "New Project");
    await user.type(screen.getByLabelText(/^Time budget/), "10");
    await user.type(screen.getByLabelText(/^Cost budget/), "1000");
    await user.type(screen.getByLabelText(/^Hourly rate/), "80");
    await user.click(screen.getByRole("button", { name: "Create" }));

    await waitFor(() => {
      expect(createProject).toHaveBeenCalledWith({
        name: "New Project",
        description: null,
        timeBudgetMinutes: 600,
        costBudgetCents: 100000,
        hourlyRateCents: 8000,
      });
    });
    expect(listProjects).toHaveBeenCalledTimes(2);
  });

  it("error surfaces: ApiError message + details[] shown in the modal", async () => {
    const user = userEvent.setup();
    mocked(createProject).mockRejectedValue(
      new ApiError(422, { status: 422, error: "Validation failed", message: "Invalid project", details: ["Name already exists"] }),
    );

    renderWithProviders(<ProjectsPage />);
    await screen.findAllByText("Website Relaunch");

    await openCreateModal(user);
    await user.type(screen.getByLabelText(/^Name/), "Dup");
    await user.type(screen.getByLabelText(/^Time budget/), "10");
    await user.type(screen.getByLabelText(/^Cost budget/), "1000");
    await user.type(screen.getByLabelText(/^Hourly rate/), "80");
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText("Name already exists")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Create" })).toBeInTheDocument();
  });

  it("validation: empty name / zero budgets → field errors, no API call", async () => {
    const user = userEvent.setup();

    renderWithProviders(<ProjectsPage />);
    await screen.findAllByText("Website Relaunch");

    await openCreateModal(user);
    await user.click(screen.getByRole("button", { name: "Create" }));

    expect(await screen.findByText("Name is required")).toBeInTheDocument();
    expect(screen.getAllByText("Must be greater than 0")).toHaveLength(3);
    expect(createProject).not.toHaveBeenCalled();
  });
});
