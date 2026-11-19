import { Link, useSearchParams } from 'react-router'

import { NewsList } from '../../features/admin-news/components/NewsList'
import { NEW_NEWS_PATH } from '../../features/admin-news/newsRoutes'
import { useNewsList, type NewsListState } from '../../features/admin-news/useNewsList'
import { pageFromQuery } from '../../shared/lib/pageFromQuery'

export function NewsListPage() {
  const [searchParams] = useSearchParams()
  const { state, retry } = useNewsList(pageFromQuery(searchParams.get('page')))

  return (
    <section>
      <header className="admin-page-header">
        <h1>Tin</h1>
        <Link className="admin-button admin-button-primary" to={NEW_NEWS_PATH}>
          Tạo tin
        </Link>
      </header>
      <NewsListBody state={state} onRetry={retry} />
    </section>
  )
}

function NewsListBody({ state, onRetry }: { state: NewsListState; onRetry: () => void }) {
  switch (state.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải danh sách tin…
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được danh sách tin do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )
    case 'found':
      return <NewsList list={state.list} />
  }
}
