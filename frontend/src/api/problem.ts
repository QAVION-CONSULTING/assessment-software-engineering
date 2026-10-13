import { ApiError } from './http'

export interface ErrorInfo {
  /** Human readable summary, safe to show to the user. */
  message: string
  /** Backend validation messages keyed by request field name (HTTP 400). */
  fieldErrors: Record<string, string>
}

/** Translates anything thrown by the API layer into something a form or page can show. */
export function describeError(error: unknown): ErrorInfo {
  if (error instanceof ApiError) {
    const errors = error.problem.errors
    if (error.status === 400 && errors && typeof errors === 'object') {
      return { message: 'Please correct the highlighted fields.', fieldErrors: errors as Record<string, string> }
    }
    if (error.status >= 500) {
      return { message: 'The server ran into a problem. Please try again later.', fieldErrors: {} }
    }
    // 400 (malformed), 404 and 422 carry an explanatory `detail` from the backend.
    return { message: error.problem.detail ?? error.problem.title ?? 'The request failed.', fieldErrors: {} }
  }
  return { message: 'Cannot reach the server. Check your connection and try again.', fieldErrors: {} }
}
