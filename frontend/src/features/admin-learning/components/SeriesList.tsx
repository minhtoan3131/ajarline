import { Link } from 'react-router'

import { Pagination } from '../../../shared/ui/Pagination'
import { seriesEditorPath, seriesListPath } from '../learningRoutes'
import type { AdminContentList } from '../model/learning'
import { StatusTag } from './StatusTag'

/** Rows of one Series list page. The page header with the create link belongs to the page. */
export function SeriesList({ list }: { list: AdminContentList }) {
  if (list.items.length === 0) {
    return list.page === 0 ? (
      <p className="admin-status">Chưa có Series nào.</p>
    ) : (
      <div className="admin-status">
        <p>Trang này không còn nội dung.</p>
        <Link to={seriesListPath()}>Về trang đầu</Link>
      </div>
    )
  }
  return (
    <>
      <ul className="admin-list" aria-label="Danh sách Series">
        {list.items.map((item) => (
          <li key={item.id} className="admin-list-item">
            <Link className="admin-list-item-title" to={seriesEditorPath(item.id)}>
              {item.title}
            </Link>
            <span className="admin-list-item-meta">{item.slug}</span>
            <StatusTag status={item.status} />
          </li>
        ))}
      </ul>
      <Pagination page={list.page} hasMore={list.hasMore} pathOf={seriesListPath} />
    </>
  )
}
