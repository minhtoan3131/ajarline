import { useEffect, useState } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchNewsPage } from './api/publicNewsApi'

export type NewsEntryState = 'hidden' | 'visible'

/**
 * Tells whether the Home page should link to the News Index. It asks for one
 * News item only, so the Home page learns "any or none" and never a count.
 * Loading, an empty answer and every failure all keep the link hidden: it is
 * a side entry, and Home must work without it.
 */
export function useNewsEntry(): NewsEntryState {
  const client = useHttpClient()
  const [state, setState] = useState<NewsEntryState>('hidden')

  useEffect(() => {
    const controller = new AbortController()
    fetchNewsPage(client, 0, 1, controller.signal).then(
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
