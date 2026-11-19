import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listSeries } from './api/learningAdminApi'
import type { AdminContentItem } from './model/learning'

export type SeriesChoicesLoad =
  | { status: 'loading' }
  | { status: 'ready'; items: AdminContentItem[] }
  | { status: 'failed' }

/** Loads the Series that a select can offer. A failure is the caller's to show; it never blocks the form. */
export function useSeriesChoices(): { load: SeriesChoicesLoad; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<SeriesChoicesLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setLoad({ status: 'loading' })
    listSeries(client, 0, controller.signal).then(
      (list) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', items: list.items })
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

  return { load, retry: () => setAttempt((value) => value + 1) }
}
