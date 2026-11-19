import { describe, expect, it } from 'vitest'

import { draftArticle } from '../fixture/articleAdminFixture'
import { fieldsOf, sameFields, toEditable } from './article'

describe('article model', () => {
  it('fieldsOf_ShouldCopyTopicIds_WhenArticleHasTopics', () => {
    const article = draftArticle({ topicIds: ['topic-b', 'topic-a'] })

    const fields = fieldsOf(article)

    expect(fields.topicIds).toEqual(['topic-b', 'topic-a'])
    expect(fields.topicIds).not.toBe(article.topicIds)
  })

  it('toEditable_ShouldSendChosenTopicIds_WhenTopicsAreSelected', () => {
    const fields = { ...fieldsOf(draftArticle()), topicIds: ['topic-a'] }

    const editable = toEditable(fields)

    expect(editable).toMatchObject({ moduleId: null, position: null, topicIds: ['topic-a'] })
  })

  it('sameFields_ShouldIgnoreTopicOrder_WhenSameTopicsAreSelected', () => {
    const left = { ...fieldsOf(draftArticle()), topicIds: ['topic-a', 'topic-b'] }
    const right = { ...left, topicIds: ['topic-b', 'topic-a'] }

    expect(sameFields(left, right)).toBe(true)
  })

  it('sameFields_ShouldDetectChange_WhenTopicSetDiffers', () => {
    const left = { ...fieldsOf(draftArticle()), topicIds: ['topic-a'] }
    const right = { ...left, topicIds: ['topic-a', 'topic-b'] }

    expect(sameFields(left, right)).toBe(false)
  })

  it('fieldsOf_ShouldReadPlacement_WhenArticleIsInModule', () => {
    const fields = fieldsOf(draftArticle({ moduleId: 'module-1', position: 3 }))

    expect(fields.placement).toEqual({ moduleId: 'module-1', position: 3 })
  })

  it('fieldsOf_ShouldHaveNoPlacement_WhenArticleIsStandalone', () => {
    expect(fieldsOf(draftArticle()).placement).toBeNull()
  })

  it('toEditable_ShouldSendSamePlacement_WhenFormIsUnchanged', () => {
    const article = draftArticle({ moduleId: 'module-1', position: 3 })

    const editable = toEditable(fieldsOf(article))

    expect(editable).toEqual({
      title: article.title,
      slug: article.slug,
      summary: article.summary,
      bodyMarkdown: article.bodyMarkdown,
      moduleId: 'module-1',
      position: 3,
      topicIds: [],
    })
  })

  it('sameFields_ShouldDetectChange_WhenPlacementDiffers', () => {
    const left = { ...fieldsOf(draftArticle()), placement: { moduleId: 'module-1', position: 1 } }

    expect(sameFields(left, { ...left, placement: { moduleId: 'module-1', position: 2 } })).toBe(false)
    expect(sameFields(left, { ...left, placement: { moduleId: 'module-2', position: 1 } })).toBe(false)
    expect(sameFields(left, { ...left, placement: null })).toBe(false)
  })

  it('sameFields_ShouldTreatSamePlacementAsSame_WhenObjectsAreDifferent', () => {
    const left = { ...fieldsOf(draftArticle()), placement: { moduleId: 'module-1', position: 1 } }

    expect(sameFields(left, { ...left, placement: { moduleId: 'module-1', position: 1 } })).toBe(true)
  })
})
