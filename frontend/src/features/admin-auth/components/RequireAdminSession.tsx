import { Navigate, Outlet, useLocation } from 'react-router'

import { useAdminSession } from '../AdminSessionContext'
import { AdminSessionPending } from './AdminSessionPending'

/**
 * Route guard for Admin pages. The server still checks every request; this
 * guard only avoids showing a page that cannot work without a session.
 */
export function RequireAdminSession() {
  const session = useAdminSession()
  const location = useLocation()

  switch (session.status) {
    case 'checking':
    case 'unavailable':
      return <AdminSessionPending status={session.status} onRetry={session.retry} />
    case 'signed-out':
      return <Navigate to="/admin/login" replace state={{ from: location.pathname }} />
    case 'signed-in':
    case 'expired':
      // An expired session keeps the page so unsaved text stays visible.
      return <Outlet />
  }
}
