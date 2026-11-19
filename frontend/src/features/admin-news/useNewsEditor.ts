import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import type { Versioned } from '../admin-content/model/article'
import type { EditorNotice } from '../admin-content/model/articleEditorState'
import { createNews, getNews, updateNews } from './api/newsAdminApi'
import { toEditable, type NewsAdminDetail, type NewsTextField } from './model/news'
import { toNewsEditorFailure } from './model/newsEditorFailure'
import { createNewsEditorState, isDirty, newsEditorReducer, type NewsEditorState } from './model/newsEditorState'

export interface NewsEditorController {
  readonly state: NewsEditorState
  readonly dirty: boolean
  changeField(field: NewsTextField, value: string): void
  changeTopics(topicIds: string[]): void
  save(): Promise<void>
  reload(): Promise<void>
}

/**
 * Runs News editor actions against the Admin API.
 *
 * The server response is the only source of canonical state. Every write
 * sends the ETag of the version being edited, so a change made elsewhere is
 * reported as a conflict instead of being overwritten.
 */
export function useNewsEditor(
  initial: Versioned<NewsAdminDetail> | null,
  initialNotice: EditorNotice | null,
  onCreated?: (newsId: string) => void,
): NewsEditorController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(newsEditorReducer, { initial, initialNotice }, createNewsEditorState)

  function fail(error: unknown) {
    const failure = toNewsEditorFailure(error)
    if (failure.kind === 'session-renewed') {
      // The old token is no longer valid. Get a new one so the next try works.
      void session.renewCsrfToken().catch(() => undefined)
    }
    dispatch({ type: 'mutation-failed', failure })
  }

  async function save() {
    if (state.pending !== null) {
      return
    }
    const current = state.current
    const submitted = state.fields
    dispatch({ type: 'mutation-started', pending: 'saving', submitted })
    try {
      if (current === null) {
        const created = await createNews(client, toEditable(submitted))
        dispatch({ type: 'news-saved', news: created, notice: 'created' })
        onCreated?.(created.value.id)
      } else {
        const updated = await updateNews(client, current.value.id, toEditable(submitted), current.etag)
        dispatch({ type: 'news-saved', news: updated, notice: 'saved' })
      }
    } catch (error) {
      fail(error)
    }
  }

  async function reload() {
    const current = state.current
    if (current === null || state.pending !== null) {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'reloading', submitted: null })
    try {
      dispatch({ type: 'news-reloaded', news: await getNews(client, current.value.id) })
    } catch (error) {
      fail(error)
    }
  }

  return {
    state,
    dirty: isDirty(state),
    changeField: (field, value) => dispatch({ type: 'field-changed', field, value }),
    changeTopics: (topicIds) => dispatch({ type: 'topics-changed', topicIds }),
    save,
    reload,
  }
}
