import { Link } from 'react-router'

/**
 * Previous and next links by page and hasMore. There is no total, so there is
 * no "2 of 14". Nothing shows when the only page is the first one.
 *
 * @param pathOf builds the Reader URL of a page index
 */
export function Pagination({
  page,
  hasMore,
  pathOf,
}: {
  page: number
  hasMore: boolean
  pathOf: (page: number) => string
}) {
  if (page === 0 && !hasMore) {
    return null
  }
  return (
    <nav className="reader-pagination" aria-label="Phân trang">
      {page > 0 && (
        <Link to={pathOf(page - 1)} rel="prev" className="reader-pagination-link">
          Trang trước
        </Link>
      )}
      <span className="reader-pagination-current">Trang {page + 1}</span>
      {hasMore && (
        <Link to={pathOf(page + 1)} rel="next" className="reader-pagination-link">
          Trang sau
        </Link>
      )}
    </nav>
  )
}
