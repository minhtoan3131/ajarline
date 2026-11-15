import { describe, expect, it } from 'vitest'

import {
  INITIAL_TOPIC_MANAGER_STATE,
  isDirty,
  topicManagerReducer,
  type TopicManagerState,
} from './topicManagerState'

function openedCreate(): TopicManagerState {
  return topicManagerReducer(INITIAL_TOPIC_MANAGER_STATE, { type: 'create-opened' })
}

describe('topicManagerReducer', () => {
  it('reduce_ShouldStartWithEmptyActiveForm_WhenCreateIsOpened', () => {
    // When
    const state = openedCreate()

    // Then
    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields).toEqual({ slug: '', name: '', description: '', active: true })
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldMarkFormDirty_WhenFieldChanges', () => {
    // When
    const state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })

    // Then
    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldClearOnlyTheSlugError_WhenSlugIsEdited', () => {
    // Given
    let state = openedCreate()
    state = topicManagerReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'slug', message: 'Slug này đã được Topic khác sử dụng.' },
    })

    // When
    const afterOtherField = topicManagerReducer(state, { type: 'fields-changed', changes: { name: 'Java' } })
    const afterSlug = topicManagerReducer(afterOtherField, { type: 'fields-changed', changes: { slug: 'java-2' } })

    // Then
    expect(afterOtherField.failure).not.toBeNull()
    expect(afterSlug.failure).toBeNull()
  })

  it('reduce_ShouldCloseFormAndShowNotice_WhenMutationSucceeds', () => {
    // Given
    let state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })
    state = topicManagerReducer(state, { type: 'mutation-started', pending: 'saving' })

    // When
    state = topicManagerReducer(state, { type: 'mutation-succeeded', notice: 'created' })

    // Then
    expect(state.panel).toEqual({ mode: 'closed' })
    expect(state.notice).toBe('created')
    expect(state.pending).toBeNull()
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepTypedValues_WhenMutationFails', () => {
    // Given
    let state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })
    state = topicManagerReducer(state, { type: 'mutation-started', pending: 'saving' })

    // When
    state = topicManagerReducer(state, { type: 'mutation-failed', failure: { kind: 'session-expired' } })

    // Then
    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields.name).toBe('Java')
    expect(state.pending).toBeNull()
    expect(state.failure).toEqual({ kind: 'session-expired' })
  })
})
