import { useCallback, useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listConcepts } from './api/conceptAdminApi'
import type { ConceptAdminItem } from './model/concept'

export type ConceptListLoad =
  | { status: 'loading' }
  | { status: 'ready'; concepts: ConceptAdminItem[] }
  | { status: 'failed' }

/**
 * Loads the Concept list. The server owns the order, so after any change the
 * screen asks for the list again instead of editing its own copy.
 */
export function useConceptList(): { load: ConceptListLoad; reload: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<ConceptListLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    // Keep the old list on screen while a reload runs. Only the first load
    // and a retry after a failure show "loading".
    setLoad((current) => (current.status === 'ready' ? current : { status: 'loading' }))
    listConcepts(client, controller.signal).then(
      (concepts) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', concepts })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        setLoad({ status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, attempt])

  const reload = useCallback(() => setAttempt((value) => value + 1), [])
  return { load, reload }
}
