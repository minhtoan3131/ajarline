import { describe, expect, it, vi } from 'vitest'

import { ApiError, HttpClient } from './httpClient'
import { jsonResponse, problemResponse, StubApiServer } from './testsupport/StubApiServer'

describe('HttpClient', () => {
  it('request_ShouldSendCsrfHeaderAndJsonBody_WhenMethodIsUnsafe', async () => {
    // Given
    const api = new StubApiServer().on('POST', '/api/x', jsonResponse(200, { ok: true })).install()
    const client = new HttpClient()
    client.setCsrfToken({ headerName: 'X-CSRF-TOKEN', token: 'token-1' })

    // When
    await client.request('/api/x', { method: 'POST', body: { a: 1 } })

    // Then
    const [request] = api.requests
    expect(request.headers.get('X-CSRF-TOKEN')).toBe('token-1')
    expect(request.headers.get('Content-Type')).toBe('application/json')
    expect(request.body).toEqual({ a: 1 })
  })

  it('request_ShouldNotSendCsrfHeader_WhenMethodIsGet', async () => {
    // Given
    const api = new StubApiServer().on('GET', '/api/x', jsonResponse(200, {})).install()
    const client = new HttpClient()
    client.setCsrfToken({ headerName: 'X-CSRF-TOKEN', token: 'token-1' })

    // When
    await client.request('/api/x')

    // Then
    expect(api.requests[0].headers.has('X-CSRF-TOKEN')).toBe(false)
  })

  it('request_ShouldSendIfMatchAndReturnEtag_WhenPreconditionIsGiven', async () => {
    // Given
    const api = new StubApiServer().on('PUT', '/api/x', jsonResponse(200, {}, { ETag: '"4"' })).install()

    // When
    const response = await new HttpClient().request('/api/x', { method: 'PUT', body: {}, ifMatch: '"3"' })

    // Then
    expect(api.requests[0].headers.get('If-Match')).toBe('"3"')
    expect(response.etag).toBe('"4"')
  })

  it('request_ShouldReturnUndefinedBody_WhenResponseHasNoContent', async () => {
    // Given
    new StubApiServer().on('POST', '/api/x', { status: 204 }).install()

    // When
    const response = await new HttpClient().request('/api/x', { method: 'POST' })

    // Then
    expect(response.status).toBe(204)
    expect(response.body).toBeUndefined()
  })

  it('request_ShouldThrowProblemDetails_WhenResponseIsNotSuccessful', async () => {
    // Given
    new StubApiServer()
      .on('POST', '/api/x', problemResponse(429, 'ADMIN_LOGIN_LOCKED', { headers: { 'Retry-After': '900' } }))
      .install()

    // When
    const error = await new HttpClient().request('/api/x', { method: 'POST' }).catch((caught: unknown) => caught)

    // Then
    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 429, code: 'ADMIN_LOGIN_LOCKED', retryAfterSeconds: 900 })
  })

  it('request_ShouldNotifyListeners_WhenAdminSessionIsMissing', async () => {
    // Given
    new StubApiServer()
      .on('GET', '/api/admin', problemResponse(401, 'AUTHENTICATION_REQUIRED'))
      .on('POST', '/api/login', problemResponse(401, 'INVALID_CREDENTIALS'))
      .install()
    const client = new HttpClient()
    const listener = vi.fn()
    client.onUnauthorized(listener)

    // When
    await client.request('/api/admin').catch(() => undefined)
    await client.request('/api/login', { method: 'POST' }).catch(() => undefined)

    // Then
    expect(listener).toHaveBeenCalledTimes(1)
  })

  it('request_ShouldThrowStatusZero_WhenNoResponseArrives', async () => {
    // Given
    vi.stubGlobal('fetch', () => Promise.reject(new TypeError('Failed to fetch')))

    // When / Then
    await expect(new HttpClient().request('/api/x')).rejects.toMatchObject({ status: 0, code: null })
  })
})
