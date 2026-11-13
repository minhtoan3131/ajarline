import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicArticle } from './api/publicArticleApi'
import type { PublicArticle } from './model/publicArticle'

export type PublicArticleState =
  | { status: 'loading' }
  | { status: 'found'; article: PublicArticle }
  | { status: 'not-found' }
  | { status: 'failed' }

/**
 * Loads the public Article for a slug. Missing and non-public Articles share
 * one not-found state, so the page never reveals a private status.
 */
export function usePublicArticle(slug: string): { state: PublicArticleState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<PublicArticleState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchPublicArticle(client, slug, controller.signal).then(
      (article) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', article })
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
