import { Link } from 'react-router'

import { STATUS_LABELS } from '../../admin-content/components/PublicationControls'
import { Pagination } from '../../../shared/ui/Pagination'
import { newsEditorPath, newsListPath } from '../newsRoutes'
import type { AdminNewsList } from '../model/newsList'

/** Rows of one News list page. The page header with the create link belongs to the page. */
export function NewsList({ list }: { list: AdminNewsList }) {
  if (list.items.length === 0) {
    return list.page === 0 ? (
      <p className="admin-status">Chưa có tin nào.</p>
    ) : (
      <div className="admin-status">
        <p>Trang này không còn nội dung.</p>
        <Link to={newsListPath()}>Về trang đầu</Link>
      </div>
    )
  }
  return (
    <>
      <ul className="admin-list" aria-label="Danh sách tin">
        {list.items.map((item) => (
          <li key={item.id} className="admin-list-item">
            <Link className="admin-list-item-title" to={newsEditorPath(item.id)}>
              {item.title}
            </Link>
            <span className="admin-list-item-meta">{item.slug}</span>
            <span className={`admin-badge admin-badge-${item.status.toLowerCase()}`}>{STATUS_LABELS[item.status]}</span>
          </li>
        ))}
      </ul>
      <Pagination page={list.page} hasMore={list.hasMore} pathOf={newsListPath} />
    </>
  )
}
