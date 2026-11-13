export interface AdminSessionPendingProps {
  status: 'checking' | 'unavailable'
  onRetry: () => void
}

/** Shown while the session is checked, or when the check could not finish. */
export function AdminSessionPending({ status, onRetry }: AdminSessionPendingProps) {
  if (status === 'checking') {
    return (
      <p className="admin-status" role="status">
        Đang kiểm tra phiên đăng nhập…
      </p>
    )
  }
  return (
    <div className="admin-notice admin-notice-error" role="alert">
      <p>Không kiểm tra được phiên đăng nhập do lỗi kết nối hoặc máy chủ.</p>
      <button type="button" className="admin-button" onClick={onRetry}>
        Thử lại
      </button>
    </div>
  )
}
