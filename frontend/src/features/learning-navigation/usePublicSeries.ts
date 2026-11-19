import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicSeries } from './api/publicLearningApi'
import type { PublicSeries } from './model/publicSeries'

export type PublicSeriesState =
  | { status: 'loading' }
  | { status: 'found'; series: PublicSeries }
  | { status: 'not-found' }
  | { status: 'failed' }

/** A public Series has Modules, and each Module has Articles. An empty list is wrong data. */
function hasEmptyChildList(series: PublicSeries): boolean {
  return series.modules.length === 0 || series.modules.some((item) => item.articles.length === 0)
}

/**
 * Loads the public Series for a slug. Missing and non-public Series share one
 * not-found state. A Series with an empty child list is a failure, so the
 * page never draws an empty frame.
 */
export function usePublicSeries(slug: string): { state: PublicSeriesState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<PublicSeriesState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchPublicSeries(client, slug, controller.signal).then(
      (series) => {
        if (!controller.signal.aborted) {
          setState(hasEmptyChildList(series) ? { status: 'failed' } : { status: 'found', series })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        const notFound = error instanceof ApiError && error.status === 404
        setState(notFound ? { status: 'not-found' } : { status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, slug, attempt])

  return { state, retry: () => setAttempt((value) => value + 1) }
}
