import { useLocation, useNavigate, useParams } from 'react-router'

import { ArticleEditor } from '../../features/admin-content/components/ArticleEditor'
import type { EditorNotice } from '../../features/admin-content/model/articleEditorState'
import { useAdminArticle } from '../../features/admin-content/useAdminArticle'

export function ArticleEditorPage() {
  const { articleId } = useParams()
  const navigate = useNavigate()
  const location = useLocation()

  if (articleId === undefined) {
    return (
      <ArticleEditor
        initial={null}
        initialNotice={null}
        onCreated={(id) => {
          // Replace the create URL, so Back does not reopen an empty form.
          void navigate(`/admin/articles/${id}`, { replace: true, state: { notice: 'created' } })
        }}
      />
    )
  }
  return <ExistingArticleEditor key={articleId} articleId={articleId} initialNotice={noticeFrom(location.state)} />
}

function ExistingArticleEditor({ articleId, initialNotice }: { articleId: string; initialNotice: EditorNotice | null }) {
  const { load, retry } = useAdminArticle(articleId)

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
      return <ArticleEditor initial={load.article} initialNotice={initialNotice} />
  }
}

function noticeFrom(state: unknown): EditorNotice | null {
  return typeof state === 'object' && state !== null && 'notice' in state && state.notice === 'created'
    ? 'created'
    : null
}
