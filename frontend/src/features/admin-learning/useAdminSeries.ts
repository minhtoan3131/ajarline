import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import type { Versioned } from '../admin-content/model/article'
import { getSeries } from './api/learningAdminApi'
import type { SeriesAdminDetail } from './model/series'

export type AdminSeriesLoad =
  | { status: 'loading' }
  | { status: 'ready'; series: Versioned<SeriesAdminDetail> }
  | { status: 'not-found' }
  | { status: 'failed' }

/** Loads a Series for the editor by id, for direct entry and reload. */
export function useAdminSeries(seriesId: string): { load: AdminSeriesLoad; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<AdminSeriesLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setLoad({ status: 'loading' })
    getSeries(client, seriesId, controller.signal).then(
      (series) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', series })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        const notFound = error instanceof ApiError && error.status === 404
        setLoad(notFound ? { status: 'not-found' } : { status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, seriesId, attempt])

  return { load, retry: () => setAttempt((value) => value + 1) }
}
