import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, request } from './http'

afterEach(() => vi.unstubAllGlobals())

describe('request', () => {
  it('returns parsed JSON on success', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('{"ok":true}', { status: 200 })))
    await expect(request('/x')).resolves.toEqual({ ok: true })
  })

  it('throws ApiError carrying the Problem Details body on failure', async () => {
    const problem = { title: 'Bad Request', status: 400, detail: 'name must not be blank' }
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify(problem), { status: 400 })))
    await expect(request('/x')).rejects.toMatchObject({ status: 400, message: 'name must not be blank' })
    await expect(request('/x')).rejects.toBeInstanceOf(ApiError)
  })
})
