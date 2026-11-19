import { describe, expect, it } from 'vitest'

import { isStandaloneIndexQuery, standaloneIndexPath } from './articleRoutes'

describe('standaloneIndexPath', () => {
  it('standaloneIndexPath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(standaloneIndexPath()).toBe('/articles?standalone=true')
    expect(standaloneIndexPath(0)).toBe('/articles?standalone=true')
  })

  it('standaloneIndexPath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(standaloneIndexPath(2)).toBe('/articles?standalone=true&page=2')
  })
})

describe('isStandaloneIndexQuery', () => {
  it('isStandaloneIndexQuery_ShouldAccept_WhenStandaloneIsTrue', () => {
    expect(isStandaloneIndexQuery(new URLSearchParams('standalone=true&page=2'))).toBe(true)
  })

  it.each(['', 'standalone=false', 'standalone=abc', 'standalone=TRUE', 'standalone=', 'page=2'])(
    'isStandaloneIndexQuery_ShouldReject_WhenQueryIsMissingOrWrong (%j)',
    (query) => {
      expect(isStandaloneIndexQuery(new URLSearchParams(query))).toBe(false)
    },
  )
})
