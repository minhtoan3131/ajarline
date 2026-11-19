import { describe, expect, it } from 'vitest'

import { newsIndexPath, newsPath } from './newsRoutes'

describe('newsIndexPath', () => {
  it('newsIndexPath_ShouldHaveNoPageQuery_WhenPageIsFirst', () => {
    expect(newsIndexPath()).toBe('/news')
    expect(newsIndexPath(0)).toBe('/news')
  })

  it('newsIndexPath_ShouldPutPageInQuery_WhenPageIsNotFirst', () => {
    expect(newsIndexPath(3)).toBe('/news?page=3')
  })
})

describe('newsPath', () => {
  it('newsPath_ShouldPointToNewsPage_WhenSlugIsPlain', () => {
    expect(newsPath('spring-boot-4-1')).toBe('/news/spring-boot-4-1')
  })

  it('newsPath_ShouldEncodeSlug_WhenSlugHasReservedCharacters', () => {
    expect(newsPath('a b/c')).toBe('/news/a%20b%2Fc')
  })
})
