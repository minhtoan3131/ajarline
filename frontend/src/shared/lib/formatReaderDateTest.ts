import { describe, expect, it } from 'vitest'

import { formatReaderDate } from './formatReaderDate'

describe('formatReaderDate', () => {
  it('formatReaderDate_ShouldShowSameVietnamDay_WhenInstantIsOneSecondBeforeVietnamMidnight', () => {
    expect(formatReaderDate('2026-10-05T16:59:59Z')).toMatch(/^5 tháng 10/)
  })

  it('formatReaderDate_ShouldShowNextVietnamDay_WhenInstantIsVietnamMidnight', () => {
    expect(formatReaderDate('2026-10-05T17:00:00Z')).toMatch(/^6 tháng 10/)
  })

  it('formatReaderDate_ShouldIncludeYear_WhenDateIsFormatted', () => {
    expect(formatReaderDate('2026-10-05T17:00:00Z')).toContain('2026')
  })
})
