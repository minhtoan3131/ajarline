import { describe, expect, it } from 'vitest'

import { articleChild, draftModule, draftSeries, moduleChild, OTHER_MODULE_ID, publicationOfSeries } from '../fixture/learningAdminFixture'
import { childrenOf } from './learningDetail'
import { blankModuleFields, moduleFieldsOf } from './module'
import { createLearningEditorState, isDirty, learningEditorReducer } from './learningEditorState'

const LOADED = { value: draftSeries({ modules: [moduleChild()] }), etag: '"1"' }

describe('learningEditorReducer', () => {
  it('reduce_ShouldReplaceEtag_WhenSeriesIsSaved', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = learningEditorReducer(state, {
      type: 'saved',
      detail: { value: draftSeries({ revision: 2 }), etag: '"2"' },
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
    expect(state.current && childrenOf(state.current.value)).toHaveLength(1)
  })

  it('reduce_ShouldUseCanonicalFields_WhenNothingChangedDuringSave', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'field-changed', field: 'slug', value: 'Hoc-Spring-Boot' })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = learningEditorReducer(state, {
      type: 'saved',
      detail: { value: draftSeries({ revision: 2, slug: 'hoc-spring-boot' }), etag: '"2"' },
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
      detail: { value: draftSeries({ revision: 2 }), etag: '"2"' },
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
      detail: { value: draftSeries({ revision: 5, title: 'Bản từ máy chủ' }), etag: '"5"' },
    })

    expect(state.fields.title).toBe('Bản từ máy chủ')
    expect(state.current?.etag).toBe('"5"')
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldReplaceEtagAndSortChildren_WhenOrderIsSaved', () => {
    const two = { value: draftSeries({ modules: [moduleChild(), moduleChild({ id: OTHER_MODULE_ID, title: 'Nâng cao', position: 2 })] }), etag: '"1"' }
    let state = createLearningEditorState({ initial: two, initialNotice: null })
    state = learningEditorReducer(state, { type: 'order-changed', order: [...two.value.modules].reverse() })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'ordering', submitted: null })

    state = learningEditorReducer(state, {
      type: 'order-saved',
      order: {
        value: {
          revision: 2,
          children: [
            { id: OTHER_MODULE_ID, position: 1, revision: 5 },
            { id: moduleChild().id, position: 2, revision: 4 },
          ],
        },
        etag: '"2"',
      },
    })

    expect(state.current?.etag).toBe('"2"')
    expect(state.current?.value.revision).toBe(2)
    expect(state.current?.value).toMatchObject({ modules: [{ id: OTHER_MODULE_ID, position: 1 }, { position: 2 }] })
    expect(state.order).toBeNull()
    expect(state.pending).toBeNull()
    expect(state.notice).toBe('order-saved')
  })

  it('reduce_ShouldKeepArrangedOrder_WhenOrderSaveFails', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'order-changed', order: [moduleChild({ title: 'Đã sắp' })] })

    state = learningEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'order', message: 'Danh sách đã thay đổi.' },
    })

    expect(state.order).toHaveLength(1)
    expect(state.failure?.kind).toBe('order')
  })

  it('reduce_ShouldDropArrangedOrder_WhenFormIsSaved', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'order-changed', order: [moduleChild()] })
    state = learningEditorReducer(state, { type: 'mutation-started', pending: 'saving', submitted: state.fields })

    state = learningEditorReducer(state, {
      type: 'saved',
      detail: { value: draftSeries({ revision: 2 }), etag: '"2"' },
      notice: 'saved',
    })

    expect(state.order).toBeNull()
    expect(state.current?.etag).toBe('"2"')
  })

  it('reduce_ShouldDropArrangedOrderAndError_WhenRecordIsReloaded', () => {
    let state = createLearningEditorState({ initial: LOADED, initialNotice: null })
    state = learningEditorReducer(state, { type: 'order-changed', order: [moduleChild()] })
    state = learningEditorReducer(state, { type: 'mutation-failed', failure: { kind: 'order', message: 'x' } })

    state = learningEditorReducer(state, { type: 'reloaded', detail: LOADED })

    expect(state.order).toBeNull()
    expect(state.failure).toBeNull()
  })

  it('reduce_ShouldClearOnlyParentError_WhenParentIsChanged', () => {
    let state = createLearningEditorState({ initial: { value: draftModule(), etag: '"1"' }, initialNotice: null })
    state = learningEditorReducer(state, {
      type: 'mutation-failed',
      failure: { kind: 'field', field: 'parent', message: 'Vị trí không hợp lệ.' },
    })

    const changed = learningEditorReducer(state, { type: 'parent-changed', parent: { seriesId: 's-2', position: '' } })

    expect(changed.failure).toBeNull()
    expect(isDirty(changed)).toBe(true)
  })

  it('reduce_ShouldReadParentFromModule_WhenModuleIsLoaded', () => {
    const state = createLearningEditorState({ initial: { value: draftModule({ position: 2 }), etag: '"1"' }, initialNotice: null })

    expect(state.fields.parent).toEqual(moduleFieldsOf(draftModule({ position: 2 })).parent)
    expect(state.current?.value).toMatchObject({ type: 'module' })
  })

  it('createLearningEditorState_ShouldUseBlankForm_WhenNewModuleHasChosenSeries', () => {
    const blank = blankModuleFields({ seriesId: 's-1', position: '3' })

    const state = createLearningEditorState({ initial: null, initialNotice: null, blank })

    expect(state.fields.parent).toEqual({ seriesId: 's-1', position: '3' })
    expect(isDirty(state)).toBe(false)
  })

  it('reduce_ShouldKeepArticleChildren_WhenModulePublicationChanges', () => {
    const module = { value: draftModule({ articles: [articleChild()] }), etag: '"1"' }
    let state = createLearningEditorState({ initial: module, initialNotice: null })

    state = learningEditorReducer(state, {
      type: 'publication-changed',
      publication: {
        value: { id: module.value.id, type: 'module', status: 'PUBLISHED', revision: 2, publishedAt: '2026-11-18T03:00:00Z', unpublishedAt: null, updatedAt: '2026-11-18T03:00:00Z' },
        etag: '"2"',
      },
      notice: 'published',
    })

    expect(state.current?.etag).toBe('"2"')
    expect(state.current?.value).toMatchObject({ status: 'PUBLISHED', articles: [{ title: 'Bài mở đầu' }] })
  })
})
