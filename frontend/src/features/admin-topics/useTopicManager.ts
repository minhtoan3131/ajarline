import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { createTopic } from './api/topicAdminApi'
import { toCreatable, type TopicFields } from './model/topic'
import { toTopicFailure } from './model/topicFailure'
import {
  INITIAL_TOPIC_MANAGER_STATE,
  isDirty,
  topicManagerReducer,
  type TopicManagerState,
} from './model/topicManagerState'

export interface TopicManagerController {
  readonly state: TopicManagerState
  readonly dirty: boolean
  openCreate(): void
  close(): void
  changeFields(changes: Partial<TopicFields>): void
  save(): Promise<void>
}

/**
 * Runs Topic form actions against the Admin API.
 *
 * The screen never says a change happened before the server answers. After a
 * successful change it calls `onTopicsChanged` so the list is loaded again.
 */
export function useTopicManager(onTopicsChanged: () => void): TopicManagerController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(topicManagerReducer, INITIAL_TOPIC_MANAGER_STATE)

  function fail(error: unknown) {
    const failure = toTopicFailure(error)
    if (failure.kind === 'session-renewed') {
      // The old token is no longer valid. Get a new one so the next try works.
      void session.renewCsrfToken().catch(() => undefined)
    }
    dispatch({ type: 'mutation-failed', failure })
  }

  async function save() {
    if (state.pending !== null || state.panel.mode !== 'create') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'saving' })
    try {
      await createTopic(client, toCreatable(state.fields))
      dispatch({ type: 'mutation-succeeded', notice: 'created' })
      onTopicsChanged()
    } catch (error) {
      fail(error)
    }
  }

  return {
    state,
    dirty: isDirty(state),
    openCreate: () => dispatch({ type: 'create-opened' }),
    close: () => dispatch({ type: 'panel-closed' }),
    changeFields: (changes) => dispatch({ type: 'fields-changed', changes }),
    save,
  }
}
