import type { HealthResponse } from '@/types/health'
import { request } from './http'

export function getHealth(): Promise<HealthResponse> {
  return request<HealthResponse>('/actuator/health')
}
