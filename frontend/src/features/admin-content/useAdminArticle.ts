import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { getArticle } from './api/articleAdminApi'
import type { ArticleAdminDetail, Versioned } from './model/article'

export type AdminArticleLoad =
  | { status: 'loading' }
  | { status: 'ready'; article: Versioned<ArticleAdminDetail> }
  | { status: 'not-found' }
  | { status: 'failed' }

/** Loads an Article for the editor by id, for direct entry and reload. */
export function useAdminArticle(articleId: string): { load: AdminArticleLoad; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<AdminArticleLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setLoad({ status: 'loading' })
    getArticle(client, articleId, controller.signal).then(
      (article) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', article })
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
  }, [client, articleId, attempt])

  return { load, retry: () => setAttempt((value) => value + 1) }
}
