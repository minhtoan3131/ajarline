import { describe, expect, it } from 'vitest'

import { activeTopic, inactiveTopic } from '../fixture/topicAdminFixture'
import { EMPTY_TOPIC_FIELDS, fieldsOf, toCreatable, toEditable } from './topic'

describe('toCreatable', () => {
  it('toCreatable_ShouldSendNullDescription_WhenDescriptionIsEmpty', () => {
    const fields = { ...EMPTY_TOPIC_FIELDS, slug: 'java', name: 'Java' }

    const creatable = toCreatable(fields)

    expect(creatable).toEqual({ slug: 'java', name: 'Java', description: null, active: true })
  })

  it('toCreatable_ShouldKeepTypedValuesUnchanged_WhenFieldsAreFilled', () => {
    const fields = { slug: ' Java ', name: ' Java ', description: 'Mô tả', active: false }

    const creatable = toCreatable(fields)

    expect(creatable).toEqual({ slug: ' Java ', name: ' Java ', description: 'Mô tả', active: false })
  })
})

describe('toEditable', () => {
  it('toEditable_ShouldOmitSlug_WhenFieldsAreConverted', () => {
    const fields = fieldsOf(activeTopic())

    const editable = toEditable(fields)

    expect(Object.keys(editable).sort()).toEqual(['active', 'description', 'name'])
  })

  it('toEditable_ShouldSendNullDescription_WhenDescriptionIsEmpty', () => {
    const fields = { ...fieldsOf(activeTopic()), description: '' }

    const editable = toEditable(fields)

    expect(editable.description).toBeNull()
  })
})

describe('fieldsOf', () => {
  it('fieldsOf_ShouldUseEmptyText_WhenDescriptionIsNull', () => {
    const fields = fieldsOf(inactiveTopic())

    expect(fields).toEqual({ slug: 'spring', name: 'Spring', description: '', active: false })
  })
})
