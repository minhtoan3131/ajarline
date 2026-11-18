import type { PublicationState, Versioned } from '../../admin-content/model/article'
import {
  EMPTY_LEARNING_FIELDS,
  learningFieldsOf,
  sameLearningFields,
  type LearningFields,
  type LearningTextField,
} from './learning'
import type { LearningEditorFailure } from './learningEditorFailure'
import type { SeriesAdminDetail } from './series'

export type LearningPending = 'saving' | 'reloading' | 'publishing' | 'unpublishing'

export type LearningNotice = 'created' | 'saved' | 'reloaded' | 'published' | 'unpublished'

/**
 * Editor state in three independent parts: the canonical server version
 * (`current`), the local form (`fields` against `baseline`) and the running
 * mutation (`pending`, `failure`, `notice`). Every successful action replaces
 * the ETag in `current`, so the next write never sends a stale one.
 */
export interface LearningEditorState {
  current: Versioned<SeriesAdminDetail> | null
  baseline: LearningFields
  fields: LearningFields
  pending: LearningPending | null
  submitted: LearningFields | null
  failure: LearningEditorFailure | null
  notice: LearningNotice | null
}

export type LearningEditorAction =
  | { type: 'field-changed'; field: LearningTextField; value: string }
  | { type: 'topics-changed'; topicIds: string[] }
  | { type: 'mutation-started'; pending: LearningPending; submitted: LearningFields | null }
  | { type: 'saved'; series: Versioned<SeriesAdminDetail>; notice: LearningNotice }
  | { type: 'reloaded'; series: Versioned<SeriesAdminDetail> }
  | { type: 'publication-changed'; publication: Versioned<PublicationState>; notice: LearningNotice }
  | { type: 'mutation-failed'; failure: LearningEditorFailure }

export interface LearningEditorStart {
  initial: Versioned<SeriesAdminDetail> | null
  initialNotice: LearningNotice | null
}

export function createLearningEditorState({ initial, initialNotice }: LearningEditorStart): LearningEditorState {
  const fields = initial === null ? EMPTY_LEARNING_FIELDS : learningFieldsOf(initial.value)
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

export function isDirty(state: LearningEditorState): boolean {
  return !sameLearningFields(state.fields, state.baseline)
}

export function learningEditorReducer(state: LearningEditorState, action: LearningEditorAction): LearningEditorState {
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
    case 'topics-changed': {
      const failure = state.failure
      // Changing the Topic choice clears the Topic error only.
      const keepFailure = !(failure?.kind === 'field' && failure.field === 'topicIds')
      return {
        ...state,
        fields: { ...state.fields, topicIds: action.topicIds },
        failure: keepFailure ? failure : null,
        notice: null,
      }
    }
    case 'mutation-started':
      return { ...state, pending: action.pending, submitted: action.submitted, failure: null, notice: null }
    case 'saved': {
      const canonical = learningFieldsOf(action.series.value)
      // Text typed while the request was running must not be lost.
      const untouched = state.submitted !== null && sameLearningFields(state.fields, state.submitted)
      return {
        ...state,
        current: action.series,
        baseline: canonical,
        fields: untouched ? canonical : state.fields,
        pending: null,
        submitted: null,
        notice: action.notice,
      }
    }
    case 'reloaded': {
      const canonical = learningFieldsOf(action.series.value)
      return {
        ...state,
        current: action.series,
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
      // Publication changes state and revision only. Fields and Modules stay.
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
