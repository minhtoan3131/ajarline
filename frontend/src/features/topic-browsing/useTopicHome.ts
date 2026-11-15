import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicTopic, fetchTopicContent } from './api/publicTopicApi'
import type { PublicTopic, TopicContentPage } from './model/publicTopic'

export type TopicHomeState =
  | { status: 'loading' }
  | { status: 'found'; topic: PublicTopic; content: TopicContentPage }
  | { status: 'not-found' }
  | { status: 'failed' }

/**
 * Loads a public Topic and one page of its content together. A missing Topic
 * and a Topic without public content share one not-found state.
 */
export function useTopicHome(slug: string, page: number): { state: TopicHomeState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<TopicHomeState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    Promise.all([
      fetchPublicTopic(client, slug, controller.signal),
      fetchTopicContent(client, slug, page, controller.signal),
    ]).then(
      ([topic, content]) => {
        if (!controller.signal.aborted) {
          setState({ status: 'found', topic, content })
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
  }, [client, slug, page, attempt])

  return { state, retry: () => setAttempt((value) => value + 1) }
}
