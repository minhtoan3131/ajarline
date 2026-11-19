import { useLocation, useNavigate, useParams, useSearchParams } from 'react-router'

import { ModuleEditor } from '../../features/admin-learning/components/ModuleEditor'
import { moduleEditorPath } from '../../features/admin-learning/learningRoutes'
import type { LearningNotice } from '../../features/admin-learning/model/learningEditorState'
import { blankModuleFields } from '../../features/admin-learning/model/module'
import { useAdminModule } from '../../features/admin-learning/useAdminModule'
import { useNewModuleParent } from '../../features/admin-learning/useNewModuleParent'
import { AdminNotFoundPage } from './AdminNotFoundPage'

export function ModuleEditorPage() {
  const { moduleId } = useParams()
  const location = useLocation()

  if (moduleId === undefined) {
    return <NewModuleEditor />
  }
  return <ExistingModuleEditor key={moduleId} moduleId={moduleId} initialNotice={noticeFrom(location.state)} />
}

function NewModuleEditor() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const parent = useNewModuleParent(searchParams.get('seriesId'))

  if (parent.status === 'loading') {
    return (
      <p className="admin-status" role="status">
        Đang tải Series…
      </p>
    )
  }
  return (
    <ModuleEditor
      initial={null}
      initialNotice={null}
      blank={blankModuleFields(parent.parent)}
      onCreated={(id) => {
        // Replace the create URL, so Back does not reopen an empty form and refresh does not create a second Module.
        void navigate(moduleEditorPath(id), { replace: true, state: { notice: 'created' } })
      }}
    />
  )
}

function ExistingModuleEditor({ moduleId, initialNotice }: { moduleId: string; initialNotice: LearningNotice | null }) {
  const { load, retry } = useAdminModule(moduleId)

  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải Module…
        </p>
      )
    case 'not-found':
      return <AdminNotFoundPage />
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được Module do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={retry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return <ModuleEditor initial={load.module} initialNotice={initialNotice} />
  }
}

function noticeFrom(state: unknown): LearningNotice | null {
  return typeof state === 'object' && state !== null && 'notice' in state && state.notice === 'created'
    ? 'created'
    : null
}
