import { useParams, useSearchParams } from 'react-router'

import { TopicHomeView } from '../features/topic-browsing/components/TopicHomeView'
import { useTopicHome } from '../features/topic-browsing/useTopicHome'
import { pageFromQuery } from '../shared/lib/pageFromQuery'
import { NotFoundPage } from './NotFoundPage'

export function TopicHomePage() {
  const { slug = '' } = useParams()
  const [searchParams] = useSearchParams()
  const page = pageFromQuery(searchParams.get('page'))
  const { state, retry } = useTopicHome(slug, page)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải chủ đề…
        </p>
      )
    case 'not-found':
      return <NotFoundPage />
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được chủ đề do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <TopicHomeView topic={state.topic} content={state.content} />
  }
}
