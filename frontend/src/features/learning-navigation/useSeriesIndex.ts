import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchSeriesPage } from './api/publicLearningApi'
import type { SeriesCollectionPage } from './model/seriesCollection'

export type SeriesIndexState =
  | { status: 'loading' }
  | { status: 'found'; collection: SeriesCollectionPage }
  | { status: 'failed' }

/**
 * Loads one page of public Series. An empty page is a found state: it is a
 * valid answer, not a failure. Every failure is the same failed state.
 */
export function useSeriesIndex(page: number): { state: SeriesIndexState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<SeriesIndexState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchSeriesPage(client, page, undefined, controller.signal).then(
      (collection) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', collection })
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
