import { useParams } from 'react-router'

import { SeriesView } from '../features/learning-navigation/components/SeriesView'
import { usePublicSeries } from '../features/learning-navigation/usePublicSeries'
import { NotFoundPage } from './NotFoundPage'

export function SeriesPage() {
  const { slug = '' } = useParams()
  const { state, retry } = usePublicSeries(slug)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải Series…
        </p>
      )
    case 'not-found':
      return <NotFoundPage />
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được Series do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <SeriesView series={state.series} />
  }
}
