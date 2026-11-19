import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listSeries } from './api/learningAdminApi'
import type { AdminContentList } from './model/learning'

export type SeriesListState =
  | { status: 'loading' }
  | { status: 'found'; list: AdminContentList }
  | { status: 'failed' }

/**
 * Loads one page of Series. An empty page is a found state: it is a valid
 * answer, not a failure. Every failure is the same failed state.
 */
export function useSeriesList(page: number): { state: SeriesListState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<SeriesListState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    listSeries(client, page, controller.signal).then(
      (list) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', list })
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
  }, [client, page, attempt])

  return { state, retry: () => setAttempt((value) => value + 1) }
}
