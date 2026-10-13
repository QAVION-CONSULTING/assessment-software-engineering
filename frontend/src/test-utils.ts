import type { VueWrapper } from '@vue/test-utils'
import type { BudgetStatus, Project } from '@/types/project'
import type { TimeEntry } from '@/types/timeEntry'

/** Finds a form control through its visible <label>, the way a user (or screen reader) would. */
export function field(wrapper: VueWrapper, labelText: string) {
  const label = wrapper.findAll('label').find((l) => l.text().startsWith(labelText))
  if (!label) throw new Error(`No label starting with "${labelText}"`)
  return wrapper.get(`[id="${label.attributes('for')}"]`)
}

export function buttonByText(wrapper: VueWrapper, text: string) {
  const button = wrapper.findAll('button').find((b) => b.text() === text)
  if (!button) throw new Error(`No button "${text}"`)
  return button
}

export function makeProject(overrides: Partial<Project> & { overall?: BudgetStatus; timePercent?: number; costPercent?: number } = {}): Project {
  const { overall = 'OK', timePercent = 10, costPercent = 5, ...rest } = overrides
  return {
    id: 1,
    name: 'Website',
    timeBudgetHours: 100,
    costBudgetEur: 10000,
    hourlyRateEur: 50,
    budget: {
      totalHours: 10,
      totalCostEur: 500,
      timeUtilizationPercent: timePercent,
      costUtilizationPercent: costPercent,
      timeStatus: overall,
      costStatus: 'OK',
      overallStatus: overall,
    },
    ...rest,
  }
}

export function makeEntry(overrides: Partial<TimeEntry> = {}): TimeEntry {
  return { id: 10, projectId: 1, entryDate: '2025-03-10', durationHours: 7.5, description: 'Coding', ...overrides }
}
