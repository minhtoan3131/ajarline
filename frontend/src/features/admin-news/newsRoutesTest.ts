import { describe, expect, it } from 'vitest'

import { newsEditorPath } from './newsRoutes'

describe('newsRoutes', () => {
  it('newsEditorPath_ShouldPutIdInPath_WhenIdIsGiven', () => {
    expect(newsEditorPath('abc')).toBe('/admin/news/abc')
  })
})
