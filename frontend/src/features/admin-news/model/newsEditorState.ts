import type { PublicationState, Versioned } from '../../admin-content/model/article'
import type { EditorNotice, PendingMutation } from '../../admin-content/model/articleEditorState'
import {
  EMPTY_NEWS_FIELDS,
  fieldsOf,
  sameFields,
  type NewsAdminDetail,
  type NewsFields,
  type NewsTextField,
} from './news'
import type { NewsEditorFailure } from './newsEditorFailure'

/**
 * Editor state in three independent parts: the canonical server version
 * (`current`), the local form (`fields` against `baseline`) and the running
 * mutation (`pending`, `failure`, `notice`).
 */
export interface NewsEditorState {
  current: Versioned<NewsAdminDetail> | null
  baseline: NewsFields
  fields: NewsFields
  pending: PendingMutation | null
  submitted: NewsFields | null
  failure: NewsEditorFailure | null
  notice: EditorNotice | null
}

export type NewsEditorAction =
  | { type: 'field-changed'; field: NewsTextField; value: string }
  | { type: 'topics-changed'; topicIds: string[] }
  | { type: 'mutation-started'; pending: PendingMutation; submitted: NewsFields | null }
  | { type: 'news-saved'; news: Versioned<NewsAdminDetail>; notice: EditorNotice }
  | { type: 'news-reloaded'; news: Versioned<NewsAdminDetail> }
  | { type: 'publication-changed'; publication: Versioned<PublicationState>; notice: EditorNotice }
  | { type: 'mutation-failed'; failure: NewsEditorFailure }

export interface NewsEditorStart {
  initial: Versioned<NewsAdminDetail> | null
  initialNotice: EditorNotice | null
}

export function createNewsEditorState({ initial, initialNotice }: NewsEditorStart): NewsEditorState {
  const fields = initial === null ? EMPTY_NEWS_FIELDS : fieldsOf(initial.value)
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

export function isDirty(state: NewsEditorState): boolean {
  return !sameFields(state.fields, state.baseline)
}

const SOURCE_FIELDS: ReadonlySet<string> = new Set(['sourceName', 'sourceUrl', 'sourcePublishedAt'])

/** True when editing `field` answers the error: the field itself, or any part of the source block. */
function answersFailure(failure: NewsEditorFailure | null, field: keyof NewsFields): boolean {
  if (failure?.kind !== 'field') {
    return false
  }
  return failure.field === field || (failure.field === 'source' && SOURCE_FIELDS.has(field))
}

export function newsEditorReducer(state: NewsEditorState, action: NewsEditorAction): NewsEditorState {
  switch (action.type) {
    case 'field-changed':
      return {
        ...state,
        fields: { ...state.fields, [action.field]: action.value },
        // Editing the field that had an error clears that error only.
        failure: answersFailure(state.failure, action.field) ? null : state.failure,
        notice: null,
      }
    case 'topics-changed':
      return {
        ...state,
        fields: { ...state.fields, topicIds: action.topicIds },
        failure: answersFailure(state.failure, 'topicIds') ? null : state.failure,
        notice: null,
      }
    case 'mutation-started':
      return { ...state, pending: action.pending, submitted: action.submitted, failure: null, notice: null }
    case 'news-saved': {
      const canonical = fieldsOf(action.news.value)
      // Text typed while the request was running must not be lost.
      const untouched = state.submitted !== null && sameFields(state.fields, state.submitted)
      return {
        ...state,
        current: action.news,
        baseline: canonical,
        fields: untouched ? canonical : state.fields,
        pending: null,
        submitted: null,
        notice: action.notice,
      }
    }
    case 'news-reloaded': {
      const canonical = fieldsOf(action.news.value)
      return {
        ...state,
        current: action.news,
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
