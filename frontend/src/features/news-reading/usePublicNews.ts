import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicNews } from './api/publicNewsApi'
import type { PublicNews } from './model/publicNews'

export type PublicNewsState =
  | { status: 'loading' }
  | { status: 'found'; news: PublicNews }
  | { status: 'not-found' }
  | { status: 'failed' }

/**
 * Loads the public News item for a slug. Missing and non-public items share
 * one not-found state, so the page never reveals a private status. A server
 * error, including broken data, is a failed state and never looks like
 * not-found.
 */
export function usePublicNews(slug: string): { state: PublicNewsState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<PublicNewsState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchPublicNews(client, slug, controller.signal).then(
      (news) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', news })
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
