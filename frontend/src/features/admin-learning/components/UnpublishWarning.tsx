import { useId, useState, type ReactNode } from 'react'

/**
 * Tells the Admin that unpublishing changes only this content. The levels
 * below keep their own status. Cancel is the default focus.
 */
export function UnpublishWarning({ onConfirm, onCancel }: { onConfirm: () => void; onCancel: () => void }) {
  const titleId = useId()
  const descriptionId = useId()

  return (
    <div className="admin-dialog-backdrop">
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        className="admin-dialog"
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            onCancel()
          }
        }}
      >
        <h2 id={titleId}>Gỡ xuất bản?</h2>
        <p id={descriptionId}>
          Gỡ xuất bản chỉ đổi trạng thái của nội dung này. Các cấp bên dưới (Module, bài viết) giữ nguyên trạng thái
          xuất bản của chúng.
        </p>
        <div className="admin-dialog-actions">
          <button type="button" className="admin-button" autoFocus onClick={onCancel}>
            Huỷ
          </button>
          <button type="button" className="admin-button admin-button-primary" onClick={onConfirm}>
            Vẫn gỡ xuất bản
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Connects the warning to `PublicationControls`: `confirm` opens the dialog
 * and resolves with the Admin's answer. Render `warning` once in the editor.
 */
export function useUnpublishWarning(): { confirm: () => Promise<boolean>; warning: ReactNode } {
  const [answer, setAnswer] = useState<((confirmed: boolean) => void) | null>(null)

  function confirm(): Promise<boolean> {
    // The updater form stores the resolve function itself, not its result.
    return new Promise<boolean>((resolve) => setAnswer(() => resolve))
  }

  function settle(confirmed: boolean) {
    answer?.(confirmed)
    setAnswer(null)
  }

  return {
    confirm,
    warning: answer === null ? null : <UnpublishWarning onConfirm={() => settle(true)} onCancel={() => settle(false)} />,
  }
}
