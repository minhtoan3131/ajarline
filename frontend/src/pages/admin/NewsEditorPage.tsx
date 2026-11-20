import { useLocation, useNavigate, useParams } from 'react-router'

import type { EditorNotice } from '../../features/admin-content/model/articleEditorState'
import { NewsEditor } from '../../features/admin-news/components/NewsEditor'
import { newsEditorPath } from '../../features/admin-news/newsRoutes'
import { useAdminNews } from '../../features/admin-news/useAdminNews'
import { AdminNotFoundPage } from './AdminNotFoundPage'

export function NewsEditorPage() {
  const { newsId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  if (newsId === undefined) {
    return (
      <NewsEditor
        initial={null}
        initialNotice={null}
        onCreated={(id) => {
          // Replace the create URL, so Back does not reopen an empty form and refresh does not create a second item.
          void navigate(newsEditorPath(id), { replace: true, state: { notice: 'created' } })
        }}
      />
    )
  }
  return <ExistingNewsEditor key={newsId} newsId={newsId} initialNotice={noticeFrom(location.state)} />
}

function ExistingNewsEditor({ newsId, initialNotice }: { newsId: string; initialNotice: EditorNotice | null }) {
  const { load, retry } = useAdminNews(newsId)

  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải tin…
        </p>
      )
    case 'not-found':
      return <AdminNotFoundPage />
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được tin do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={retry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return <NewsEditor initial={load.news} initialNotice={initialNotice} />
  }
}

function noticeFrom(state: unknown): EditorNotice | null {
  return typeof state === 'object' && state !== null && 'notice' in state && state.notice === 'created'
    ? 'created'
    : null
}
