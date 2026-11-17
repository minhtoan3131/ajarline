import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchStandaloneArticles } from './api/publicArticleCollectionApi'
import type { ArticleCollectionPage } from './model/articleCollection'

export type StandaloneIndexState =
  | { status: 'loading' }
  | { status: 'found'; collection: ArticleCollectionPage }
  | { status: 'failed' }

/**
 * Loads one page of standalone Articles. An empty page is a found state: it
 * is a valid answer, not a failure. Every failure is the same failed state.
 */
export function useStandaloneIndex(page: number): { state: StandaloneIndexState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<StandaloneIndexState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchStandaloneArticles(client, page, undefined, controller.signal).then(
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
