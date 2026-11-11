import { useEffect, useId, useRef, type KeyboardEvent, type MouseEvent } from 'react'
import { createPortal } from 'react-dom'

import type { ArticleFields } from '../model/article'
import { ArticlePreview } from './ArticlePreview'

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'

export interface FullPreviewOverlayProps {
  fields: ArticleFields
  onClose: () => void
}

/**
 * Full-screen preview of the current editor values.
 *
 * It does not change the URL, history, editor state or call the server.
 * Focus stays inside while it is open and returns to the opener on close.
 */
export function FullPreviewOverlay({ fields, onClose }: FullPreviewOverlayProps) {
  const dialogRef = useRef<HTMLDivElement>(null)
  const closeRef = useRef<HTMLButtonElement>(null)
  const titleId = useId()

  useEffect(() => {
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null
    document.body.classList.add('scroll-locked')
    closeRef.current?.focus()
    return () => {
      document.body.classList.remove('scroll-locked')
      opener?.focus()
    }
  }, [])

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === 'Escape') {
      event.stopPropagation()
      onClose()
      return
    }
    if (event.key === 'Tab') {
      keepFocusInside(event)
    }
  }

  function keepFocusInside(event: KeyboardEvent<HTMLDivElement>) {
    const focusable = dialogRef.current?.querySelectorAll<HTMLElement>(FOCUSABLE)
    if (focusable === undefined || focusable.length === 0) {
      return
    }
    const first = focusable[0]
    const last = focusable[focusable.length - 1]
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault()
      last.focus()
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault()
      first.focus()
    }
  }

  function closeOnBackdrop(event: MouseEvent<HTMLDivElement>) {
    // Only a press on the dim area closes; presses inside the panel do not.
    if (event.target === event.currentTarget) {
      onClose()
    }
  }

  return createPortal(
    <div className="preview-overlay" onMouseDown={closeOnBackdrop}>
      <div
        ref={dialogRef}
        className="preview-overlay-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        onKeyDown={handleKeyDown}
      >
        <div className="preview-overlay-bar">
          <h2 id={titleId}>Xem trước bài viết</h2>
          <button ref={closeRef} type="button" className="preview-overlay-close" aria-label="Đóng xem trước" onClick={onClose}>
            ×
          </button>
        </div>
        <div className="preview-overlay-body">
          <ArticlePreview fields={fields} />
        </div>
      </div>
    </div>,
    document.body,
  )
}
