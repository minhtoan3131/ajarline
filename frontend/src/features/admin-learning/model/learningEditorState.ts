import type { PublicationState, Versioned } from '../../admin-content/model/article'
import {
  EMPTY_LEARNING_FIELDS,
  sameLearningFields,
  type ChildOrder,
  type LearningChild,
  type LearningFields,
  type LearningTextField,
  type ParentFields,
} from './learning'
import { childrenOf, fieldsOfDetail, withChildren, type LearningDetail } from './learningDetail'
import type { LearningEditorFailure } from './learningEditorFailure'

export type LearningPending = 'saving' | 'ordering' | 'reloading' | 'publishing' | 'unpublishing'

export type LearningNotice = 'created' | 'saved' | 'order-saved' | 'reloaded' | 'published' | 'unpublished'

/**
 * Editor state in four independent parts: the canonical server version
 * (`current`), the local form (`fields` against `baseline`), the order the
 * Admin is arranging (`order`) and the running mutation (`pending`,
 * `failure`, `notice`). Every successful action replaces the ETag in
 * `current`, so the next write never sends a stale one.
 */
export interface LearningEditorState {
  current: Versioned<LearningDetail> | null
  baseline: LearningFields
  fields: LearningFields
  /** The children in the order the Admin arranged, not saved yet. `null` when there is no such change. */
  order: LearningChild[] | null
  pending: LearningPending | null
  submitted: LearningFields | null
  failure: LearningEditorFailure | null
  notice: LearningNotice | null
}

export type LearningEditorAction =
  | { type: 'field-changed'; field: LearningTextField; value: string }
  | { type: 'topics-changed'; topicIds: string[] }
  | { type: 'parent-changed'; parent: ParentFields }
  | { type: 'order-changed'; order: LearningChild[] }
  | { type: 'mutation-started'; pending: LearningPending; submitted: LearningFields | null }
  | { type: 'saved'; detail: Versioned<LearningDetail>; notice: LearningNotice }
  | { type: 'order-saved'; order: Versioned<ChildOrder> }
  | { type: 'reloaded'; detail: Versioned<LearningDetail> }
  | { type: 'publication-changed'; publication: Versioned<PublicationState>; notice: LearningNotice }
  | { type: 'mutation-failed'; failure: LearningEditorFailure }

export interface LearningEditorStart {
  initial: Versioned<LearningDetail> | null
  initialNotice: LearningNotice | null
  /** Form values of a new record. Used only when `initial` is `null`. */
  blank?: LearningFields
}

export function createLearningEditorState({
  initial,
  initialNotice,
  blank = EMPTY_LEARNING_FIELDS,
}: LearningEditorStart): LearningEditorState {
  const fields = initial === null ? blank : fieldsOfDetail(initial.value)
  return {
    current: initial,
    baseline: fields,
    fields,
    order: null,
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
    case 'parent-changed': {
      const failure = state.failure
      // Changing the Series or the position clears the parent error only.
      const keepFailure = !(failure?.kind === 'field' && failure.field === 'parent')
      return {
        ...state,
        fields: { ...state.fields, parent: action.parent },
        failure: keepFailure ? failure : null,
        notice: null,
      }
    }
    case 'order-changed': {
      const failure = state.failure
      // Arranging again clears the order error only.
      const keepFailure = failure?.kind !== 'order'
      return { ...state, order: action.order, failure: keepFailure ? failure : null, notice: null }
    }
    case 'mutation-started':
      return { ...state, pending: action.pending, submitted: action.submitted, failure: null, notice: null }
    case 'saved': {
      const canonical = fieldsOfDetail(action.detail.value)
      // Text typed while the request was running must not be lost.
      const untouched = state.submitted !== null && sameLearningFields(state.fields, state.submitted)
      return {
        ...state,
        current: action.detail,
        baseline: canonical,
        fields: untouched ? canonical : state.fields,
        // The response carries the children in the server's order, so an unsaved arrangement is over.
        order: null,
        pending: null,
        submitted: null,
        notice: action.notice,
      }
    }
    case 'order-saved': {
      if (state.current === null) {
        return state
      }
      const { value: saved, etag } = action.order
      const positionOf = new Map(saved.children.map((child) => [child.id, child.position]))
      const children = childrenOf(state.current.value)
        .map((child) => ({ ...child, position: positionOf.get(child.id) ?? child.position }))
        .sort((left, right) => left.position - right.position)
      // The reorder changes the parent's revision. Take it and the new ETag now, so the next save of the form matches.
      return {
        ...state,
        current: { etag, value: { ...withChildren(state.current.value, children), revision: saved.revision } },
        order: null,
        pending: null,
        submitted: null,
        notice: 'order-saved',
      }
    }
    case 'reloaded': {
      const canonical = fieldsOfDetail(action.detail.value)
      return {
        ...state,
        current: action.detail,
        baseline: canonical,
        fields: canonical,
        order: null,
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
      // Publication changes state and revision only. Fields and children stay.
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
