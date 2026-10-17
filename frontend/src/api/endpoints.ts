import { api } from "./client";
import type { components } from "./schema";

export type ProjectInput = components["schemas"]["ProjectInput"];
export type ProjectResponse = components["schemas"]["ProjectResponse"];
export type TimeEntryInput = components["schemas"]["TimeEntryInput"];
export type TimeEntryResponse = components["schemas"]["TimeEntryResponse"];
export type ErrorResponse = components["schemas"]["ErrorResponse"];

/** Thrown when the API returns a non-2xx response; carries the parsed error body. */
export class ApiError extends Error {
  readonly status: number;
  readonly details: string[];

  constructor(status: number, body: ErrorResponse | undefined) {
    super(body?.message ?? `Request failed with status ${status}`);
    this.name = "ApiError";
    this.status = status;
    this.details = body?.details ?? [];
  }
}

async function unwrap<T>(
  promise: Promise<{ data?: T; error?: unknown; response: Response }>,
): Promise<T> {
  const { data, error, response } = await promise;
  if (error !== undefined) {
    throw new ApiError(response.status, error as ErrorResponse);
  }
  return data as T;
}

// ---- Projects ----

export const listProjects = (signal?: AbortSignal) =>
  unwrap<ProjectResponse[]>(api.GET("/api/projects", { signal }));

export const getProject = (id: number) =>
  unwrap<ProjectResponse>(api.GET("/api/projects/{id}", { params: { path: { id } } }));

export const createProject = (body: ProjectInput) =>
  unwrap<ProjectResponse>(api.POST("/api/projects", { body }));

export const updateProject = (id: number, ifMatch: number, body: ProjectInput) =>
  unwrap<ProjectResponse>(
    api.PUT("/api/projects/{id}", { params: { path: { id }, header: { "If-Match": ifMatch } }, body }),
  );

export const deleteProject = (id: number) =>
  unwrap<void>(api.DELETE("/api/projects/{id}", { params: { path: { id } } }));

// ---- Time entries ----

export interface TimeEntryFilters {
  date?: string;
  projectId?: number;
}

export const listTimeEntries = (filters: TimeEntryFilters = {}, signal?: AbortSignal) =>
  unwrap<TimeEntryResponse[]>(
    api.GET("/api/time-entries", { params: { query: { ...filters } }, signal }),
  );

export const getTimeEntry = (id: number) =>
  unwrap<TimeEntryResponse>(api.GET("/api/time-entries/{id}", { params: { path: { id } } }));

export const createTimeEntry = (body: TimeEntryInput) =>
  unwrap<TimeEntryResponse>(api.POST("/api/time-entries", { body }));

export const updateTimeEntry = (id: number, ifMatch: number, body: TimeEntryInput) =>
  unwrap<TimeEntryResponse>(
    api.PUT("/api/time-entries/{id}", { params: { path: { id }, header: { "If-Match": ifMatch } }, body }),
  );

export const deleteTimeEntry = (id: number) =>
  unwrap<void>(api.DELETE("/api/time-entries/{id}", { params: { path: { id } } }));
