import type { PublicationStatus, Versioned } from '../model/article'
import { canDeleteContent } from '../model/contentDelete'
import type { AdminContentType } from '../model/contentIndex'
import type { CarriedNotice } from '../model/indexNotice'
import { DeleteContentControl } from './DeleteContentControl'

export interface EditorDeleteControlProps {
  type: AdminContentType
  /** The saved item with its ETag, or null for an item that was never saved. */
  current: Versioned<{ id: string; title: string; status: PublicationStatus }> | null
  /** True while the editor saves, reloads or changes the status. */
  disabled: boolean
  /** Called when the item was deleted or is already gone. Without it nothing is shown. */
  onRemoved?: (notice: CarriedNotice) => void
}

/**
 * Delete button of an editor. It deletes the saved version, so it is not
 * shown for an item that was never saved or that is not a Draft.
 */
export function EditorDeleteControl({ type, current, disabled, onRemoved }: EditorDeleteControlProps) {
  if (current === null || onRemoved === undefined || !canDeleteContent(current.value.status)) {
    return null
  }
  const { id, title } = current.value
  return (
    <DeleteContentControl
      target={{ type, id, title, etag: current.etag }}
      disabled={disabled}
      onDeleted={() => onRemoved({ kind: 'deleted', title })}
      onFailed={(failure) => {
        if (failure.kind === 'gone') {
          onRemoved({ kind: 'gone' })
        }
      }}
    />
  )
}
