import { useEffect, useState } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchStandaloneArticles } from './api/publicArticleCollectionApi'

export type StandaloneEntryState = 'hidden' | 'visible'

/**
 * Tells whether the Home page should link to the standalone Article Index.
 * It asks for one Article only, so the Home page learns "any or none" and
 * never a count. Loading, an empty answer and every failure all keep the link
 * hidden: it is a side entry, and Home must work without it.
 */
export function useStandaloneEntry(): StandaloneEntryState {
  const client = useHttpClient()
  const [state, setState] = useState<StandaloneEntryState>('hidden')

  useEffect(() => {
    const controller = new AbortController()
    fetchStandaloneArticles(client, 0, 1, controller.signal).then(
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
