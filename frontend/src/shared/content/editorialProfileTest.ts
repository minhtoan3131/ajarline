import { describe, expect, it } from 'vitest'

import { EDITORIAL_PROFILE } from './editorialProfile'

describe('EDITORIAL_PROFILE', () => {
  it.each(['displayName', 'responsibilityLabel', 'shortBio'] as const)(
    'field_ShouldHaveText_WhenProfileIsRead (%s)',
    (field) => {
      expect(EDITORIAL_PROFILE[field].trim()).not.toBe('')
    },
  )
})
