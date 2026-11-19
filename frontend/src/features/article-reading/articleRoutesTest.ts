import { describe, expect, it } from 'vitest'

import { articlePath } from './articleRoutes'

describe('articlePath', () => {
  it('articlePath_ShouldBuildArticleRoute_WhenSlugIsPlain', () => {
    expect(articlePath('hieu-protobuf')).toBe('/articles/hieu-protobuf')
  })

  it('articlePath_ShouldEncodeSlug_WhenSlugHasReservedCharacters', () => {
    expect(articlePath('a/b c')).toBe('/articles/a%2Fb%20c')
  })
})
