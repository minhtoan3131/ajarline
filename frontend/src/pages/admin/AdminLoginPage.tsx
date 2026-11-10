import { Navigate, useLocation } from 'react-router'

import { useAdminSession } from '../../features/admin-auth/AdminSessionContext'
import { AdminLoginForm } from '../../features/admin-auth/components/AdminLoginForm'
import { AdminSessionPending } from '../../features/admin-auth/components/AdminSessionPending'

const DEFAULT_TARGET = '/admin/articles/new'

export function AdminLoginPage() {
  const session = useAdminSession()
  const location = useLocation()

  if (session.status === 'checking' || session.status === 'unavailable') {
    return <AdminSessionPending status={session.status} onRetry={session.retry} />
  }
  if (session.status === 'signed-in') {
    return <Navigate to={redirectTarget(location.state)} replace />
  }
  return <AdminLoginForm />
}

/** Returns the Admin page that sent the user here, if there is one. */
function redirectTarget(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'from' in state && typeof state.from === 'string') {
    // Only Admin paths are accepted, so the value can never leave the area.
    return state.from.startsWith('/admin/') && state.from !== '/admin/login' ? state.from : DEFAULT_TARGET
  }
  return DEFAULT_TARGET
}
