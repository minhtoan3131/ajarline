import { describe, expect, it } from 'vitest'

import { draftNews } from '../fixture/newsAdminFixture'
import { createNewsEditorState, isDirty, newsEditorReducer } from './newsEditorState'

const LOADED = { value: draftNews(), etag: '"1"' }

describe('newsEditorReducer', () => {
  it('reduce_ShouldReplaceEtag_WhenNewsIsSaved', () => {
    let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
    state = newsEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'Spring-Boot' })
    state = newsEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = newsEditorReducer(state, {
      type: 'news-saved',
      news: { value: draftNews({ revision: 2, slug: 'spring-boot' }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.current?.etag).toBe('"2"')
    expect(state.fields.slug).toBe('spring-boot')
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepTypedText_WhenFieldChangesDuringSave', () => {
    let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
    state = newsEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })
    state = newsEditorReducer(state, { type: 'field-changed', field: 'title', value: 'Gõ trong lúc lưu' })

    state = newsEditorReducer(state, {
      type: 'news-saved',
      news: { value: draftNews({ revision: 2 }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.fields.title).toBe('Gõ trong lúc lưu')
    expect(isDirty(state)).toBe(true)
  })

  it('reduce_ShouldMarkFormDirty_WhenSourceChanges', () => {
    const state = createNewsEditorState({ initial: LOADED, initialNotice: null })

    const changed = newsEditorReducer(state, { type: 'field-changed', field: 'sourceUrl', value: 'https://other.example' })

    expect(isDirty(changed)).toBe(true)
  })

  it('reduce_ShouldMarkFormDirty_WhenTopicSelectionChanges', () => {
    const state = createNewsEditorState({ initial: LOADED, initialNotice: null })

    const changed = newsEditorReducer(state, { type: 'topics-changed', topicIds: ['topic-a'] })

    expect(isDirty(changed)).toBe(true)
  })

  it('reduce_ShouldClearOnlyThatFieldError_WhenFieldWithErrorIsEdited', () => {
    let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
    state = newsEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'slug', message: 'Slug này đã được một tin khác sử dụng.' },
    })

    const afterTitle = newsEditorReducer(state, { type: 'field-changed', field: 'title', value: 'x' })
    const afterSlug = newsEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'x' })

    expect(afterTitle.failure).not.toBeNull()
    expect(afterSlug.failure).toBeNull()
  })

  it.each(['sourceName', 'sourceUrl', 'sourcePublishedAt'] as const)(
    'reduce_ShouldClearSourceError_WhenSourceField%sIsEdited',
    (field) => {
      let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
      state = newsEditorReducer(state, {
        type: 'mutation-failed',
        failure: { kind: 'field', field: 'source', message: 'Chưa đủ nguồn để xuất bản.' },
      })

      const edited = newsEditorReducer(state, { type: 'field-changed', field, value: 'x' })

      expect(edited.failure).toBeNull()
    },
  )

  it('reduce_ShouldKeepSourceError_WhenTitleIsEdited', () => {
    let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
    state = newsEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'source', message: 'Chưa đủ nguồn để xuất bản.' },
    })

    const edited = newsEditorReducer(state, { type: 'field-changed', field: 'title', value: 'x' })

    expect(edited.failure).not.toBeNull()
  })

  it('reduce_ShouldDropLocalChanges_WhenNewsIsReloaded', () => {
    let state = createNewsEditorState({ initial: LOADED, initialNotice: null })
    state = newsEditorReducer(state, { type: 'field-changed', field: 'title', value: 'Bản của tôi' })

    state = newsEditorReducer(state, {
      type: 'news-reloaded',
      news: { value: draftNews({ revision: 2, title: 'Bản từ nơi khác' }), etag: '"2"' },
    })

    expect(state.fields.title).toBe('Bản từ nơi khác')
    expect(state.current?.etag).toBe('"2"')
    expect(isDirty(state)).toBe(false)
  })
})
