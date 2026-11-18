import { describe, expect, it } from 'vitest'

import { draftSeries, moduleChild, publicationOfSeries } from '../fixture/learningAdminFixture'
import { createLearningEditorState, isDirty, learningEditorReducer } from './learningEditorState'

const LOADED = { value: draftSeries({ modules: [moduleChild()] }), etag: '"1"' }

describe('learningEditorReducer', () => {
  it('reduce_ShouldReplaceEtag_WhenSeriesIsSaved', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = learningEditorReducer(state, {
      type: 'saved',
      series: { value: draftSeries({ revision: 2 }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.current?.etag).toBe('"2"')
    expect(state.current?.value.revision).toBe(2)
    expect(state.pending).toBeNull()
  })

  it('reduce_ShouldReplaceEtagAndKeepContent_WhenPublicationChanges', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })

    state = learningEditorReducer(state, {
      type: 'publication-changed',
      publication: {
        value: publicationOfSeries(LOADED.value, { status: 'PUBLISHED', revision: 2, publishedAt: '2026-11-18T03:00:00Z' }),
        etag: '"2"',
      },
      notice: 'published',
    })

    expect(state.current?.etag).toBe('"2"')
    expect(state.current?.value).toMatchObject({ status: 'PUBLISHED', revision: 2, title: 'Học Spring Boot' })
    expect(state.current?.value.modules).toHaveLength(1)
  })

  it('reduce_ShouldUseCanonicalFields_WhenNothingChangedDuringSave', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'Hoc-Spring-Boot' })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = learningEditorReducer(state, {
      type: 'saved',
      series: { value: draftSeries({ revision: 2, slug: 'hoc-spring-boot' }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.fields.slug).toBe('hoc-spring-boot')
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepTypedText_WhenFieldChangesDuringSave', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })
    state = learningEditorReducer(state, { type: 'field-changed', field: 'title', value: 'Gõ trong lúc lưu' })

    state = learningEditorReducer(state, {
      type: 'saved',
      series: { value: draftSeries({ revision: 2 }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.fields.title).toBe('Gõ trong lúc lưu')
    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldClearOnlyThatFieldError_WhenFieldWithErrorIsEdited', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'slug', message: 'Slug này đã được sử dụng.' },
    })

    const afterTitle = learningEditorReducer(state, { type: 'field-changed', field: 'title', value: 'x' })
    const afterSlug = learningEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'x' })

    expect(afterTitle.failure).not.toBeNull()
    expect(afterSlug.failure).toBeNull()
  })

  it('reduce_ShouldMarkFormDirty_WhenTopicSelectionChanges', () => {
    const state = createLearningEditorState({ initial: LOADED, initialNotice: null })

    const changed = learningEditorReducer(state, { type: 'topics-changed', topicIds: ['topic-a'] })

    expect(isDirty(changed)).toBe(true)
  })

  it('reduce_ShouldDropUnsavedText_WhenSeriesIsReloaded', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'field-changed', field: 'title', value: 'Gõ dở' })

    state = learningEditorReducer(state, {
      type: 'reloaded',
      series: { value: draftSeries({ revision: 5, title: 'Bản từ máy chủ' }), etag: '"5"' },
    })

    expect(state.fields.title).toBe('Bản từ máy chủ')
    expect(state.current?.etag).toBe('"5"')
    expect(isDirty(state)).toBe(false)
  })
})
