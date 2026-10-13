// Mirrors the backend DTOs (TimeEntryRequest, TimeEntryResponse).
export interface TimeEntryRequest {
  projectId: number
  /** ISO date, yyyy-MM-dd */
  entryDate: string
  durationHours: number
  description?: string
}

export interface TimeEntry extends TimeEntryRequest {
  id: number
}
