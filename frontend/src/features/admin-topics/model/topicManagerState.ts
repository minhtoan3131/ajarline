import { EMPTY_TOPIC_FIELDS, sameTopicFields, type TopicFields } from './topic'
import type { TopicFailure } from './topicFailure'

/** Which form is open. There is at most one at a time. */
export type TopicPanel = { mode: 'closed' } | { mode: 'create' }

export type PendingTopicAction = 'saving'

export type TopicNotice = 'created'

/**
 * Screen state: the open form (`panel`), the typed values (`fields`) against
 * the values the form started with (`baseline`), and the running action.
 * The Topic list itself is not here; the server owns it.
 */
export interface TopicManagerState {
  panel: TopicPanel
  baseline: TopicFields
  fields: TopicFields
  pending: PendingTopicAction | null
  failure: TopicFailure | null
  notice: TopicNotice | null
}

export type TopicManagerAction =
  | { type: 'create-opened' }
  | { type: 'panel-closed' }
  | { type: 'fields-changed'; changes: Partial<TopicFields> }
  | { type: 'mutation-started'; pending: PendingTopicAction }
  | { type: 'mutation-succeeded'; notice: TopicNotice }
  | { type: 'mutation-failed'; failure: TopicFailure }

export const INITIAL_TOPIC_MANAGER_STATE: TopicManagerState = {
  panel: { mode: 'closed' },
  baseline: EMPTY_TOPIC_FIELDS,
  fields: EMPTY_TOPIC_FIELDS,
  pending: null,
  failure: null,
  notice: null,
}

export function isDirty(state: TopicManagerState): boolean {
  return !sameTopicFields(state.fields, state.baseline)
}

export function topicManagerReducer(state: TopicManagerState, action: TopicManagerAction): TopicManagerState {
  switch (action.type) {
    case 'create-opened':
      return { ...INITIAL_TOPIC_MANAGER_STATE, panel: { mode: 'create' } }
    case 'panel-closed':
      return INITIAL_TOPIC_MANAGER_STATE
    case 'fields-changed': {
      const failure = state.failure
      // Editing the field that had an error clears that error only.
      const keepFailure = !(failure?.kind === 'field' && failure.field in action.changes)
      return {
        ...state,
        fields: { ...state.fields, ...action.changes },
        failure: keepFailure ? failure : null,
        notice: null,
      }
    }
    case 'mutation-started':
      return { ...state, pending: action.pending, failure: null, notice: null }
    case 'mutation-succeeded':
      // Success closes the form. The next list load shows the stored values.
      return { ...INITIAL_TOPIC_MANAGER_STATE, notice: action.notice }
    case 'mutation-failed':
      return { ...state, pending: null, failure: action.failure }
  }
}
