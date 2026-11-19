import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toTopicFailure } from './topicFailure'

function apiError(status: number, code: string | null = null): ApiError {
  return new ApiError(status, code === null ? null : { code, requestId: 'req-1' }, null)
}

describe('toTopicFailure', () => {
  it('toTopicFailure_ShouldPointAtSlug_WhenSlugConflicts', () => {
    expect(toTopicFailure(apiError(409, 'SLUG_CONFLICT'))).toEqual({
      kind: 'field',
      field: 'slug',
      message: 'Slug này đã được Topic khác sử dụng.',
    })
  })

  it('toTopicFailure_ShouldAskToRepeat_WhenCsrfProofIsInvalid', () => {
    expect(toTopicFailure(apiError(403, 'CSRF_INVALID'))).toEqual({ kind: 'session-renewed' })
  })

  it('toTopicFailure_ShouldReportExpiredSession_WhenAuthenticationIsMissing', () => {
    expect(toTopicFailure(apiError(401, 'AUTHENTICATION_REQUIRED'))).toEqual({ kind: 'session-expired' })
  })

  it('toTopicFailure_ShouldStateEveryLimitWithReference_WhenValidationFails', () => {
    const failure = toTopicFailure(apiError(422, 'VALIDATION_FAILED'))

    expect(failure.kind).toBe('general')
    expect(failure).toMatchObject({ message: expect.stringContaining('tối đa 200 ký tự') })
    expect(failure).toMatchObject({ message: expect.stringContaining('Mã tra cứu: req-1.') })
  })

  it('toTopicFailure_ShouldReportMissingTopic_WhenServerAnswersNotFound', () => {
    expect(toTopicFailure(apiError(404, 'TOPIC_NOT_FOUND'))).toEqual({ kind: 'not-found' })
  })

  it('toTopicFailure_ShouldReportTopicInUse_WhenDeleteIsRefused', () => {
    expect(toTopicFailure(apiError(409, 'TOPIC_IN_USE'))).toEqual({ kind: 'in-use' })
  })

  it('toTopicFailure_ShouldReportTechnicalFailure_WhenNoResponseArrives', () => {
    expect(toTopicFailure(apiError(0))).toMatchObject({
      kind: 'general',
      message: expect.stringContaining('Không kết nối được máy chủ'),
    })
  })
})
