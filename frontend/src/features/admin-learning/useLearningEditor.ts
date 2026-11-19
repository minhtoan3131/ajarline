import { useReducer } from 'react'

import type { HttpClient } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { publishContent, unpublishContent } from '../admin-content/api/articleAdminApi'
import type { Versioned } from '../admin-content/model/article'
import type { LearningFields, LearningTextField, ParentFields } from './model/learning'
import type { LearningDetail } from './model/learningDetail'
import { toLearningEditorFailure } from './model/learningEditorFailure'
import {
  createLearningEditorState,
  isDirty,
  learningEditorReducer,
  type LearningEditorState,
  type LearningNotice,
} from './model/learningEditorState'
import { useChildOrder, type ChildOrderController } from './useChildOrder'

/** The three API calls that differ between a Series editor and a Module editor. */
export interface LearningSource {
  create(client: HttpClient, fields: LearningFields): Promise<Versioned<LearningDetail>>
  update(client: HttpClient, current: Versioned<LearningDetail>, fields: LearningFields): Promise<Versioned<LearningDetail>>
  load(client: HttpClient, id: string): Promise<Versioned<LearningDetail>>
}

export interface LearningEditorOptions {
  /** Form values of a new record. Used only when `initial` is `null`. */
  blank?: LearningFields
  onCreated?: (id: string) => void
}

export interface LearningEditorController {
  readonly state: LearningEditorState
  readonly dirty: boolean
  readonly order: ChildOrderController
  changeField(field: LearningTextField, value: string): void
  changeTopics(topicIds: string[]): void
  changeParent(parent: ParentFields): void
  save(): Promise<void>
  reload(): Promise<void>
  publish(): Promise<void>
  unpublish(): Promise<void>
}

/**
 * Runs the actions of a Series or Module editor against the Admin API.
 *
 * The server response is the only source of canonical state. Every write
 * sends the ETag of the version being edited, so a change made elsewhere is
 * reported as a conflict instead of being overwritten.
 */
export function useLearningEditor(
  initial: Versioned<LearningDetail> | null,
  initialNotice: LearningNotice | null,
  source: LearningSource,
  options: LearningEditorOptions = {},
): LearningEditorController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(
    learningEditorReducer,
    { initial, initialNotice, blank: options.blank },
    createLearningEditorState,
  )

  function fail(error: unknown) {
    const failure = toLearningEditorFailure(error)
    if (failure.kind === 'session-renewed') {
      // The old token is no longer valid. Get a new one so the next try works.
      void session.renewCsrfToken().catch(() => undefined)
    }
    dispatch({ type: 'mutation-failed', failure })
  }

  const order = useChildOrder(state, dispatch, fail)

  async function save() {
    if (state.pending !== null) {
      return
    }
    const current = state.current
    const submitted = state.fields
    dispatch({ type: 'mutation-started', pending: 'saving', submitted })
    try {
      if (current === null) {
        const created = await source.create(client, submitted)
        dispatch({ type: 'saved', detail: created, notice: 'created' })
        options.onCreated?.(created.value.id)
      } else {
        const updated = await source.update(client, current, submitted)
        dispatch({ type: 'saved', detail: updated, notice: 'saved' })
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
      dispatch({ type: 'reloaded', detail: await source.load(client, current.value.id) })
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
    order,
    changeField: (field, value) => dispatch({ type: 'field-changed', field, value }),
    changeTopics: (topicIds) => dispatch({ type: 'topics-changed', topicIds }),
    changeParent: (parent) => dispatch({ type: 'parent-changed', parent }),
    save,
    reload,
    publish: () => changePublication('publish'),
    unpublish: () => changePublication('unpublish'),
  }
}
