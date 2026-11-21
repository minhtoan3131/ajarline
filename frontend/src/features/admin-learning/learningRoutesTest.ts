import { describe, expect, it } from 'vitest'

import { moduleEditorPath, newModulePath, seriesEditorPath } from './learningRoutes'

describe('seriesEditorPath', () => {
  it('seriesEditorPath_ShouldPutIdInPath_WhenIdIsGiven', () => {
    expect(seriesEditorPath('abc')).toBe('/admin/series/abc')
  })
})

describe('newModulePath', () => {
  it('newModulePath_ShouldAddSeriesQuery_WhenSeriesIdIsGiven', () => {
    expect(newModulePath('abc')).toBe('/admin/modules/new?seriesId=abc')
  })

  it('newModulePath_ShouldOmitQuery_WhenNoSeriesIsGiven', () => {
    expect(newModulePath()).toBe('/admin/modules/new')
  })
})

describe('moduleEditorPath', () => {
  it('moduleEditorPath_ShouldPutIdInPath_WhenIdIsGiven', () => {
    expect(moduleEditorPath('abc')).toBe('/admin/modules/abc')
  })
})
