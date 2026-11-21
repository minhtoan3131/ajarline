import { useMemo, useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'

import { ContentFilters } from '../../features/admin-content/components/ContentFilters'
import { ContentIndexList } from '../../features/admin-content/components/ContentIndexList'
import { NewContentMenu } from '../../features/admin-content/components/NewContentMenu'
import { contentIndexPath } from '../../features/admin-content/contentRoutes'
import type { AdminContentIndex } from '../../features/admin-content/model/contentIndex'
import { needsReload, type DeleteFailure } from '../../features/admin-content/model/contentDeleteFailure'
import {
  describeEmpty,
  NO_FILTER,
  parseContentIndexQuery,
  type ContentIndexQuery,
} from '../../features/admin-content/model/contentIndexQuery'
import {
  indexNoticeFrom,
  isProblemNotice,
  noticeText,
  type IndexNotice,
} from '../../features/admin-content/model/indexNotice'
import { useContentIndex, type ContentIndexState } from '../../features/admin-content/useContentIndex'
import { Pagination } from '../../shared/ui/Pagination'

/** A notice and the search part of the URL it was made for. */
interface NoticeEntry {
  search: string
  notice: IndexNotice | null
}

export function ContentIndexPage() {
  const [searchParams] = useSearchParams()
  const location = useLocation()
  const navigate = useNavigate()
  // One object per URL, so the hook loads once per URL and not once per render.
  const query = useMemo(() => parseContentIndexQuery(searchParams), [searchParams])
  const { state, reload } = useContentIndex(query)
  const [entry, setEntry] = useState<NoticeEntry>(() => ({
    search: location.search,
    notice: indexNoticeFrom(location.state),
  }))

  // A notice belongs to one URL. When the Admin filters or changes page, it goes away.
  if (entry.search !== location.search) {
    setEntry({ search: location.search, notice: null })
  }

  function show(next: ContentIndexQuery) {
    void navigate(contentIndexPath(next))
  }

  function announce(notice: IndexNotice) {
    setEntry({ search: location.search, notice })
  }

  function handleDeleted(title: string) {
    announce({ kind: 'deleted', title })
    reload()
  }

  function handleDeleteFailed(failure: DeleteFailure) {
    announce({ kind: 'failure', message: failure.message })
    if (needsReload(failure)) {
      reload()
    }
  }

  return (
    <section>
      <header className="admin-page-header">
        <h1>Nội dung</h1>
        <NewContentMenu />
      </header>
      <ContentFilters query={query} onChange={show} />
      {entry.notice !== null && <NoticeBar notice={entry.notice} />}
      <ContentIndexBody
        query={query}
        state={state}
        onRetry={reload}
        onShow={show}
        onDeleted={handleDeleted}
        onDeleteFailed={handleDeleteFailed}
      />
    </section>
  )
}

function NoticeBar({ notice }: { notice: IndexNotice }) {
  const problem = isProblemNotice(notice)
  return (
    <p
      className={`admin-notice ${problem ? 'admin-notice-error' : 'admin-notice-success'}`}
      role={problem ? 'alert' : 'status'}
    >
      {noticeText(notice)}
    </p>
  )
}

interface ContentIndexBodyProps {
  query: ContentIndexQuery
  state: ContentIndexState
  onRetry: () => void
  onShow: (query: ContentIndexQuery) => void
  onDeleted: (title: string) => void
  onDeleteFailed: (failure: DeleteFailure) => void
}

function ContentIndexBody({ query, state, onRetry, ...rest }: ContentIndexBodyProps) {
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
      return <IndexResults query={query} index={state.index} {...rest} />
  }
}

interface IndexResultsProps {
  query: ContentIndexQuery
  index: AdminContentIndex
  onShow: (query: ContentIndexQuery) => void
  onDeleted: (title: string) => void
  onDeleteFailed: (failure: DeleteFailure) => void
}

function IndexResults({ query, index, onShow, onDeleted, onDeleteFailed }: IndexResultsProps) {
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
          <ContentIndexList items={index.items} onDeleted={onDeleted} onDeleteFailed={onDeleteFailed} />
          <Pagination
            page={index.page}
            hasMore={index.hasMore}
            pathOf={(page) => contentIndexPath({ ...query, page })}
          />
        </>
      )
  }
}
