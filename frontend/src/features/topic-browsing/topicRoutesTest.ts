import { describe, expect, it } from 'vitest'

import { topicHomePath } from './topicRoutes'

describe('topicHomePath', () => {
  it('topicHomePath_ShouldOmitPage_WhenPageIsFirst', () => {
    expect(topicHomePath('java')).toBe('/topics/java')
    expect(topicHomePath('java', 0)).toBe('/topics/java')
  })

  it('topicHomePath_ShouldAddPageQuery_WhenPageIsLater', () => {
    expect(topicHomePath('java', 2)).toBe('/topics/java?page=2')
  })
})
