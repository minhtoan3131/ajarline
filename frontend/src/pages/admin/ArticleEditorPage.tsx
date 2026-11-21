import { useLocation, useNavigate, useParams } from 'react-router'

import { ArticleEditor, type PlacementSlot } from '../../features/admin-content/components/ArticleEditor'
import { editorPathOf } from '../../features/admin-content/contentRoutes'
import type { EditorNotice } from '../../features/admin-content/model/articleEditorState'
import { useAdminArticle } from '../../features/admin-content/useAdminArticle'
import { useLeaveToIndex } from '../../features/admin-content/useLeaveToIndex'
import { ModulePlacementPicker } from '../../features/admin-learning/components/ModulePlacementPicker'

/** Draws the Module picker for the slot of the Article editor. A new Article has no id yet. */
function placementPicker(articleId: string | null) {
  return (slot: PlacementSlot) => <ModulePlacementPicker articleId={articleId} {...slot} />
}

export function ArticleEditorPage() {
  const { articleId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  if (articleId === undefined) {
    return (
      <ArticleEditor
        initial={null}
        initialNotice={null}
        renderPlacement={placementPicker(null)}
        onCreated={(id) => {
          // Replace the create URL, so Back does not reopen an empty form.
          void navigate(editorPathOf('article', id), { replace: true, state: { notice: 'created' } })
        }}
      />
    )
  }
  return <ExistingArticleEditor key={articleId} articleId={articleId} initialNotice={noticeFrom(location.state)} />
}

function ExistingArticleEditor({ articleId, initialNotice }: { articleId: string; initialNotice: EditorNotice | null }) {
  const { load, retry } = useAdminArticle(articleId)
  const leaveToIndex = useLeaveToIndex()

  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải bài viết…
        </p>
      )
    case 'not-found':
      return (
        <p className="admin-notice admin-notice-error" role="alert">
          Không tìm thấy bài viết này.
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được bài viết do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={retry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return (
        <ArticleEditor
          initial={load.article}
          initialNotice={initialNotice}
          renderPlacement={placementPicker(articleId)}
          onRemoved={leaveToIndex}
        />
      )
  }
}

function noticeFrom(state: unknown): EditorNotice | null {
  return typeof state === 'object' && state !== null && 'notice' in state && state.notice === 'created'
    ? 'created'
    : null
}
