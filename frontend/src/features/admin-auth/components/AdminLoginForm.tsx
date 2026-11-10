import { useState, type FormEvent } from 'react'

import { ApiError } from '../../../shared/api/httpClient'
import { useAdminSession } from '../AdminSessionContext'

type LoginFeedback =
  | { kind: 'none' }
  | { kind: 'invalid' }
  | { kind: 'locked'; retryAfterMinutes: number | null }
  | { kind: 'session-renewed' }
  | { kind: 'failed' }

/**
 * Username and password form for the single Admin account.
 *
 * Messages never say which field was wrong and never count attempts. A lock
 * shows the wait time only from the Retry-After header.
 */
export function AdminLoginForm() {
  const session = useAdminSession()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [feedback, setFeedback] = useState<LoginFeedback>({ kind: 'none' })

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) {
      return
    }
    setSubmitting(true)
    setFeedback({ kind: 'none' })
    try {
      await session.signIn(username, password)
      // Success changes the session status; the page then leaves the form.
    } catch (error) {
      setPassword('')
      setFeedback(toLoginFeedback(error))
      setSubmitting(false)
    }
  }

  return (
    <form className="admin-login" noValidate onSubmit={(event) => void submit(event)}>
      <h1>Đăng nhập quản trị</h1>
      <label htmlFor="admin-username">Tên đăng nhập</label>
      <input
        id="admin-username"
        className="admin-input"
        name="username"
        autoComplete="username"
        value={username}
        onChange={(event) => setUsername(event.target.value)}
      />
      <label htmlFor="admin-password">Mật khẩu</label>
      <input
        id="admin-password"
        className="admin-input"
        name="password"
        type="password"
        autoComplete="current-password"
        value={password}
        onChange={(event) => setPassword(event.target.value)}
      />
      {feedback.kind !== 'none' && (
        <p className="admin-notice admin-notice-error" role="alert">
          {feedbackText(feedback)}
        </p>
      )}
      <button type="submit" className="admin-button admin-button-primary" disabled={submitting}>
        {submitting ? 'Đang đăng nhập…' : 'Đăng nhập'}
      </button>
    </form>
  )
}

function toLoginFeedback(error: unknown): LoginFeedback {
  if (error instanceof ApiError) {
    if (error.status === 401) {
      return { kind: 'invalid' }
    }
    if (error.status === 429) {
      const seconds = error.retryAfterSeconds
      return { kind: 'locked', retryAfterMinutes: seconds === null ? null : Math.ceil(seconds / 60) }
    }
    if (error.status === 403 && error.code === 'CSRF_INVALID') {
      return { kind: 'session-renewed' }
    }
  }
  return { kind: 'failed' }
}

function feedbackText(feedback: Exclude<LoginFeedback, { kind: 'none' }>): string {
  switch (feedback.kind) {
    case 'invalid':
      return 'Tên đăng nhập hoặc mật khẩu không đúng.'
    case 'locked':
      return feedback.retryAfterMinutes === null
        ? 'Đăng nhập đang tạm khoá do nhập sai nhiều lần. Hãy thử lại sau ít phút.'
        : `Đăng nhập đang tạm khoá do nhập sai nhiều lần. Thử lại sau khoảng ${feedback.retryAfterMinutes} phút.`
    case 'session-renewed':
      return 'Phiên làm việc vừa được làm mới. Hãy đăng nhập lại.'
    case 'failed':
      return 'Không đăng nhập được do lỗi kết nối hoặc máy chủ. Hãy thử lại.'
  }
}
