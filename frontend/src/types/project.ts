// Mirrors the backend DTOs (ProjectRequest, ProjectResponse, BudgetSummary). Decimals arrive as JSON numbers.
export type BudgetStatus = 'OK' | 'WARNING' | 'EXCEEDED'

export interface BudgetSummary {
  totalHours: number
  totalCostEur: number
  timeUtilizationPercent: number
  costUtilizationPercent: number
  timeStatus: BudgetStatus
  costStatus: BudgetStatus
  overallStatus: BudgetStatus
}

export interface ProjectRequest {
  name: string
  timeBudgetHours: number
  costBudgetEur: number
  hourlyRateEur: number
}

export interface Project extends ProjectRequest {
  id: number
  budget: BudgetSummary
}
