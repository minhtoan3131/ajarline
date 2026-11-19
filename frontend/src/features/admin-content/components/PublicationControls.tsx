import type { PublicationStatus } from '../model/article'

export const STATUS_LABELS: Record<PublicationStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  UNPUBLISHED: 'Đã gỡ xuất bản',
}

/** Status label of an Admin editor. `null` means the content was never saved. */
export function PublicationBadge({ status }: { status: PublicationStatus | null }) {
  const label = status === null ? 'Chưa lưu lần nào' : STATUS_LABELS[status]
  const variant = status === null ? 'new' : status.toLowerCase()
  return <span className={`admin-badge admin-badge-${variant}`}>Trạng thái: {label}</span>
}

function canPublish(status: PublicationStatus | null): boolean {
  return status !== null && status !== 'PUBLISHED'
}

export interface PublicationControlsProps {
  /** `null` when the content was never saved. Then no button shows. */
  status: PublicationStatus | null
  /** The form has unsaved changes. Publish applies to the saved version only. */
  dirty: boolean
  /** Running mutation. Any value locks the buttons; `publishing` and `unpublishing` also change the label. */
  pending: string | null
  /** Id of the `PublicationHint` paragraph that explains a locked Publish button. */
  hintId: string
  onPublish: () => void
  onUnpublish: () => void
  /** Asks the Admin before unpublishing. Resolve false to cancel. Without it, unpublish runs at once. */
  confirmUnpublish?: () => boolean | Promise<boolean>
}

/** Publish and Unpublish buttons. They render as siblings, so the editor places them in its own toolbar. */
export function PublicationControls({
  status,
  dirty,
  pending,
  hintId,
  onPublish,
  onUnpublish,
  confirmUnpublish,
}: PublicationControlsProps) {
  async function requestUnpublish() {
    // No await when there is nothing to ask, so the call stays inside the click handler.
    if (confirmUnpublish !== undefined && !(await confirmUnpublish())) {
      return
    }
    onUnpublish()
  }

  return (
    <>
      {canPublish(status) && (
        <button
          type="button"
          className="admin-button"
          disabled={pending !== null || dirty}
          aria-describedby={dirty ? hintId : undefined}
          onClick={onPublish}
        >
          {pending === 'publishing' ? 'Đang xuất bản…' : 'Xuất bản'}
        </button>
      )}
      {status === 'PUBLISHED' && (
        <button
          type="button"
          className="admin-button"
          disabled={pending !== null}
          onClick={() => void requestUnpublish()}
        >
          {pending === 'unpublishing' ? 'Đang gỡ xuất bản…' : 'Gỡ xuất bản'}
        </button>
      )}
    </>
  )
}

/** Explains why Publish is locked. It shows only while Publish is visible and the form is dirty. */
export function PublicationHint({ id, status, dirty }: { id: string; status: PublicationStatus | null; dirty: boolean }) {
  if (!canPublish(status) || !dirty) {
    return null
  }
  return (
    <p id={id} className="admin-field-hint">
      Lưu thay đổi trước khi xuất bản. Xuất bản chỉ áp dụng cho bản đã lưu.
    </p>
  )
}
