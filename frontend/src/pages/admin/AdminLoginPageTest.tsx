import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import {
  CSRF_TOKEN,
  csrfTokenBody,
  givenAnonymousVisitor,
  givenSignedInAdmin,
} from '../../features/admin-auth/fixture/adminSessionFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const LOGIN = '/api/auth/v1/login'

async function submitCredentials(username: string, password: string) {
  const user = userEvent.setup()
  await user.type(await screen.findByLabelText('Tên đăng nhập'), username)
  await user.type(screen.getByLabelText('Mật khẩu'), password)
  await user.click(screen.getByRole('button', { name: 'Đăng nhập' }))
}

describe('AdminLoginPage', () => {
  it('render_ShouldShowLabelledForm_WhenVisitorIsAnonymous', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer()).install()

    // When
    renderAppRoute('/admin/login')

    // Then
    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByLabelText('Mật khẩu')).toHaveAttribute('type', 'password')
    expect(screen.queryByRole('button', { name: 'Đăng xuất' })).toBeNull()
  })

  it('signIn_ShouldRenewCsrfAroundLogin_WhenCredentialsAreValid', async () => {
    // Given
    const api = new StubApiServer()
      .on('GET', '/api/auth/v1/session', jsonResponse(200, { authenticated: false }))
      .on(
        'GET',
        '/api/auth/v1/csrf',
        jsonResponse(200, csrfTokenBody('boot')),
        jsonResponse(200, csrfTokenBody('before-login')),
        jsonResponse(200, csrfTokenBody('after-login')),
      )
      .on('POST', LOGIN, jsonResponse(200, { authenticated: true }))
      .install()
    renderAppRoute('/admin/login')

    // When
    await submitCredentials('admin', 'correct horse battery staple')

    // Then
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent('/admin/articles/new'))
    const [login] = api.requestsTo('POST', LOGIN)
    expect(login.body).toEqual({ username: 'admin', password: 'correct horse battery staple' })
    expect(login.headers.get('X-CSRF-TOKEN')).toBe('before-login')
    const calls = api.requests.map((request) => `${request.method} ${request.path}`)
    expect(calls.lastIndexOf('GET /api/auth/v1/csrf')).toBeGreaterThan(calls.indexOf(`POST ${LOGIN}`))
  })

  it('signIn_ShouldShowGenericMessage_WhenCredentialsAreInvalid', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer()).on('POST', LOGIN, problemResponse(401, 'INVALID_CREDENTIALS')).install()
    renderAppRoute('/admin/login')

    // When
    await submitCredentials('admin', 'wrong password')

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('Tên đăng nhập hoặc mật khẩu không đúng.')
    expect(screen.getByLabelText('Tên đăng nhập')).toHaveValue('admin')
    expect(screen.getByLabelText('Mật khẩu')).toHaveValue('')
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })

  it('signIn_ShouldShowWaitTimeFromRetryAfter_WhenLoginIsLocked', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer())
      .on('POST', LOGIN, problemResponse(429, 'ADMIN_LOGIN_LOCKED', { headers: { 'Retry-After': '900' } }))
      .install()
    renderAppRoute('/admin/login')

    // When
    await submitCredentials('admin', 'wrong password')

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('Thử lại sau khoảng 15 phút.')
    expect(screen.queryByText(/lần thử|\d\/5/)).toBeNull()
  })

  it('signOut_ShouldReturnToLoginWithoutBrowserStorage_WhenAdminLogsOut', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer()).on('POST', '/api/auth/v1/logout', { status: 204 }).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/login')

    // When
    await user.click(await screen.findByRole('button', { name: 'Đăng xuất' }))

    // Then
    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
    expect(api.requestsTo('POST', '/api/auth/v1/logout')[0].headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(window.localStorage).toHaveLength(0)
    expect(window.sessionStorage).toHaveLength(0)
  })
})
