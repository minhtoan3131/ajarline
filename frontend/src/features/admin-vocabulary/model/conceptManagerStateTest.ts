import { describe, expect, it } from 'vitest'

import { MAX_EXPRESSIONS } from './concept'
import {
  conceptManagerReducer,
  INITIAL_CONCEPT_MANAGER_STATE,
  isDirty,
  type ConceptManagerAction,
  type ConceptManagerState,
} from './conceptManagerState'

function reduce(state: ConceptManagerState, ...actions: ConceptManagerAction[]): ConceptManagerState {
  return actions.reduce((current, action) => conceptManagerReducer(current, action), state)
}

function openedCreate(): ConceptManagerState {
  return reduce(INITIAL_CONCEPT_MANAGER_STATE, { type: 'create-opened' })
}

function openedWithRows(count: number): ConceptManagerState {
  const additions = Array.from({ length: count }, (): ConceptManagerAction => ({ type: 'expression-added' }))
  return reduce(openedCreate(), ...additions)
}

describe('conceptManagerReducer', () => {
  it('reduce_ShouldStartWithEmptyActiveForm_WhenCreateIsOpened', () => {
    const state = openedCreate()

    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields).toEqual({ key: '', displayName: '', description: '', active: true, expressions: [] })
    expect(state.nextRow).toBe(0)
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldMarkFormDirty_WhenFieldChanges', () => {
    const state = reduce(openedCreate(), { type: 'fields-changed', changes: { displayName: 'Thread' } })

    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldAddEmptyRowWithNewKey_WhenExpressionIsAdded', () => {
    const state = reduce(openedCreate(), { type: 'expression-added' })

    expect(state.fields.expressions).toEqual([
      { rowKey: 'row-0', expression: '', languageHint: '', autoExpand: false },
    ])
    expect(state.nextRow).toBe(1)
    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldIgnoreTheRow_WhenFiftyRowsAlreadyExist', () => {
    const full = openedWithRows(MAX_EXPRESSIONS)

    const next = reduce(full, { type: 'expression-added' })

    expect(next).toBe(full)
    expect(next.fields.expressions).toHaveLength(MAX_EXPRESSIONS)
    expect(next.nextRow).toBe(MAX_EXPRESSIONS)
  })

  it('reduce_ShouldKeepOtherRowsAndKeys_WhenMiddleRowIsRemoved', () => {
    const state = reduce(
      openedWithRows(3),
      { type: 'expression-changed', rowKey: 'row-0', changes: { expression: 'alpha' } },
      { type: 'expression-changed', rowKey: 'row-1', changes: { expression: 'beta' } },
      { type: 'expression-changed', rowKey: 'row-2', changes: { expression: 'gamma' } },
      { type: 'expression-removed', rowKey: 'row-1' },
    )

    expect(state.fields.expressions.map((row) => [row.rowKey, row.expression])).toEqual([
      ['row-0', 'alpha'],
      ['row-2', 'gamma'],
    ])
  })

  it('reduce_ShouldNotReuseKey_WhenRowIsAddedAfterARowWasRemoved', () => {
    const state = reduce(
      openedWithRows(2),
      { type: 'expression-removed', rowKey: 'row-1' },
      { type: 'expression-added' },
    )

    expect(state.fields.expressions.map((row) => row.rowKey)).toEqual(['row-0', 'row-2'])
  })

  it('reduce_ShouldChangeOnlyThatRow_WhenExpressionIsChanged', () => {
    const state = reduce(
      openedWithRows(2),
      { type: 'expression-changed', rowKey: 'row-1', changes: { languageHint: 'vi', autoExpand: true } },
    )

    expect(state.fields.expressions[0]).toEqual({ rowKey: 'row-0', expression: '', languageHint: '', autoExpand: false })
    expect(state.fields.expressions[1]).toEqual({ rowKey: 'row-1', expression: '', languageHint: 'vi', autoExpand: true })
  })

  it('reduce_ShouldNotBeDirty_WhenRowIsAddedAndThenRemoved', () => {
    const state = reduce(openedCreate(), { type: 'expression-added' }, { type: 'expression-removed', rowKey: 'row-0' })

    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldClearOnlyTheKeyError_WhenKeyIsEdited', () => {
    const failed = reduce(openedCreate(), {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'key', message: 'Key này đã được Concept khác sử dụng.' },
    })

    const afterOtherField = reduce(failed, { type: 'fields-changed', changes: { displayName: 'Thread' } })
    const afterKey = reduce(afterOtherField, { type: 'fields-changed', changes: { key: 'thread-2' } })

    expect(afterOtherField.failure).not.toBeNull()
    expect(afterKey.failure).toBeNull()
  })

  it('reduce_ShouldCloseFormAndShowNotice_WhenMutationSucceeds', () => {
    const state = reduce(
      openedWithRows(1),
      { type: 'fields-changed', changes: { displayName: 'Thread' } },
      { type: 'mutation-started', pending: 'saving' },
      { type: 'mutation-succeeded', notice: 'created' },
    )

    expect(state.panel).toEqual({ mode: 'closed' })
    expect(state.notice).toBe('created')
    expect(state.pending).toBeNull()
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepTypedValuesAndRows_WhenMutationFails', () => {
    const state = reduce(
      openedWithRows(2),
      { type: 'fields-changed', changes: { displayName: 'Thread' } },
      { type: 'expression-changed', rowKey: 'row-1', changes: { expression: 'thread' } },
      { type: 'mutation-started', pending: 'saving' },
      { type: 'mutation-failed', failure: { kind: 'duplicate-expression' } },
    )

    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields.displayName).toBe('Thread')
    expect(state.fields.expressions.map((row) => [row.rowKey, row.expression])).toEqual([
      ['row-0', ''],
      ['row-1', 'thread'],
    ])
    expect(state.pending).toBeNull()
    expect(state.failure).toEqual({ kind: 'duplicate-expression' })
  })
})
