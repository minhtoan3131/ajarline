import {
  EMPTY_FIELDS,
  fieldsOf,
  sameFields,
  type ArticleAdminDetail,
  type ArticleFields,
  type PublicationState,
  type Versioned,
} from './article'
import type { EditorFailure } from './editorFailure'

export type PendingMutation = 'saving' | 'reloading' | 'publishing' | 'unpublishing'

export type EditorNotice = 'created' | 'saved' | 'reloaded' | 'published' | 'unpublished'

/**
 * Editor state in three independent parts: the canonical server version
 * (`current`), the local form (`fields` against `baseline`) and the running
 * mutation (`pending`, `failure`, `notice`).
 */
export interface EditorState {
  current: Versioned<ArticleAdminDetail> | null
  baseline: ArticleFields
  fields: ArticleFields
  pending: PendingMutation | null
  submitted: ArticleFields | null
  failure: EditorFailure | null
  notice: EditorNotice | null
}

export type EditorAction =
  | { type: 'field-changed'; field: keyof ArticleFields; value: string }
  | { type: 'mutation-started'; pending: PendingMutation; submitted: ArticleFields | null }
  | { type: 'article-saved'; article: Versioned<ArticleAdminDetail>; notice: EditorNotice }
  | { type: 'article-reloaded'; article: Versioned<ArticleAdminDetail> }
  | { type: 'publication-changed'; publication: Versioned<PublicationState>; notice: EditorNotice }
  | { type: 'mutation-failed'; failure: EditorFailure }

export interface EditorStart {
  initial: Versioned<ArticleAdminDetail> | null
  initialNotice: EditorNotice | null
}

export function createEditorState({ initial, initialNotice }: EditorStart): EditorState {
  const fields = initial === null ? EMPTY_FIELDS : fieldsOf(initial.value)
  return {
    current: initial,
    baseline: fields,
    fields,
    pending: null,
    submitted: null,
    failure: null,
    notice: initialNotice,
  }
}

export function isDirty(state: EditorState): boolean {
  return !sameFields(state.fields, state.baseline)
}

export function articleEditorReducer(state: EditorState, action: EditorAction): EditorState {
  switch (action.type) {
    case 'field-changed': {
      const failure = state.failure
      // Editing the field that had an error clears that error only.
      const keepFailure = !(failure?.kind === 'field' && failure.field === action.field)
      return {
        ...state,
        fields: { ...state.fields, [action.field]: action.value },
        failure: keepFailure ? failure : null,
        notice: null,
      }
    }
    case 'mutation-started':
      return { ...state, pending: action.pending, submitted: action.submitted, failure: null, notice: null }
    case 'article-saved': {
      const canonical = fieldsOf(action.article.value)
      // Text typed while the request was running must not be lost.
      const untouched = state.submitted !== null && sameFields(state.fields, state.submitted)
      return {
        ...state,
        current: action.article,
        baseline: canonical,
        fields: untouched ? canonical : state.fields,
        pending: null,
        submitted: null,
        notice: action.notice,
      }
    }
    case 'article-reloaded': {
      const canonical = fieldsOf(action.article.value)
      return {
        ...state,
        current: action.article,
        baseline: canonical,
        fields: canonical,
        pending: null,
        submitted: null,
        failure: null,
        notice: 'reloaded',
      }
    }
    case 'publication-changed': {
      if (state.current === null) {
        return state
      }
      const { value, etag } = action.publication
      // Publication changes state and revision only; content fields stay.
      return {
        ...state,
        current: {
          etag,
          value: {
            ...state.current.value,
            status: value.status,
            revision: value.revision,
            publishedAt: value.publishedAt,
            unpublishedAt: value.unpublishedAt,
            updatedAt: value.updatedAt,
          },
        },
        pending: null,
        notice: action.notice,
      }
    }
    case 'mutation-failed':
      return { ...state, pending: null, submitted: null, failure: action.failure }
  }
}
