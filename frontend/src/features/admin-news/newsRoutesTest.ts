import { describe, expect, it } from 'vitest'

import { newsEditorPath, newsListPath } from './newsRoutes'

describe('newsRoutes', () => {
  it('newsListPath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(newsListPath()).toBe('/admin/news')
    expect(newsListPath(0)).toBe('/admin/news')
  })

  it('newsListPath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(newsListPath(2)).toBe('/admin/news?page=2')
  })

  it('newsEditorPath_ShouldPutIdInPath_WhenIdIsGiven', () => {
    expect(newsEditorPath('abc')).toBe('/admin/news/abc')
  })
})
