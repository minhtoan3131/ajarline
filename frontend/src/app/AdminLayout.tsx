import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router'

import { AdminSessionProvider, useAdminSession } from '../features/admin-auth/AdminSessionContext'

/** Admin frame. The session provider only exists inside this route tree. */
export function AdminLayout() {
  return (
    <AdminSessionProvider>
      <div className="admin-scope">
        <AdminHeader />
        <main className="admin-main">
          <Outlet />
        </main>
      </div>
    </AdminSessionProvider>
  )
}

function AdminHeader() {
  const session = useAdminSession()
  const navigate = useNavigate()
  const [signOutFailed, setSignOutFailed] = useState(false)

  async function signOut() {
    setSignOutFailed(false)
    try {
      await session.signOut()
      void navigate('/admin/login', { replace: true })
    } catch {
      setSignOutFailed(true)
    }
  }

  return (
    <header className="admin-header">
      <span className="admin-brand">Ajarline · Quản trị</span>
      {(session.status === 'signed-in' || session.status === 'expired') && <AdminNavigation />}
      {session.status === 'signed-in' && (
        <button type="button" className="admin-button" onClick={() => void signOut()}>
          Đăng xuất
        </button>
      )}
      {session.status === 'expired' && (
        <p className="admin-notice admin-notice-warning admin-header-notice" role="alert">
          Phiên đăng nhập đã hết hạn. Nội dung chưa lưu vẫn còn trên màn hình nhưng không lưu được nữa; hãy sao
          chép phần cần giữ rồi <Link to="/admin/login">đăng nhập lại</Link>.
        </p>
      )}
      {signOutFailed && (
        <p className="admin-notice admin-notice-error admin-header-notice" role="alert">
          Không đăng xuất được do lỗi kết nối. Hãy thử lại.
        </p>
      )}
    </header>
  )
}

/**
 * Links to the Admin screens, grouped like the information architecture.
 * The Content group has Series and News so far; the Organization group has Topic.
 */
function AdminNavigation() {
  return (
    <nav className="admin-nav" aria-label="Điều hướng quản trị">
      <span className="admin-nav-group">Nội dung</span>
      <NavLink to="/admin/series" className="admin-nav-link">
        Series
      </NavLink>
      <NavLink to="/admin/news" className="admin-nav-link">
        Tin
      </NavLink>
      <span className="admin-nav-group">Tổ chức</span>
      <NavLink to="/admin/topics" className="admin-nav-link">
        Topic
      </NavLink>
    </nav>
  )
}
