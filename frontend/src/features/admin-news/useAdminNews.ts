import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import type { Versioned } from '../admin-content/model/article'
import { getNews } from './api/newsAdminApi'
import type { NewsAdminDetail } from './model/news'

export type AdminNewsLoad =
  | { status: 'loading' }
  | { status: 'ready'; news: Versioned<NewsAdminDetail> }
  | { status: 'not-found' }
  | { status: 'failed' }

/** Loads a News item for the editor by id, for direct entry and reload. */
export function useAdminNews(newsId: string): { load: AdminNewsLoad; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<AdminNewsLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setLoad({ status: 'loading' })
    getNews(client, newsId, controller.signal).then(
      (news) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', news })
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
  }, [client, newsId, attempt])

  return { load, retry: () => setAttempt((value) => value + 1) }
}
