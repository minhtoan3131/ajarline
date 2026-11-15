import { describe, expect, it } from 'vitest'

import { contentItem } from '../fixture/publicTopicFixture'
import type { TopicContentContext } from './publicTopic'
import { contextTrailOf, groupTopicContent, readerPathOf } from './topicContentGroups'

const SERIES = { id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23', slug: 'java-concurrency', title: 'Java Concurrency' }
const MODULE = {
  id: '5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d',
  slug: 'thread-co-ban',
  title: 'Thread cơ bản',
  position: 1,
}
const IN_MODULE: TopicContentContext = { series: SERIES, module: MODULE }

describe('groupTopicContent', () => {
  it('groupTopicContent_ShouldReturnOnlyStandaloneGroup_WhenEveryItemIsStandaloneArticle', () => {
    const items = [contentItem({ id: 'a1' }), contentItem({ id: 'a2' })]

    const groups = groupTopicContent(items)

    expect(groups.map((group) => [group.key, group.label])).toEqual([['standalone', 'Bài viết độc lập']])
    expect(groups[0].items).toEqual(items)
  })

  it('groupTopicContent_ShouldOrderGroupsAndKeepServerOrder_WhenPageMixesTypes', () => {
    const series = contentItem({ id: 's', type: 'series' })
    const moduleItem = contentItem({ id: 'm', type: 'module', context: { series: SERIES, module: null } })
    const inModule = contentItem({ id: 'a1', context: IN_MODULE })
    const standalone = contentItem({ id: 'a2' })
    const laterInModule = contentItem({ id: 'a3', context: IN_MODULE })
    const news = contentItem({ id: 'n', type: 'news' })

    const groups = groupTopicContent([series, moduleItem, inModule, standalone, laterInModule, news])

    expect(groups.map((group) => group.key)).toEqual(['series', 'in-series', 'standalone', 'news'])
    expect(groups[1].items).toEqual([moduleItem, inModule, laterInModule])
    expect(groups[2].items).toEqual([standalone])
  })

  it('groupTopicContent_ShouldReturnNoGroup_WhenPageIsEmpty', () => {
    expect(groupTopicContent([])).toEqual([])
  })
})

describe('readerPathOf', () => {
  it('readerPathOf_ShouldPointToArticlePage_WhenItemIsArticle', () => {
    expect(readerPathOf(contentItem())).toBe('/articles/hieu-protobuf')
  })

  it.each(['series', 'module', 'news'] as const)('readerPathOf_ShouldReturnNull_WhenTypeHasNoReaderPage (%s)', (type) => {
    expect(readerPathOf(contentItem({ type }))).toBeNull()
  })
})

describe('contextTrailOf', () => {
  it('contextTrailOf_ShouldJoinSeriesAndModule_WhenBothArePublic', () => {
    expect(contextTrailOf(contentItem({ context: IN_MODULE }))).toBe('Java Concurrency › Thread cơ bản')
  })

  it('contextTrailOf_ShouldShowModuleOnly_WhenSeriesIsHidden', () => {
    expect(contextTrailOf(contentItem({ context: { series: null, module: MODULE } }))).toBe('Thread cơ bản')
  })

  it('contextTrailOf_ShouldReturnNull_WhenItemIsStandalone', () => {
    expect(contextTrailOf(contentItem())).toBeNull()
  })
})
