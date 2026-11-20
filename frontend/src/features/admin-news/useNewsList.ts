import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listNews } from './api/newsAdminApi'
import type { AdminNewsList } from './model/newsList'

export type NewsListState =
  | { status: 'loading' }
  | { status: 'found'; list: AdminNewsList }
  | { status: 'failed' }

/**
 * Loads one page of News. An empty page is a found state: it is a valid
 * answer, not a failure. Every failure is the same failed state.
 */
export function useNewsList(page: number): { state: NewsListState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<NewsListState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    listNews(client, page, controller.signal).then(
      (list) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', list })
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
