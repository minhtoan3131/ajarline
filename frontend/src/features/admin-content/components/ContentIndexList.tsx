import { Link } from 'react-router'

import { editorPathOf } from '../contentRoutes'
import { CONTENT_TYPE_LABELS, type AdminContentItem } from '../model/contentIndex'
import { STATUS_LABELS } from './PublicationControls'

/** Rows of one Content Index page. The title opens the editor of that item. */
export function ContentIndexList({ items }: { items: AdminContentItem[] }) {
  return (
    <ul className="admin-list" aria-label="Danh sách nội dung">
      {items.map((item) => (
        <li key={item.id} className="admin-list-item">
          <Link className="admin-list-item-title" to={editorPathOf(item.type, item.id)}>
            {item.title}
          </Link>
          <span className="content-type-tag">{CONTENT_TYPE_LABELS[item.type]}</span>
          <span className={`admin-badge admin-badge-${item.status.toLowerCase()}`}>{STATUS_LABELS[item.status]}</span>
          <span className="admin-list-item-meta">{item.slug}</span>
          <time className="admin-list-item-meta" dateTime={item.updatedAt}>
            {formatUpdatedAt(item.updatedAt)}
          </time>
        </li>
      ))}
    </ul>
  )
}

function formatUpdatedAt(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })
}
