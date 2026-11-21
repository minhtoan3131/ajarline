import { useState } from 'react'

import type { DeleteTarget } from '../model/contentDelete'
import { allowsRetry, type DeleteFailure } from '../model/contentDeleteFailure'
import { useDeleteContent } from '../useDeleteContent'

export interface DeleteContentControlProps {
  target: DeleteTarget
  /** True while the screen does something else with the item, for example saving it. */
  disabled?: boolean
  /** Shows the failure message next to the button. A caller that shows failures itself passes `false`. */
  inlineFailure?: boolean
  /** Called after the server deleted the item. The caller must then remove this control. */
  onDeleted: () => void
  onFailed?: (failure: DeleteFailure) => void
}

/**
 * Two-step delete: the first click only asks, the second one deletes.
 * Nothing is sent before the Admin confirms.
 */
export function DeleteContentControl({
  target,
  disabled = false,
  inlineFailure = true,
  onDeleted,
  onFailed,
}: DeleteContentControlProps) {
  const { state, remove } = useDeleteContent({ onDeleted, onFailed })
  const [confirming, setConfirming] = useState(false)
  const failure = state.status === 'failed' ? state.failure : null

  if (state.status === 'deleting') {
    return (
      <button type="button" className="admin-button" disabled>
        Đang xóa…
      </button>
    )
  }
  return (
    <div className="content-delete">
      {confirming ? (
        <div className="content-delete-question" role="group" aria-label={`Xác nhận xóa ${target.title}`}>
          <p>Xóa “{target.title}”? Xóa hẳn, không khôi phục được.</p>
          <button
            type="button"
            className="admin-button admin-button-danger"
            onClick={() => {
              // Collapse the question first, so a failed delete leaves the plain button.
              setConfirming(false)
              void remove(target)
            }}
          >
            Xác nhận xóa
          </button>
          <button type="button" className="admin-button" onClick={() => setConfirming(false)}>
            Giữ lại
          </button>
        </div>
      ) : (
        (failure === null || allowsRetry(failure)) && (
          <button
            type="button"
            className="admin-button admin-button-danger"
            disabled={disabled}
            aria-label={`Xóa ${target.title}`}
            onClick={() => setConfirming(true)}
          >
            Xóa
          </button>
        )
      )}
      {inlineFailure && failure !== null && (
        <p className="admin-notice admin-notice-error" role="alert">
          {failure.message}
        </p>
      )}
    </div>
  )
}
