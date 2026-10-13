import type { TimeEntry, TimeEntryRequest } from '@/types/timeEntry'
import { request } from './http'

export const listTimeEntries = () => request<TimeEntry[]>('/time-entries')

export const createTimeEntry = (body: TimeEntryRequest) =>
  request<TimeEntry>('/time-entries', { method: 'POST', body: JSON.stringify(body) })

export const updateTimeEntry = (id: number, body: TimeEntryRequest) =>
  request<TimeEntry>(`/time-entries/${id}`, { method: 'PUT', body: JSON.stringify(body) })

export const deleteTimeEntry = (id: number) => request<void>(`/time-entries/${id}`, { method: 'DELETE' })
