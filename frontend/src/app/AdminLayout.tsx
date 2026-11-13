import { useState } from 'react'
import { Link, Outlet, useNavigate } from 'react-router'

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
