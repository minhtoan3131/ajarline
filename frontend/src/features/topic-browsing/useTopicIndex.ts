import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicTopics } from './api/publicTopicApi'
import type { PublicTopicRef } from './model/publicTopic'

export type TopicIndexState =
  | { status: 'loading' }
  | { status: 'ready'; topics: PublicTopicRef[] }
  | { status: 'failed' }

/** Loads the public Topic list. An empty list is a ready state, not a failure. */
export function useTopicIndex(): { state: TopicIndexState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<TopicIndexState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchPublicTopics(client, controller.signal).then(
      (topics) => {
        if (!controller.signal.aborted) {
          setState({ status: 'ready', topics })
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
  }, [client, attempt])

  return { state, retry: () => setAttempt((value) => value + 1) }
}
