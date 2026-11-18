import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toCommonEditorFailure, toEditorFailure } from './editorFailure'

function apiError(status: number, code: string): ApiError {
  return new ApiError(status, { code, requestId: 'req-1' }, null)
}

describe('toEditorFailure', () => {
  it('toEditorFailure_ShouldPointAtTopics_WhenTopicIsInactive', () => {
    expect(toEditorFailure(apiError(422, 'INACTIVE_TOPIC_ASSIGNMENT'))).toMatchObject({
      kind: 'field',
      field: 'topicIds',
    })
  })

  it('toEditorFailure_ShouldPointAtTopics_WhenTopicDoesNotExist', () => {
    expect(toEditorFailure(apiError(422, 'TOPIC_REFERENCE_NOT_FOUND'))).toMatchObject({
      kind: 'field',
      field: 'topicIds',
    })
  })

  it.each(['MODULE_REFERENCE_NOT_FOUND', 'INVALID_POSITION'])(
    'toEditorFailure_ShouldPointAtPlacement_When%s',
    (code) => {
      expect(toEditorFailure(apiError(422, code))).toMatchObject({ kind: 'field', field: 'placement' })
    },
  )
})

describe('toCommonEditorFailure', () => {
  it('toCommonEditorFailure_ShouldReportSessionExpired_WhenSessionIsMissing', () => {
    expect(toCommonEditorFailure(apiError(401, 'AUTHENTICATION_REQUIRED'))).toEqual({ kind: 'session-expired' })
  })

  it('toCommonEditorFailure_ShouldAskForNewToken_WhenCsrfTokenIsInvalid', () => {
    expect(toCommonEditorFailure(apiError(403, 'CSRF_INVALID'))).toEqual({ kind: 'session-renewed' })
  })

  it('toCommonEditorFailure_ShouldReportConflict_WhenRevisionIsStale', () => {
    expect(toCommonEditorFailure(apiError(412, 'REVISION_CONFLICT'))).toEqual({ kind: 'conflict' })
  })

  it('toCommonEditorFailure_ShouldReportTechnicalProblem_WhenNoResponseCame', () => {
    const failure = toCommonEditorFailure(new ApiError(0, null, null))

    expect(failure).toMatchObject({ kind: 'general' })
    expect(failure.kind === 'general' && failure.message).toContain('Không kết nối được máy chủ')
  })
})
