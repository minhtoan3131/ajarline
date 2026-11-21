import { describe, expect, it } from 'vitest'

import { activeConcept, inactiveConcept } from '../fixture/conceptAdminFixture'
import { EMPTY_CONCEPT_FIELDS, fieldsOf, sameConceptFields, toCreatable, toEditable, type ConceptFields } from './concept'

function threadFields(): ConceptFields {
  return {
    key: 'thread',
    displayName: 'Thread',
    description: 'Luồng thực thi.',
    active: true,
    expressions: [
      { rowKey: 'row-0', expression: 'đa luồng', languageHint: 'vi', autoExpand: true },
      { rowKey: 'row-1', expression: 'thread', languageHint: '', autoExpand: false },
    ],
  }
}

describe('toCreatable', () => {
  it('toCreatable_ShouldSendNullDescription_WhenDescriptionIsEmpty', () => {
    const fields = { ...EMPTY_CONCEPT_FIELDS, key: 'thread', displayName: 'Thread' }

    const creatable = toCreatable(fields)

    expect(creatable).toEqual({ key: 'thread', displayName: 'Thread', description: null, active: true, expressions: [] })
  })

  it('toCreatable_ShouldSendNullLanguageHint_WhenNoLanguageIsChosen', () => {
    const creatable = toCreatable(threadFields())

    expect(creatable.expressions).toEqual([
      { expression: 'đa luồng', languageHint: 'vi', autoExpand: true },
      { expression: 'thread', languageHint: null, autoExpand: false },
    ])
  })

  it('toCreatable_ShouldKeepTypedValuesUnchanged_WhenFieldsAreFilled', () => {
    const fields = { ...threadFields(), key: ' Thread ', displayName: ' Thread ', active: false }

    const creatable = toCreatable(fields)

    expect(creatable).toMatchObject({ key: ' Thread ', displayName: ' Thread ', active: false })
  })

  it('toCreatable_ShouldOmitRowKeys_WhenExpressionsAreConverted', () => {
    const creatable = toCreatable(threadFields())

    for (const expression of creatable.expressions) {
      expect(Object.keys(expression).sort()).toEqual(['autoExpand', 'expression', 'languageHint'])
    }
  })
})

describe('sameConceptFields', () => {
  it('sameConceptFields_ShouldBeTrue_WhenOnlyRowKeysDiffer', () => {
    const renumbered = {
      ...threadFields(),
      expressions: threadFields().expressions.map((row, index) => ({ ...row, rowKey: `row-${index + 10}` })),
    }

    expect(sameConceptFields(threadFields(), renumbered)).toBe(true)
  })

  it('sameConceptFields_ShouldBeFalse_WhenRowOrderDiffers', () => {
    const reversed = { ...threadFields(), expressions: [...threadFields().expressions].reverse() }

    expect(sameConceptFields(threadFields(), reversed)).toBe(false)
  })

  it('sameConceptFields_ShouldBeFalse_WhenOneAutoExpandDiffers', () => {
    const changed = threadFields()
    changed.expressions[1] = { ...changed.expressions[1], autoExpand: true }

    expect(sameConceptFields(threadFields(), changed)).toBe(false)
  })

  it('sameConceptFields_ShouldBeFalse_WhenRowCountDiffers', () => {
    const fewer = { ...threadFields(), expressions: threadFields().expressions.slice(0, 1) }

    expect(sameConceptFields(threadFields(), fewer)).toBe(false)
  })
})

describe('fieldsOf', () => {
  it('fieldsOf_ShouldNumberRowsInServerOrder_WhenConceptHasExpressions', () => {
    const fields = fieldsOf(activeConcept())

    expect(fields.expressions.map((row) => [row.rowKey, row.expression])).toEqual([
      ['row-0', 'multithreading'],
      ['row-1', 'thread'],
      ['row-2', 'đa luồng'],
    ])
    expect(fields.expressions[0]).toMatchObject({ languageHint: 'en', autoExpand: true })
  })

  it('fieldsOf_ShouldUseEmptyTextAndNoRows_WhenDescriptionIsNullAndThereIsNoExpression', () => {
    const fields = fieldsOf(inactiveConcept())

    expect(fields).toEqual({ key: 'spring-boot', displayName: 'Spring Boot', description: '', active: false, expressions: [] })
  })

  it('fieldsOf_ShouldUseEmptyLanguage_WhenLanguageHintIsNull', () => {
    const concept = activeConcept({ expressions: [{ expression: 'thread', languageHint: null, autoExpand: false }] })

    expect(fieldsOf(concept).expressions[0].languageHint).toBe('')
  })
})

describe('toEditable', () => {
  it('toEditable_ShouldOmitKey_WhenFieldsAreConverted', () => {
    const editable = toEditable(fieldsOf(activeConcept()))

    expect(Object.keys(editable).sort()).toEqual(['active', 'description', 'displayName', 'expressions'])
  })

  it('toEditable_ShouldOmitRowKeys_WhenExpressionsAreConverted', () => {
    const editable = toEditable(fieldsOf(activeConcept()))

    for (const expression of editable.expressions) {
      expect(Object.keys(expression).sort()).toEqual(['autoExpand', 'expression', 'languageHint'])
    }
  })

  it('toEditable_ShouldSendNullDescriptionAndNullLanguage_WhenTheyAreEmpty', () => {
    const fields = { ...fieldsOf(activeConcept()), description: '' }
    fields.expressions[0] = { ...fields.expressions[0], languageHint: '' }

    const editable = toEditable(fields)

    expect(editable.description).toBeNull()
    expect(editable.expressions[0].languageHint).toBeNull()
  })

  it('toEditable_ShouldKeepEveryExpressionInOrder_WhenConceptIsOpenedAndSavedUntouched', () => {
    const concept = activeConcept()

    const editable = toEditable(fieldsOf(concept))

    expect(editable.expressions).toEqual(concept.expressions)
  })
})
