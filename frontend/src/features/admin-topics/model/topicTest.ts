import { describe, expect, it } from 'vitest'

import { EMPTY_TOPIC_FIELDS, toCreatable } from './topic'

describe('toCreatable', () => {
  it('toCreatable_ShouldSendNullDescription_WhenDescriptionIsEmpty', () => {
    // Given
    const fields = { ...EMPTY_TOPIC_FIELDS, slug: 'java', name: 'Java' }

    // When
    const creatable = toCreatable(fields)

    // Then
    expect(creatable).toEqual({ slug: 'java', name: 'Java', description: null, active: true })
  })

  it('toCreatable_ShouldKeepTypedValuesUnchanged_WhenFieldsAreFilled', () => {
    // Given
    const fields = { slug: ' Java ', name: ' Java ', description: 'Mô tả', active: false }

    // When
    const creatable = toCreatable(fields)

    // Then
    expect(creatable).toEqual({ slug: ' Java ', name: ' Java ', description: 'Mô tả', active: false })
  })
})
