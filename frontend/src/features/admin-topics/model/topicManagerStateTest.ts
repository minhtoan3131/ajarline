import { describe, expect, it } from 'vitest'

import { activeTopic } from '../fixture/topicAdminFixture'
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
    const state = openedCreate()

    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields).toEqual({ slug: '', name: '', description: '', active: true })
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldMarkFormDirty_WhenFieldChanges', () => {
    const state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })

    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldClearOnlyTheSlugError_WhenSlugIsEdited', () => {
    let state = openedCreate()
    state = topicManagerReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'slug', message: 'Slug này đã được Topic khác sử dụng.' },
    })

    const afterOtherField = topicManagerReducer(state, { type: 'fields-changed', changes: { name: 'Java' } })
    const afterSlug = topicManagerReducer(afterOtherField, { type: 'fields-changed', changes: { slug: 'java-2' } })

    expect(afterOtherField.failure).not.toBeNull()
    expect(afterSlug.failure).toBeNull()
  })

  it('reduce_ShouldCloseFormAndShowNotice_WhenMutationSucceeds', () => {
    let state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })
    state = topicManagerReducer(state, { type: 'mutation-started', pending: 'saving' })

    state = topicManagerReducer(state, { type: 'mutation-succeeded', notice: 'created' })

    expect(state.panel).toEqual({ mode: 'closed' })
    expect(state.notice).toBe('created')
    expect(state.pending).toBeNull()
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepTypedValues_WhenMutationFails', () => {
    let state = topicManagerReducer(openedCreate(), { type: 'fields-changed', changes: { name: 'Java' } })
    state = topicManagerReducer(state, { type: 'mutation-started', pending: 'saving' })

    state = topicManagerReducer(state, { type: 'mutation-failed', failure: { kind: 'session-expired' } })

    expect(state.panel).toEqual({ mode: 'create' })
    expect(state.fields.name).toBe('Java')
    expect(state.pending).toBeNull()
    expect(state.failure).toEqual({ kind: 'session-expired' })
  })

  it('reduce_ShouldFillFormFromTopic_WhenEditIsOpened', () => {
    const topic = activeTopic()

    const state = topicManagerReducer(INITIAL_TOPIC_MANAGER_STATE, { type: 'edit-opened', topic })

    expect(state.panel).toEqual({ mode: 'edit', topic })
    expect(state.fields).toEqual({ slug: 'java', name: 'Java', description: 'Ngôn ngữ và nền tảng Java.', active: true })
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldCloseFormAndKeepFailure_WhenTopicIsNotFound', () => {
    let state = topicManagerReducer(INITIAL_TOPIC_MANAGER_STATE, { type: 'edit-opened', topic: activeTopic() })
    state = topicManagerReducer(state, { type: 'mutation-started', pending: 'saving' })

    state = topicManagerReducer(state, { type: 'mutation-failed', failure: { kind: 'not-found' } })

    expect(state.panel).toEqual({ mode: 'closed' })
    expect(state.failure).toEqual({ kind: 'not-found' })
    expect(state.pending).toBeNull()
  })
})
