import { describe, expect, it } from 'vitest'

import { canDeleteContent, etagOfRevision } from './contentDelete'

describe('canDeleteContent', () => {
  it('canDeleteContent_ShouldBeTrue_WhenStatusIsDraft', () => {
    expect(canDeleteContent('DRAFT')).toBe(true)
  })

  it('canDeleteContent_ShouldBeFalse_WhenItemWasPublishedOnce', () => {
    expect(canDeleteContent('PUBLISHED')).toBe(false)
    expect(canDeleteContent('UNPUBLISHED')).toBe(false)
  })
})

describe('etagOfRevision', () => {
  it('etagOfRevision_ShouldQuoteTheNumber_WhenRevisionIsGiven', () => {
    expect(etagOfRevision(7)).toBe('"7"')
  })
})
