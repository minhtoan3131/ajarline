import { useId } from 'react'
import { Link } from 'react-router'

import type { LearningChild } from '../model/learning'
import type { ListFailure } from '../model/learningEditorFailure'
import { StatusTag } from './StatusTag'

export interface ChildOrderListProps {
  heading: string
  listLabel: string
  emptyText: string
  items: LearningChild[]
  /** There is an arranged order that is not saved yet. */
  dirty: boolean
  /** Running mutation of the editor. Any value locks every button here. */
  pending: string | null
  failure: ListFailure | null
  /** Where a title links to. Without it the title is plain text. */
  pathOf?: (childId: string) => string
  onMove: (index: number, offset: -1 | 1) => void
  onSave: () => void
  onReload: () => void
}

/** Children in order, each with its own status. The Admin moves one at a time, then saves the order. */
export function ChildOrderList({
  heading,
  listLabel,
  emptyText,
  items,
  dirty,
  pending,
  failure,
  pathOf,
  onMove,
  onSave,
  onReload,
}: ChildOrderListProps) {
  const headingId = useId()
  const locked = pending !== null
  return (
    <section className="learning-children" aria-labelledby={headingId}>
      <h2 id={headingId}>{heading}</h2>
      {failure !== null && (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>{failure.message}</p>
          {failure.kind === 'order' && (
            <button type="button" className="admin-button" disabled={locked} onClick={onReload}>
              Tải lại danh sách (bỏ thay đổi chưa lưu)
            </button>
          )}
        </div>
      )}
      {items.length === 0 ? (
        <p className="admin-status">{emptyText}</p>
      ) : (
        <>
          <ol className="admin-list" aria-label={listLabel}>
            {items.map((child, index) => (
              <li key={child.id} className="admin-list-item">
                <span className="admin-list-item-meta">{index + 1}.</span>
                {pathOf === undefined ? (
                  <span className="admin-list-item-title">{child.title}</span>
                ) : (
                  <Link className="admin-list-item-title" to={pathOf(child.id)}>
                    {child.title}
                  </Link>
                )}
                <span className="admin-list-item-meta">{child.slug}</span>
                <StatusTag status={child.status} />
                <span className="admin-list-item-actions">
                  <button
                    type="button"
                    className="admin-button"
                    aria-label={`Đưa lên: ${child.title}`}
                    disabled={locked || index === 0}
                    onClick={() => onMove(index, -1)}
                  >
                    Lên
                  </button>
                  <button
                    type="button"
                    className="admin-button"
                    aria-label={`Đưa xuống: ${child.title}`}
                    disabled={locked || index === items.length - 1}
                    onClick={() => onMove(index, 1)}
                  >
                    Xuống
                  </button>
                </span>
              </li>
            ))}
          </ol>
          {items.length > 1 && (
            <div className="learning-children-actions">
              <button type="button" className="admin-button" disabled={locked || !dirty} onClick={onSave}>
                {pending === 'ordering' ? 'Đang lưu thứ tự…' : 'Lưu thứ tự'}
              </button>
            </div>
          )}
        </>
      )}
    </section>
  )
}
