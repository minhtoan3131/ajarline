import { useParams } from 'react-router'

import { PublicNewsView } from '../features/news-reading/components/PublicNewsView'
import { usePublicNews } from '../features/news-reading/usePublicNews'
import { NotFoundPage } from './NotFoundPage'

export function NewsPage() {
  const { slug = '' } = useParams()
  const { state, retry } = usePublicNews(slug)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải tin…
        </p>
      )
    case 'not-found':
      return <NotFoundPage />
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được tin do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <PublicNewsView news={state.news} />
  }
}
