import { describe, expect, it } from 'vitest'
import { describeError } from './problem'
import { ApiError } from './http'

describe('describeError', () => {
  it('maps 400 validation errors to field errors', () => {
    const error = new ApiError(400, { status: 400, errors: { name: 'must not be blank' } })
    expect(describeError(error)).toEqual({
      message: 'Please correct the highlighted fields.',
      fieldErrors: { name: 'must not be blank' },
    })
  })

  it.each([404, 422])('shows the backend detail for %i', (status) => {
    const info = describeError(new ApiError(status, { status, detail: 'Explained by the backend.' }))
    expect(info.message).toBe('Explained by the backend.')
    expect(info.fieldErrors).toEqual({})
  })

  it('hides internals of server errors', () => {
    const info = describeError(new ApiError(500, { status: 500, detail: 'NullPointerException at ...' }))
    expect(info.message).toBe('The server ran into a problem. Please try again later.')
  })

  it('treats non-API failures as network problems', () => {
    expect(describeError(new TypeError('Failed to fetch')).message).toMatch(/Cannot reach the server/)
  })
})
