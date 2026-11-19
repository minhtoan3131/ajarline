import { Navigate, useSearchParams } from 'react-router'

import { isStandaloneIndexQuery, standaloneIndexPath } from '../features/article-browsing/articleRoutes'
import { StandaloneIndexView } from '../features/article-browsing/components/StandaloneIndexView'
import { useStandaloneIndex } from '../features/article-browsing/useStandaloneIndex'
import { pageFromQuery } from '../shared/lib/pageFromQuery'

export function StandaloneIndexPage() {
  const [searchParams] = useSearchParams()

  // Replace, so Back does not return to a URL that redirects again.
  if (!isStandaloneIndexQuery(searchParams)) {
    return <Navigate to={standaloneIndexPath()} replace />
  }
  return <StandaloneIndexScreen page={pageFromQuery(searchParams.get('page'))} />
}

/** Loads the page only for a valid URL, so a bad link never costs a request. */
function StandaloneIndexScreen({ page }: { page: number }) {
  const { state, retry } = useStandaloneIndex(page)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải danh sách bài viết…
        </p>
      )
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được danh sách bài viết do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <StandaloneIndexView collection={state.collection} />
  }
}
