import { describe, expect, it } from 'vitest'

import { modulePath, seriesIndexPath, seriesPath } from './learningRoutes'

describe('seriesIndexPath', () => {
  it('seriesIndexPath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(seriesIndexPath()).toBe('/series')
    expect(seriesIndexPath(0)).toBe('/series')
  })

  it('seriesIndexPath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(seriesIndexPath(2)).toBe('/series?page=2')
  })
})

describe('seriesPath', () => {
  it('seriesPath_ShouldBuildSeriesRoute_WhenSlugIsGiven', () => {
    expect(seriesPath('java-concurrency')).toBe('/series/java-concurrency')
  })

  it('seriesPath_ShouldEncodeSlug_WhenSlugHasSpecialCharacters', () => {
    expect(seriesPath('a b')).toBe('/series/a%20b')
  })
})

describe('modulePath', () => {
  it('modulePath_ShouldBuildModuleRoute_WhenSlugIsGiven', () => {
    expect(modulePath('thread-co-ban')).toBe('/modules/thread-co-ban')
  })
})
