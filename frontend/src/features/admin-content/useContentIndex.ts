import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listContent } from './api/contentIndexApi'
import type { AdminContentIndex } from './model/contentIndex'
import type { ContentIndexQuery } from './model/contentIndexQuery'

export type ContentIndexState =
  | { status: 'loading' }
  | { status: 'found'; index: AdminContentIndex }
  | { status: 'failed' }

/**
 * Loads one page of the Content Index. An empty page is a found state: it is a
 * valid answer, not a failure. Every failure is the same failed state.
 * The `query` object must stay the same object while its values stay the same.
 */
export function useContentIndex(query: ContentIndexQuery): { state: ContentIndexState; reload: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<ContentIndexState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    listContent(client, query, controller.signal).then(
      (index) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', index })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        setState({ status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, query, attempt])

  return { state, reload: () => setAttempt((value) => value + 1) }
}
