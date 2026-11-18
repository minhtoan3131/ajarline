import { describe, expect, it } from 'vitest'

import { draftModule } from '../fixture/learningAdminFixture'
import { EMPTY_LEARNING_FIELDS } from './learning'
import { blankModuleFields, moduleFieldsOf, ParentNotReadyError, toModuleEditable } from './module'

describe('module model', () => {
  it('moduleFieldsOf_ShouldHoldParentAsText_WhenModuleIsSaved', () => {
    const fields = moduleFieldsOf(draftModule({ position: 3 }))

    expect(fields.parent).toEqual({ seriesId: draftModule().seriesId, position: '3' })
  })

  it('blankModuleFields_ShouldHaveEmptyParent_WhenNoSeriesIsChosen', () => {
    expect(blankModuleFields(null).parent).toEqual({ seriesId: '', position: '' })
  })

  it('toModuleEditable_ShouldSendNumberAndNullForEmptyText_WhenParentIsReady', () => {
    const fields = { ...EMPTY_LEARNING_FIELDS, title: 'Nền tảng', slug: 'nen-tang', parent: { seriesId: 's-1', position: '2' } }

    expect(toModuleEditable(fields)).toEqual({
      title: 'Nền tảng',
      slug: 'nen-tang',
      summary: null,
      overviewMarkdown: null,
      topicIds: [],
      seriesId: 's-1',
      position: 2,
    })
  })

  it.each([
    ['no Series', { seriesId: '', position: '1' }],
    ['empty position', { seriesId: 's-1', position: '' }],
    ['zero position', { seriesId: 's-1', position: '0' }],
    ['text position', { seriesId: 's-1', position: 'abc' }],
    ['fraction position', { seriesId: 's-1', position: '1.5' }],
  ])('toModuleEditable_ShouldThrow_When%s', (_name, parent) => {
    expect(() => toModuleEditable({ ...EMPTY_LEARNING_FIELDS, parent })).toThrow(ParentNotReadyError)
  })

  it('toModuleEditable_ShouldThrow_WhenFormHasNoParent', () => {
    expect(() => toModuleEditable(EMPTY_LEARNING_FIELDS)).toThrow(ParentNotReadyError)
  })
})
