import { describe, expect, it } from 'vitest'

import { draftArticle } from '../fixture/articleAdminFixture'
import { fieldsOf, sameFields, STANDALONE, toEditable } from './article'

describe('article model', () => {
  it('fieldsOf_ShouldCopyTopicIds_WhenArticleHasTopics', () => {
    const article = draftArticle({ topicIds: ['topic-b', 'topic-a'] })

    const fields = fieldsOf(article)

    expect(fields.topicIds).toEqual(['topic-b', 'topic-a'])
    expect(fields.topicIds).not.toBe(article.topicIds)
  })

  it('toEditable_ShouldSendChosenTopicIds_WhenTopicsAreSelected', () => {
    const fields = { ...fieldsOf(draftArticle()), topicIds: ['topic-a'] }

    const editable = toEditable(fields, STANDALONE)

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
})
