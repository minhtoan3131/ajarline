import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { createConcept } from './api/conceptAdminApi'
import { toCreatable, type ConceptFieldChanges, type ExpressionChanges } from './model/concept'
import { toConceptFailure } from './model/conceptFailure'
import {
  conceptManagerReducer,
  INITIAL_CONCEPT_MANAGER_STATE,
  isDirty,
  type ConceptManagerState,
} from './model/conceptManagerState'

export interface ConceptManagerController {
  readonly state: ConceptManagerState
  readonly dirty: boolean
  openCreate(): void
  close(): void
  changeFields(changes: ConceptFieldChanges): void
  addExpression(): void
  changeExpression(rowKey: string, changes: ExpressionChanges): void
  removeExpression(rowKey: string): void
  save(): Promise<void>
}

/**
 * Runs Concept form actions against the Admin API.
 *
 * The screen never says a change happened before the server answers. After a
 * successful change it calls `onConceptsChanged` so the list is loaded again.
 */
export function useConceptManager(onConceptsChanged: () => void): ConceptManagerController {
  const client = useHttpClient()
  const [state, dispatch] = useReducer(conceptManagerReducer, INITIAL_CONCEPT_MANAGER_STATE)

  function fail(error: unknown) {
    dispatch({ type: 'mutation-failed', failure: toConceptFailure(error) })
  }

  async function save() {
    if (state.pending !== null || state.panel.mode === 'closed') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'saving' })
    try {
      await createConcept(client, toCreatable(state.fields))
      dispatch({ type: 'mutation-succeeded', notice: 'created' })
      onConceptsChanged()
    } catch (error) {
      fail(error)
    }
  }

  return {
    state,
    dirty: isDirty(state),
    openCreate: () => dispatch({ type: 'create-opened' }),
    close: () => dispatch({ type: 'panel-closed' }),
    changeFields: (changes) => dispatch({ type: 'fields-changed', changes }),
    addExpression: () => dispatch({ type: 'expression-added' }),
    changeExpression: (rowKey, changes) => dispatch({ type: 'expression-changed', rowKey, changes }),
    removeExpression: (rowKey) => dispatch({ type: 'expression-removed', rowKey }),
    save,
  }
}
