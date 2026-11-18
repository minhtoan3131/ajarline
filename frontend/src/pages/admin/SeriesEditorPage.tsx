import { useLocation, useNavigate, useParams } from 'react-router'

import { SeriesEditor } from '../../features/admin-learning/components/SeriesEditor'
import { seriesEditorPath } from '../../features/admin-learning/learningRoutes'
import type { LearningNotice } from '../../features/admin-learning/model/learningEditorState'
import { useAdminSeries } from '../../features/admin-learning/useAdminSeries'
import { AdminNotFoundPage } from './AdminNotFoundPage'

export function SeriesEditorPage() {
  const { seriesId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  if (seriesId === undefined) {
    return (
      <SeriesEditor
        initial={null}
        initialNotice={null}
        onCreated={(id) => {
          // Replace the create URL, so Back does not reopen an empty form and refresh does not create a second Series.
          void navigate(seriesEditorPath(id), { replace: true, state: { notice: 'created' } })
        }}
      />
    )
  }
  return <ExistingSeriesEditor key={seriesId} seriesId={seriesId} initialNotice={noticeFrom(location.state)} />
}

function ExistingSeriesEditor({ seriesId, initialNotice }: { seriesId: string; initialNotice: LearningNotice | null }) {
  const { load, retry } = useAdminSeries(seriesId)

  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải Series…
        </p>
      )
    case 'not-found':
      return <AdminNotFoundPage />
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được Series do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={retry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return <SeriesEditor initial={load.series} initialNotice={initialNotice} />
  }
}

function noticeFrom(state: unknown): LearningNotice | null {
  return typeof state === 'object' && state !== null && 'notice' in state && state.notice === 'created'
    ? 'created'
    : null
}
