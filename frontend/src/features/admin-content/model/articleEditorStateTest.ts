import { describe, expect, it } from 'vitest'

import { draftArticle } from '../fixture/articleAdminFixture'
import { articleEditorReducer, createEditorState, isDirty } from './articleEditorState'

const LOADED = { value: draftArticle(), etag: '"1"' }

describe('articleEditorReducer', () => {
  it('reduce_ShouldUseCanonicalFields_WhenNothingChangedDuringSave', () => {
    // Given
    let state = createEditorState({ initial: LOADED, initialNotice: null })
    state = articleEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'Hieu-Protobuf' })
    state = articleEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    // When
    state = articleEditorReducer(state, {
      type: 'article-saved',
      article: { value: draftArticle({ revision: 2, slug: 'hieu-protobuf' }), etag: '"2"' },
      notice: 'saved',
    })

    // Then
    expect(state.fields.slug).toBe('hieu-protobuf')
    expect(isDirty(state)).toBe(false)
    expect(state.current?.etag).toBe('"2"')
  })

  it('reduce_ShouldKeepTypedText_WhenFieldChangesDuringSave', () => {
    // Given
    let state = createEditorState({ initial: LOADED, initialNotice: null })
    state = articleEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })
    state = articleEditorReducer(state, { type: 'field-changed', field: 'title', value: 'Gõ trong lúc lưu' })

    // When
    state = articleEditorReducer(state, {
      type: 'article-saved',
      article: { value: draftArticle({ revision: 2 }), etag: '"2"' },
      notice: 'saved',
    })

    // Then
    expect(state.fields.title).toBe('Gõ trong lúc lưu')
    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldClearOnlyThatFieldError_WhenFieldWithErrorIsEdited', () => {
    // Given
    let state = createEditorState({ initial: LOADED, initialNotice: null })
    state = articleEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'slug', message: 'Slug này đã được bài khác sử dụng.' },
    })

    // When
    const afterTitle = articleEditorReducer(state, { type: 'field-changed', field: 'title', value: 'x' })
    const afterSlug = articleEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'x' })

    // Then
    expect(afterTitle.failure).not.toBeNull()
    expect(afterSlug.failure).toBeNull()
  })
})
