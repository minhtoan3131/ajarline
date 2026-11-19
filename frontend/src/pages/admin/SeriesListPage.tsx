import { Link, useSearchParams } from 'react-router'

import { SeriesList } from '../../features/admin-learning/components/SeriesList'
import { NEW_SERIES_PATH } from '../../features/admin-learning/learningRoutes'
import { useSeriesList, type SeriesListState } from '../../features/admin-learning/useSeriesList'
import { pageFromQuery } from '../../shared/lib/pageFromQuery'

export function SeriesListPage() {
  const [searchParams] = useSearchParams()
  const { state, retry } = useSeriesList(pageFromQuery(searchParams.get('page')))

  return (
    <section>
      <header className="admin-page-header">
        <h1>Series</h1>
        <Link className="admin-button admin-button-primary" to={NEW_SERIES_PATH}>
          Tạo Series
        </Link>
      </header>
      <SeriesListBody state={state} onRetry={retry} />
    </section>
  )
}

function SeriesListBody({ state, onRetry }: { state: SeriesListState; onRetry: () => void }) {
  switch (state.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải danh sách Series…
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được danh sách Series do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )
    case 'found':
      return <SeriesList list={state.list} />
  }
}
