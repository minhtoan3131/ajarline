import { useReducer } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { useAdminSession } from '../admin-auth/AdminSessionContext'
import { createConcept, deleteConcept, updateConcept } from './api/conceptAdminApi'
import {
  toCreatable,
  toEditable,
  type ConceptAdminItem,
  type ConceptFieldChanges,
  type ExpressionChanges,
} from './model/concept'
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
  openEdit(concept: ConceptAdminItem): void
  close(): void
  changeFields(changes: ConceptFieldChanges): void
  addExpression(): void
  changeExpression(rowKey: string, changes: ExpressionChanges): void
  removeExpression(rowKey: string): void
  save(): Promise<void>
  remove(): Promise<void>
}

/**
 * Runs Concept form actions against the Admin API.
 *
 * The screen never says a change happened before the server answers. After a
 * successful change it calls `onConceptsChanged` so the list is loaded again.
 */
export function useConceptManager(onConceptsChanged: () => void): ConceptManagerController {
  const client = useHttpClient()
  const session = useAdminSession()
  const [state, dispatch] = useReducer(conceptManagerReducer, INITIAL_CONCEPT_MANAGER_STATE)

  function fail(error: unknown) {
    const failure = toConceptFailure(error)
    if (failure.kind === 'session-renewed') {
      // The old token is no longer valid. Get a new one so the next try works.
      void session.renewCsrfToken().catch(() => undefined)
    }
    if (failure.kind === 'not-found') {
      // The Concept is gone; load the list again so its row disappears.
      onConceptsChanged()
    }
    dispatch({ type: 'mutation-failed', failure })
  }

  async function save() {
    const panel = state.panel
    if (state.pending !== null || panel.mode === 'closed') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'saving' })
    try {
      if (panel.mode === 'create') {
        await createConcept(client, toCreatable(state.fields))
        dispatch({ type: 'mutation-succeeded', notice: 'created' })
      } else {
        await updateConcept(client, panel.concept.id, toEditable(state.fields))
        dispatch({ type: 'mutation-succeeded', notice: 'saved' })
      }
      onConceptsChanged()
    } catch (error) {
      fail(error)
    }
  }

  async function remove() {
    const panel = state.panel
    if (state.pending !== null || panel.mode !== 'edit') {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'deleting' })
    try {
      await deleteConcept(client, panel.concept.id)
      dispatch({ type: 'mutation-succeeded', notice: 'deleted' })
      onConceptsChanged()
    } catch (error) {
      fail(error)
    }
  }

  return {
    state,
    dirty: isDirty(state),
    openCreate: () => dispatch({ type: 'create-opened' }),
    openEdit: (concept) => dispatch({ type: 'edit-opened', concept }),
    close: () => dispatch({ type: 'panel-closed' }),
    changeFields: (changes) => dispatch({ type: 'fields-changed', changes }),
    addExpression: () => dispatch({ type: 'expression-added' }),
    changeExpression: (rowKey, changes) => dispatch({ type: 'expression-changed', rowKey, changes }),
    removeExpression: (rowKey) => dispatch({ type: 'expression-removed', rowKey }),
    save,
    remove,
  }
}
