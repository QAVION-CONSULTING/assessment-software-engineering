import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import TimeEntriesPage from "../pages/TimeEntriesPage";
import type { ProjectResponse, TimeEntryResponse } from "../api/endpoints";
import { renderWithProviders } from "./utils";

vi.mock("../api/endpoints", async (importOriginal) => {
  const mod = await importOriginal<typeof import("../api/endpoints")>();
  return {
    ...mod,
    listProjects: vi.fn(),
    listTimeEntries: vi.fn(),
    createTimeEntry: vi.fn(),
    updateTimeEntry: vi.fn(),
    deleteTimeEntry: vi.fn(),
  };
});

import { listProjects, listTimeEntries } from "../api/endpoints";

const mocked = vi.mocked;

const project: ProjectResponse = {
  id: 1,
  name: "Website Relaunch",
  timeBudgetMinutes: 600,
  costBudgetCents: 100000,
  hourlyRateCents: 8000,
  version: 1,
  recordedMinutes: 0,
  recordedCostCents: 0,
  timeBudgetRatio: 0,
  costBudgetRatio: 0,
  timeBudgetWarning: false,
  timeBudgetExceeded: false,
  costBudgetWarning: false,
  costBudgetExceeded: false,
};

const entry: TimeEntryResponse = {
  id: 1,
  date: "2026-10-16",
  projectId: 1,
  projectName: "Website Relaunch",
  durationMinutes: 480,
  version: 1,
  description: "Day 1",
};

beforeEach(() => {
  vi.clearAllMocks();
  mocked(listProjects).mockResolvedValue([project]);
  mocked(listTimeEntries).mockResolvedValue([entry]);
});

describe("TimeEntriesPage", () => {
  it("filters drive the query: date/project filter → matching params; clearing drops them", async () => {
    const user = userEvent.setup();
    renderWithProviders(<TimeEntriesPage />);
    await screen.findAllByText("Website Relaunch");

    // initial load: no filters
    expect(listTimeEntries).toHaveBeenLastCalledWith({}, expect.anything());

    // set project filter.
    await user.click(screen.getByPlaceholderText("Filter by project"));
    const option = await waitFor(() => {
      const el = document.querySelector<HTMLElement>("[role='option']");
      expect(el).not.toBeNull();
      return el!;
    });
    await user.click(option);
    await waitFor(() => {
      expect(listTimeEntries).toHaveBeenLastCalledWith(
        { projectId: 1 },
        expect.anything(),
      );
    });

    // clear filter → params dropped.
    const filterInput = screen.getByPlaceholderText("Filter by project");
    const clearButton = filterInput
      .closest(".mantine-InputWrapper-root")!
      .querySelector<HTMLButtonElement>("button")!;
    await user.click(clearButton);
    await waitFor(() => {
      expect(listTimeEntries).toHaveBeenLastCalledWith({}, expect.anything());
    });
  });

  it("validation: duration ≤ 0 or > 24 h → field error, no API call", async () => {
    const user = userEvent.setup();
    renderWithProviders(<TimeEntriesPage />);
    await screen.findAllByText("Website Relaunch");

    await user.click(screen.getByRole("button", { name: "New entry" }));
    await screen.findByLabelText(/^Date/);

    // duration 0 → error
    const duration = screen.getByLabelText(/^Duration/);
    await user.type(duration, "0");
    await user.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByText("Must be greater than 0")).toBeInTheDocument();

    // duration 25 → error
    await user.clear(duration);
    await user.type(duration, "25");
    await user.click(screen.getByRole("button", { name: "Create" }));
    expect(await screen.findByText("A single entry cannot exceed 24 h")).toBeInTheDocument();
  });
});
