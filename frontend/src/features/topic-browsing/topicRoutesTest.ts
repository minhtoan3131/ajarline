import { describe, expect, it } from 'vitest'

import { pageFromQuery, topicHomePath } from './topicRoutes'

describe('topicHomePath', () => {
  it('topicHomePath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(topicHomePath('java')).toBe('/topics/java')
    expect(topicHomePath('java', 0)).toBe('/topics/java')
  })

  it('topicHomePath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(topicHomePath('java', 2)).toBe('/topics/java?page=2')
  })
})

describe('pageFromQuery', () => {
  it.each<[string, number]>([
    ['0', 0],
    ['3', 3],
    ['999999999', 999999999],
  ])('pageFromQuery_ShouldReadPage_WhenValueIsCanonical (%s)', (value, page) => {
    expect(pageFromQuery(value)).toBe(page)
  })

  it.each<string | null>([null, '', '-1', '1.5', 'abc', '01', ' 2', '1e3', '1000000000'])(
    'pageFromQuery_ShouldOpenFirstPage_WhenValueIsMissingOrMalformed (%j)',
    (value) => {
      expect(pageFromQuery(value)).toBe(0)
    },
  )
})
