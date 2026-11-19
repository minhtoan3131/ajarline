import { useSearchParams } from 'react-router'

import { SeriesIndexView } from '../features/learning-navigation/components/SeriesIndexView'
import { useSeriesIndex } from '../features/learning-navigation/useSeriesIndex'
import { pageFromQuery } from '../shared/lib/pageFromQuery'

export function SeriesIndexPage() {
  const [searchParams] = useSearchParams()
  const { state, retry } = useSeriesIndex(pageFromQuery(searchParams.get('page')))

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải danh sách Series…
        </p>
      )
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được danh sách Series do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <SeriesIndexView collection={state.collection} />
  }
}
