import { useMemo } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { ContentFilters } from '../../features/admin-content/components/ContentFilters'
import { ContentIndexList } from '../../features/admin-content/components/ContentIndexList'
import { NewContentMenu } from '../../features/admin-content/components/NewContentMenu'
import { contentIndexPath } from '../../features/admin-content/contentRoutes'
import type { AdminContentIndex } from '../../features/admin-content/model/contentIndex'
import {
  describeEmpty,
  NO_FILTER,
  parseContentIndexQuery,
  type ContentIndexQuery,
} from '../../features/admin-content/model/contentIndexQuery'
import { useContentIndex, type ContentIndexState } from '../../features/admin-content/useContentIndex'
import { Pagination } from '../../shared/ui/Pagination'

export function ContentIndexPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  // One object per URL, so the hook loads once per URL and not once per render.
  const query = useMemo(() => parseContentIndexQuery(searchParams), [searchParams])
  const { state, reload } = useContentIndex(query)

  function show(next: ContentIndexQuery) {
    void navigate(contentIndexPath(next))
  }

  return (
    <section>
      <header className="admin-page-header">
        <h1>Nội dung</h1>
        <NewContentMenu />
      </header>
      <ContentFilters query={query} onChange={show} />
      <ContentIndexBody query={query} state={state} onRetry={reload} onShow={show} />
    </section>
  )
}

interface ContentIndexBodyProps {
  query: ContentIndexQuery
  state: ContentIndexState
  onRetry: () => void
  onShow: (query: ContentIndexQuery) => void
}

function ContentIndexBody({ query, state, onRetry, onShow }: ContentIndexBodyProps) {
  switch (state.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải danh sách nội dung…
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được danh sách nội dung do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )
    case 'found':
      return <IndexResults query={query} index={state.index} onShow={onShow} />
  }
}

function IndexResults({
  query,
  index,
  onShow,
}: {
  query: ContentIndexQuery
  index: AdminContentIndex
  onShow: (query: ContentIndexQuery) => void
}) {
  switch (describeEmpty(query, index)) {
    case 'store-empty':
      return <p className="admin-status">Chưa có nội dung nào.</p>
    case 'no-match':
      return (
        <div className="admin-status">
          <p>Không có nội dung nào khớp bộ lọc.</p>
          <button type="button" className="admin-button" onClick={() => onShow(NO_FILTER)}>
            Bỏ lọc
          </button>
        </div>
      )
    case 'past-end':
      return (
        <div className="admin-status">
          <p>Trang này không còn nội dung.</p>
          <Link to={contentIndexPath({ ...query, page: 0 })}>Về trang đầu</Link>
        </div>
      )
    case null:
      return (
        <>
          <ContentIndexList items={index.items} />
          <Pagination
            page={index.page}
            hasMore={index.hasMore}
            pathOf={(page) => contentIndexPath({ ...query, page })}
          />
        </>
      )
  }
}
