import type { CsrfToken, HttpClient } from '../../../shared/api/httpClient'

const AUTH_PATH = '/api/auth/v1'

interface CsrfTokenResponse {
  token: string
  headerName: string
}

interface SessionStateResponse {
  authenticated: boolean
}

export async function fetchCsrfToken(client: HttpClient): Promise<CsrfToken> {
  const { body } = await client.request<CsrfTokenResponse>(`${AUTH_PATH}/csrf`)
  return { headerName: body.headerName, token: body.token }
}

export async function fetchSessionState(client: HttpClient): Promise<boolean> {
  const { body } = await client.request<SessionStateResponse>(`${AUTH_PATH}/session`)
  return body.authenticated
}

/** @throws ApiError 401 INVALID_CREDENTIALS, 429 ADMIN_LOGIN_LOCKED, 403 CSRF_INVALID */
export async function loginAdmin(client: HttpClient, username: string, password: string): Promise<void> {
  await client.request(`${AUTH_PATH}/login`, { method: 'POST', body: { username, password } })
}

export async function logoutAdmin(client: HttpClient): Promise<void> {
  // The endpoint rejects any request body, so none is sent.
  await client.request(`${AUTH_PATH}/logout`, { method: 'POST' })
}
