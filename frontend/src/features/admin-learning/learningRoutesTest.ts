import { describe, expect, it } from 'vitest'

import { moduleEditorPath, newModulePath, seriesEditorPath, seriesListPath } from './learningRoutes'

describe('seriesListPath', () => {
  it('seriesListPath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(seriesListPath()).toBe('/admin/series')
    expect(seriesListPath(0)).toBe('/admin/series')
  })

  it('seriesListPath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(seriesListPath(2)).toBe('/admin/series?page=2')
  })
})

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
