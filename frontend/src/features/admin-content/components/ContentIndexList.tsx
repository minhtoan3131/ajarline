import { Link } from 'react-router'

import { editorPathOf } from '../contentRoutes'
import { canDeleteContent, etagOfRevision } from '../model/contentDelete'
import type { DeleteFailure } from '../model/contentDeleteFailure'
import { CONTENT_TYPE_LABELS, type AdminContentItem } from '../model/contentIndex'
import { DeleteContentControl } from './DeleteContentControl'
import { STATUS_LABELS } from './PublicationControls'

export interface ContentIndexListProps {
  items: AdminContentItem[]
  /** Called with the title of an item that the server deleted. */
  onDeleted: (title: string) => void
  /** Called when a delete failed. The list shows no message itself; the caller does. */
  onDeleteFailed: (failure: DeleteFailure) => void
}

/** Rows of one Content Index page. The title opens the editor of that item. */
export function ContentIndexList({ items, onDeleted, onDeleteFailed }: ContentIndexListProps) {
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
          {canDeleteContent(item.status) && (
            <span className="admin-list-item-actions">
              <DeleteContentControl
                target={{ type: item.type, id: item.id, title: item.title, etag: etagOfRevision(item.revision) }}
                inlineFailure={false}
                onDeleted={() => onDeleted(item.title)}
                onFailed={onDeleteFailed}
              />
            </span>
          )}
        </li>
      ))}
    </ul>
  )
}

function formatUpdatedAt(iso: string): string {
  return new Date(iso).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' })
}
