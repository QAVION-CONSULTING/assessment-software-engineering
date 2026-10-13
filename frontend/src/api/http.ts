/** RFC 9457 Problem Details as returned by the backend. */
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  instance?: string
  [extension: string]: unknown
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem: ProblemDetail,
  ) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${status}`)
  }
}

const BASE_URL = '/api'

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ProblemDetail
    throw new ApiError(response.status, problem)
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T)
}
