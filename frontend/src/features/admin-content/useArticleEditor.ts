import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { createArticle, getArticle, publishContent, unpublishContent, updateArticle } from './api/articleAdminApi'
import { toEditable, type ArticleAdminDetail, type ArticleTextField, type Placement, type Versioned } from './model/article'
import { articleEditorReducer, createEditorState, isDirty, type EditorNotice, type EditorState } from './model/articleEditorState'
import { toEditorFailure } from './model/editorFailure'

export interface ArticleEditorController {
  readonly state: EditorState
  readonly dirty: boolean
  changeField(field: ArticleTextField, value: string): void
  changeTopics(topicIds: string[]): void
  changePlacement(placement: Placement | null): void
  save(): Promise<void>
  reload(): Promise<void>
  publish(): Promise<void>
  unpublish(): Promise<void>
}

/**
 * Runs Article editor actions against the Admin API.
 *
 * The server response is the only source of canonical state. Every write
 * sends the ETag of the version being edited, so a change made elsewhere is
 * reported as a conflict instead of being overwritten.
 */
export function useArticleEditor(
  initial: Versioned<ArticleAdminDetail> | null,
  initialNotice: EditorNotice | null,
  onCreated?: (articleId: string) => void,
): ArticleEditorController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(articleEditorReducer, { initial, initialNotice }, createEditorState)

  function fail(error: unknown) {
    const failure = toEditorFailure(error)
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
        const created = await createArticle(client, toEditable(submitted))
        dispatch({ type: 'article-saved', article: created, notice: 'created' })
        onCreated?.(created.value.id)
      } else {
        const updated = await updateArticle(client, current.value.id, toEditable(submitted), current.etag)
        dispatch({ type: 'article-saved', article: updated, notice: 'saved' })
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
      dispatch({ type: 'article-reloaded', article: await getArticle(client, current.value.id) })
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
    changePlacement: (placement) => dispatch({ type: 'placement-changed', placement }),
    save,
    reload,
    publish: () => changePublication('publish'),
    unpublish: () => changePublication('unpublish'),
  }
}
