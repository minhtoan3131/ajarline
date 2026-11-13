import { useParams } from 'react-router'

import { PublicArticleView } from '../features/article-reading/components/PublicArticleView'
import { usePublicArticle } from '../features/article-reading/usePublicArticle'
import { NotFoundPage } from './NotFoundPage'

export function ArticlePage() {
  const { slug = '' } = useParams()
  const { state, retry } = usePublicArticle(slug)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải bài viết…
        </p>
      )
    case 'not-found':
      return <NotFoundPage />
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được bài viết do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <PublicArticleView article={state.article} />
  }
}
