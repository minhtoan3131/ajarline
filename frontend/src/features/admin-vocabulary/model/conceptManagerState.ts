import {
  EMPTY_CONCEPT_FIELDS,
  MAX_EXPRESSIONS,
  sameConceptFields,
  type ConceptFieldChanges,
  type ConceptFields,
  type ExpressionChanges,
  type ExpressionRow,
} from './concept'
import type { ConceptFailure } from './conceptFailure'

/** Which form is open. There is at most one at a time. */
export type ConceptPanel = { mode: 'closed' } | { mode: 'create' }

export type PendingConceptAction = 'saving'

export type ConceptNotice = 'created'

/**
 * Screen state: the open form (`panel`), the typed values (`fields`) against
 * the values the form started with (`baseline`), and the running action.
 * `nextRow` gives the key of the next expression row, so the reducer stays pure.
 * The Concept list itself is not here; the server owns it.
 */
export interface ConceptManagerState {
  panel: ConceptPanel
  baseline: ConceptFields
  fields: ConceptFields
  nextRow: number
  pending: PendingConceptAction | null
  failure: ConceptFailure | null
  notice: ConceptNotice | null
}

export type ConceptManagerAction =
  | { type: 'create-opened' }
  | { type: 'panel-closed' }
  | { type: 'fields-changed'; changes: ConceptFieldChanges }
  | { type: 'expression-added' }
  | { type: 'expression-changed'; rowKey: string; changes: ExpressionChanges }
  | { type: 'expression-removed'; rowKey: string }
  | { type: 'mutation-started'; pending: PendingConceptAction }
  | { type: 'mutation-succeeded'; notice: ConceptNotice }
  | { type: 'mutation-failed'; failure: ConceptFailure }

export const INITIAL_CONCEPT_MANAGER_STATE: ConceptManagerState = {
  panel: { mode: 'closed' },
  baseline: EMPTY_CONCEPT_FIELDS,
  fields: EMPTY_CONCEPT_FIELDS,
  nextRow: 0,
  pending: null,
  failure: null,
  notice: null,
}

export function isDirty(state: ConceptManagerState): boolean {
  return !sameConceptFields(state.fields, state.baseline)
}

function withRows(state: ConceptManagerState, expressions: ExpressionRow[]): ConceptManagerState {
  return { ...state, fields: { ...state.fields, expressions }, notice: null }
}

export function conceptManagerReducer(state: ConceptManagerState, action: ConceptManagerAction): ConceptManagerState {
  switch (action.type) {
    case 'create-opened':
      return { ...INITIAL_CONCEPT_MANAGER_STATE, panel: { mode: 'create' } }
    case 'panel-closed':
      return INITIAL_CONCEPT_MANAGER_STATE
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
    case 'expression-added': {
      if (state.fields.expressions.length >= MAX_EXPRESSIONS) {
        return state
      }
      const row: ExpressionRow = { rowKey: `row-${state.nextRow}`, expression: '', languageHint: '', autoExpand: false }
      return { ...withRows(state, [...state.fields.expressions, row]), nextRow: state.nextRow + 1 }
    }
    case 'expression-changed':
      return withRows(
        state,
        state.fields.expressions.map((row) => (row.rowKey === action.rowKey ? { ...row, ...action.changes } : row)),
      )
    case 'expression-removed':
      return withRows(
        state,
        state.fields.expressions.filter((row) => row.rowKey !== action.rowKey),
      )
    case 'mutation-started':
      return { ...state, pending: action.pending, failure: null, notice: null }
    case 'mutation-succeeded':
      // Success closes the form. The next list load shows the stored values.
      return { ...INITIAL_CONCEPT_MANAGER_STATE, notice: action.notice }
    case 'mutation-failed':
      return { ...state, pending: null, failure: action.failure }
  }
}
