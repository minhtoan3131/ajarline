import { useSearchParams } from 'react-router'

import { NewsIndexView } from '../features/news-reading/components/NewsIndexView'
import { useNewsIndex } from '../features/news-reading/useNewsIndex'
import { pageFromQuery } from '../shared/lib/pageFromQuery'

export function NewsIndexPage() {
  const [searchParams] = useSearchParams()
  const { state, retry } = useNewsIndex(pageFromQuery(searchParams.get('page')))

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải danh sách tin…
        </p>
      )
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được danh sách tin do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <NewsIndexView collection={state.collection} />
  }
}
