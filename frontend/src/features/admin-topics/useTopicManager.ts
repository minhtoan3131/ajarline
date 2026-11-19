import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { createTopic, deleteTopic, updateTopic } from './api/topicAdminApi'
import { toCreatable, toEditable, type TopicAdminItem, type TopicFields } from './model/topic'
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
  openEdit(topic: TopicAdminItem): void
  close(): void
  changeFields(changes: Partial<TopicFields>): void
  save(): Promise<void>
  remove(): Promise<void>
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
    if (failure.kind === 'not-found') {
      // The Topic is gone; load the list again so its row disappears.
      onTopicsChanged()
    }
    dispatch({ type: 'mutation-failed', failure })
  }

  async function save() {
    const panel = state.panel
    if (state.pending !== null || panel.mode === 'closed') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'saving' })
    try {
      if (panel.mode === 'create') {
        await createTopic(client, toCreatable(state.fields))
        dispatch({ type: 'mutation-succeeded', notice: 'created' })
      } else {
        await updateTopic(client, panel.topic.id, toEditable(state.fields))
        dispatch({ type: 'mutation-succeeded', notice: 'saved' })
      }
      onTopicsChanged()
    } catch (error) {
      fail(error)
    }
  }

  async function remove() {
    const panel = state.panel
    if (state.pending !== null || panel.mode !== 'edit') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'deleting' })
    try {
      await deleteTopic(client, panel.topic.id)
      dispatch({ type: 'mutation-succeeded', notice: 'deleted' })
      onTopicsChanged()
    } catch (error) {
      fail(error)
    }
  }

  return {
    state,
    dirty: isDirty(state),
    openCreate: () => dispatch({ type: 'create-opened' }),
    openEdit: (topic) => dispatch({ type: 'edit-opened', topic }),
    close: () => dispatch({ type: 'panel-closed' }),
    changeFields: (changes) => dispatch({ type: 'fields-changed', changes }),
    save,
    remove,
  }
}
