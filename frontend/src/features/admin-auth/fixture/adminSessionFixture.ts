import { jsonResponse, type StubApiServer } from '../../../shared/api/testsupport/StubApiServer'

export const CSRF_TOKEN = { headerName: 'X-CSRF-TOKEN', token: 'csrf-token' }

export function csrfTokenBody(token: string) {
  return { headerName: CSRF_TOKEN.headerName, token }
}

export function givenAnonymousVisitor(api: StubApiServer): StubApiServer {
  return api
    .on('GET', '/api/auth/v1/session', jsonResponse(200, { authenticated: false }))
    .on('GET', '/api/auth/v1/csrf', jsonResponse(200, CSRF_TOKEN))
}

export function givenSignedInAdmin(api: StubApiServer): StubApiServer {
  return api
    .on('GET', '/api/auth/v1/session', jsonResponse(200, { authenticated: true }))
    .on('GET', '/api/auth/v1/csrf', jsonResponse(200, CSRF_TOKEN))
}
