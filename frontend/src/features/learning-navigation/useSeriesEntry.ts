import { useEffect, useState } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchSeriesPage } from './api/publicLearningApi'

export type SeriesEntryState = 'hidden' | 'visible'

/**
 * Tells whether the Home page should link to the Series Index. It asks for
 * one Series only, so the Home page learns "any or none" and never a count.
 * Loading, an empty answer and every failure all keep the link hidden: it is
 * a side entry, and Home must work without it.
 */
export function useSeriesEntry(): SeriesEntryState {
  const client = useHttpClient()
  const [state, setState] = useState<SeriesEntryState>('hidden')

  useEffect(() => {
    const controller = new AbortController()
    fetchSeriesPage(client, 0, 1, controller.signal).then(
      (collection) => {
        if (!controller.signal.aborted) {
          setState(collection.items.length > 0 ? 'visible' : 'hidden')
        }
      },
      () => {
        // Any failure, including a cancelled request, leaves the link hidden.
      },
    )
    return () => controller.abort()
  }, [client])

  return state
}
