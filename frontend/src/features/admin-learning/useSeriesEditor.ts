import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { publishContent, unpublishContent } from '../admin-content/api/articleAdminApi'
import type { Versioned } from '../admin-content/model/article'
import { createSeries, getSeries, updateSeries } from './api/learningAdminApi'
import type { LearningTextField } from './model/learning'
import { toLearningEditorFailure } from './model/learningEditorFailure'
import {
  createLearningEditorState,
  isDirty,
  learningEditorReducer,
  type LearningEditorState,
  type LearningNotice,
} from './model/learningEditorState'
import { toSeriesEditable, type SeriesAdminDetail } from './model/series'

export interface SeriesEditorController {
  readonly state: LearningEditorState
  readonly dirty: boolean
  changeField(field: LearningTextField, value: string): void
  changeTopics(topicIds: string[]): void
  save(): Promise<void>
  reload(): Promise<void>
  publish(): Promise<void>
  unpublish(): Promise<void>
}

/**
 * Runs Series editor actions against the Admin API.
 *
 * The server response is the only source of canonical state. Every write
 * sends the ETag of the version being edited, so a change made elsewhere is
 * reported as a conflict instead of being overwritten.
 */
export function useSeriesEditor(
  initial: Versioned<SeriesAdminDetail> | null,
  initialNotice: LearningNotice | null,
  onCreated?: (seriesId: string) => void,
): SeriesEditorController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(learningEditorReducer, { initial, initialNotice }, createLearningEditorState)

  function fail(error: unknown) {
    const failure = toLearningEditorFailure(error)
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
        const created = await createSeries(client, toSeriesEditable(submitted))
        dispatch({ type: 'saved', series: created, notice: 'created' })
        onCreated?.(created.value.id)
      } else {
        const updated = await updateSeries(client, current.value.id, toSeriesEditable(submitted), current.etag)
        dispatch({ type: 'saved', series: updated, notice: 'saved' })
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
      dispatch({ type: 'reloaded', series: await getSeries(client, current.value.id) })
    } catch (error) {
      fail(error)
    }
  }

  async function changePublication(target: 'publish' | 'unpublish') {
    const current = state.current
    if (current === null || state.pending !== null) {
      return
    }
    dispatch({ type: 'mutation-started', pending: target === 'publish' ? 'publishing' : 'unpublishing', submitted: null })
    try {
      const command = target === 'publish' ? publishContent : unpublishContent
      const publication = await command(client, current.value.id, current.etag)
      dispatch({ type: 'publication-changed', publication, notice: target === 'publish' ? 'published' : 'unpublished' })
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
    publish: () => changePublication('publish'),
    unpublish: () => changePublication('unpublish'),
  }
}
