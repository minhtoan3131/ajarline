import { describe, expect, it } from 'vitest'

import { contentIndexPath, editorPathOf, newPathOf } from './contentRoutes'
import { NO_FILTER } from './model/contentIndexQuery'

describe('contentRoutes', () => {
  it('editorPathOf_ShouldGiveEditorRouteOfEveryType_WhenIdIsGiven', () => {
    expect(editorPathOf('article', 'abc')).toBe('/admin/articles/abc')
    expect(editorPathOf('module', 'abc')).toBe('/admin/modules/abc')
    expect(editorPathOf('series', 'abc')).toBe('/admin/series/abc')
    expect(editorPathOf('news', 'abc')).toBe('/admin/news/abc')
  })

  it('newPathOf_ShouldGiveNewRouteOfEveryType', () => {
    expect(newPathOf('article')).toBe('/admin/articles/new')
    expect(newPathOf('module')).toBe('/admin/modules/new')
    expect(newPathOf('series')).toBe('/admin/series/new')
    expect(newPathOf('news')).toBe('/admin/news/new')
  })

  it('contentIndexPath_ShouldAddSearch_WhenQueryHasFilters', () => {
    expect(contentIndexPath(NO_FILTER)).toBe('/admin/content')
    expect(contentIndexPath({ ...NO_FILTER, types: ['series'] })).toBe('/admin/content?types=series')
  })
})
