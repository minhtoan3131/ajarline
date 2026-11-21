import { useRef, useState } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { deleteContent } from './api/contentIndexApi'
import type { DeleteTarget } from './model/contentDelete'
import { describeDeleteFailure, type DeleteFailure } from './model/contentDeleteFailure'

export type DeleteState = { status: 'idle' } | { status: 'deleting' } | { status: 'failed'; failure: DeleteFailure }

export interface DeleteHandlers {
  /** Called after the server deleted the item. */
  onDeleted: () => void
  /** Called after a failed delete, with the failure that the screen can show. */
  onFailed?: (failure: DeleteFailure) => void
}

/**
 * Runs deletes against the Admin API, one at a time. After a successful
 * delete the state stays `deleting`: the caller removes the control that
 * uses this hook, so it must not offer a second delete of the same item.
 */
export function useDeleteContent(handlers: DeleteHandlers): {
  state: DeleteState
  remove: (target: DeleteTarget) => Promise<void>
} {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, setState] = useState<DeleteState>({ status: 'idle' })
  // Set at once. State changes only at the next render, too late for a fast second click.
  const running = useRef(false)

  async function remove(target: DeleteTarget): Promise<void> {
    if (running.current) {
      return
    }
    running.current = true
    setState({ status: 'deleting' })
    try {
      await deleteContent(client, target.type, target.id, target.etag)
    } catch (error) {
      const failure = describeDeleteFailure(error, target.type)
      if (failure.kind === 'session-renewed') {
        // The old token is no longer valid. Get a new one so the next try works.
        void session.renewCsrfToken().catch(() => undefined)
      }
      setState({ status: 'failed', failure })
      handlers.onFailed?.(failure)
      return
    } finally {
      running.current = false
    }
    handlers.onDeleted()
  }

  return { state, remove }
}
