import { describe, expect, it } from 'vitest'

import { GONE_MESSAGE } from './contentDeleteFailure'
import { indexNoticeFrom, indexNoticeState, isProblemNotice, noticeText } from './indexNotice'

describe('indexNoticeFrom', () => {
  it('indexNoticeFrom_ShouldGiveBackNotice_WhenStateCameFromIndexNoticeState', () => {
    expect(indexNoticeFrom(indexNoticeState({ kind: 'deleted', title: 'Tin A' }))).toEqual({ kind: 'deleted', title: 'Tin A' })
    expect(indexNoticeFrom(indexNoticeState({ kind: 'gone' }))).toEqual({ kind: 'gone' })
  })

  it('indexNoticeFrom_ShouldGiveNull_WhenStateIsNotANotice', () => {
    expect(indexNoticeFrom(undefined)).toBeNull()
    expect(indexNoticeFrom(null)).toBeNull()
    expect(indexNoticeFrom('deleted')).toBeNull()
    expect(indexNoticeFrom({})).toBeNull()
    expect(indexNoticeFrom({ indexNotice: 5 })).toBeNull()
    expect(indexNoticeFrom({ indexNotice: { kind: 'deleted' } })).toBeNull()
    expect(indexNoticeFrom({ indexNotice: { kind: 'deleted', title: 7 } })).toBeNull()
    expect(indexNoticeFrom({ indexNotice: { kind: 'failure', message: 'x' } })).toBeNull()
  })
})

describe('noticeText', () => {
  it('noticeText_ShouldNameTheItem_WhenItemWasDeleted', () => {
    expect(noticeText({ kind: 'deleted', title: 'Tin A' })).toBe('Đã xóa “Tin A”.')
  })

  it('noticeText_ShouldUseSameWordsAsGoneFailure_WhenItemIsGone', () => {
    expect(noticeText({ kind: 'gone' })).toBe(GONE_MESSAGE)
  })

  it('noticeText_ShouldGiveTheMessage_WhenNoticeIsAFailure', () => {
    expect(noticeText({ kind: 'failure', message: 'Lỗi' })).toBe('Lỗi')
  })
})

describe('isProblemNotice', () => {
  it('isProblemNotice_ShouldBeFalseOnlyForDeleted', () => {
    expect(isProblemNotice({ kind: 'deleted', title: 'A' })).toBe(false)
    expect(isProblemNotice({ kind: 'gone' })).toBe(true)
    expect(isProblemNotice({ kind: 'failure', message: 'x' })).toBe(true)
  })
})
