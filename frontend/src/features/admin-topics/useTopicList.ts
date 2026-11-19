import { useCallback, useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { listTopics } from './api/topicAdminApi'
import type { TopicAdminItem } from './model/topic'

export type TopicListLoad =
  | { status: 'loading' }
  | { status: 'ready'; topics: TopicAdminItem[] }
  | { status: 'failed' }

/**
 * Loads the Topic list. The server owns the order, so after any change the
 * screen asks for the list again instead of editing its own copy.
 */
export function useTopicList(): { load: TopicListLoad; reload: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<TopicListLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    // Keep the old list on screen while a reload runs. Only the first load
    // and a retry after a failure show "loading".
    setLoad((current) => (current.status === 'ready' ? current : { status: 'loading' }))
    listTopics(client, controller.signal).then(
      (topics) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', topics })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        setLoad({ status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, attempt])

  const reload = useCallback(() => setAttempt((value) => value + 1), [])
  return { load, reload }
}
