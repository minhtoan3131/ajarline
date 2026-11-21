import { useState } from 'react'

export interface ConceptDeleteControlProps {
  conceptName: string
  deleting: boolean
  disabled: boolean
  onDelete: () => void
}

/** Two-step delete: the first click only asks, the second one deletes. */
export function ConceptDeleteControl({ conceptName, deleting, disabled, onDelete }: ConceptDeleteControlProps) {
  const [confirming, setConfirming] = useState(false)

  if (deleting) {
    return (
      <button type="button" className="admin-button" disabled>
        Đang xóa…
      </button>
    )
  }
  if (!confirming) {
    return (
      <button type="button" className="admin-button admin-button-danger" disabled={disabled} onClick={() => setConfirming(true)}>
        Xóa Concept
      </button>
    )
  }
  return (
    <div className="concept-delete-confirm" role="group" aria-label="Xác nhận xóa Concept">
      <p>Xóa Concept “{conceptName}”? Các expression của nó bị xóa theo. Việc này không hoàn tác được.</p>
      <button
        type="button"
        className="admin-button admin-button-danger"
        onClick={() => {
          // Collapse the question first, so a failed delete leaves the plain button.
          setConfirming(false)
          onDelete()
        }}
      >
        Xác nhận xóa
      </button>
      <button type="button" className="admin-button" onClick={() => setConfirming(false)}>
        Giữ lại
      </button>
    </div>
  )
}
