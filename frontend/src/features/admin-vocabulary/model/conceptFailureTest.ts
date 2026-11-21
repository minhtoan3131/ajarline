import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toConceptFailure } from './conceptFailure'

function apiError(status: number, code: string | null = null): ApiError {
  return new ApiError(status, code === null ? null : { code, requestId: 'req-1' }, null)
}

describe('toConceptFailure', () => {
  it('toConceptFailure_ShouldPointAtKey_WhenKeyConflicts', () => {
    expect(toConceptFailure(apiError(409, 'CONCEPT_KEY_CONFLICT'))).toEqual({
      kind: 'field',
      field: 'key',
      message: 'Key này đã được Concept khác sử dụng.',
    })
  })

  it('toConceptFailure_ShouldReportDuplicateExpression_WhenExpressionsRepeatAfterNormalizing', () => {
    expect(toConceptFailure(apiError(422, 'DUPLICATE_CONCEPT_EXPRESSION'))).toEqual({ kind: 'duplicate-expression' })
  })

  it('toConceptFailure_ShouldStateEveryLimitWithReference_WhenValidationFails', () => {
    const failure = toConceptFailure(apiError(422, 'VALIDATION_FAILED'))

    expect(failure.kind).toBe('general')
    expect(failure).toMatchObject({ message: expect.stringContaining('tối đa 200 ký tự') })
    expect(failure).toMatchObject({ message: expect.stringContaining('tối đa 50 cái') })
    expect(failure).toMatchObject({ message: expect.stringContaining('Mã tra cứu: req-1.') })
  })

  it('toConceptFailure_ShouldReportExpiredSession_WhenAuthenticationIsMissing', () => {
    expect(toConceptFailure(apiError(401, 'AUTHENTICATION_REQUIRED'))).toEqual({ kind: 'session-expired' })
  })

  it('toConceptFailure_ShouldReportConflictMessage_WhenConflictHasAnotherCode', () => {
    expect(toConceptFailure(apiError(409, 'SOMETHING_ELSE'))).toMatchObject({
      kind: 'general',
      message: expect.stringContaining('xung đột dữ liệu'),
    })
  })

  it('toConceptFailure_ShouldReportTechnicalFailure_WhenNoResponseArrives', () => {
    expect(toConceptFailure(apiError(0))).toMatchObject({
      kind: 'general',
      message: expect.stringContaining('Không kết nối được máy chủ'),
    })
  })

  it('toConceptFailure_ShouldReportTechnicalFailure_WhenErrorIsNotAnApiError', () => {
    expect(toConceptFailure(new Error('boom'))).toMatchObject({
      kind: 'general',
      message: expect.stringContaining('Không kết nối được máy chủ'),
    })
  })
})
