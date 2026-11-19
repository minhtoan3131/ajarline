import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toNewsEditorFailure } from './newsEditorFailure'

function apiError(status: number, code: string): ApiError {
  return new ApiError(status, { code, requestId: 'req-1' }, null)
}

describe('toNewsEditorFailure', () => {
  it('toNewsEditorFailure_ShouldPointAtSourceBlock_WhenSourceIsRequired', () => {
    expect(toNewsEditorFailure(apiError(422, 'NEWS_SOURCE_REQUIRED'))).toMatchObject({ kind: 'field', field: 'source' })
  })

  it('toNewsEditorFailure_ShouldPointAtBody_WhenBodyIsRequired', () => {
    expect(toNewsEditorFailure(apiError(422, 'NEWS_BODY_REQUIRED'))).toMatchObject({
      kind: 'field',
      field: 'bodyMarkdown',
    })
  })

  it('toNewsEditorFailure_ShouldPointAtSummary_WhenSummaryIsRequired', () => {
    expect(toNewsEditorFailure(apiError(422, 'CONTENT_SUMMARY_REQUIRED'))).toMatchObject({
      kind: 'field',
      field: 'summary',
    })
  })

  it.each(['SLUG_CONFLICT', 'SLUG_IMMUTABLE_AFTER_PUBLICATION'])('toNewsEditorFailure_ShouldPointAtSlug_When%s', (code) => {
    expect(toNewsEditorFailure(apiError(409, code))).toMatchObject({ kind: 'field', field: 'slug' })
  })

  it.each(['TOPIC_REFERENCE_NOT_FOUND', 'INACTIVE_TOPIC_ASSIGNMENT'])(
    'toNewsEditorFailure_ShouldPointAtTopics_When%s',
    (code) => {
      expect(toNewsEditorFailure(apiError(422, code))).toMatchObject({ kind: 'field', field: 'topicIds' })
    },
  )

  it('toNewsEditorFailure_ShouldShowServerDetail_WhenValidationFails', () => {
    const error = new ApiError(422, { code: 'VALIDATION_FAILED', detail: 'source.url is not valid.', requestId: 'r' }, null)

    expect(toNewsEditorFailure(error)).toMatchObject({
      kind: 'general',
      message: expect.stringContaining('source.url is not valid.'),
    })
  })

  it('toNewsEditorFailure_ShouldReturnGeneral_WhenCodeIsUnknown', () => {
    expect(toNewsEditorFailure(apiError(422, 'SOMETHING_NEW'))).toMatchObject({ kind: 'general' })
  })

  it('toNewsEditorFailure_ShouldReturnGeneral_WhenPublicationStateIsInvalid', () => {
    expect(toNewsEditorFailure(apiError(409, 'INVALID_PUBLICATION_STATE'))).toMatchObject({ kind: 'general' })
  })

  it('toNewsEditorFailure_ShouldReturnConflict_WhenRevisionIsStale', () => {
    expect(toNewsEditorFailure(apiError(412, 'REVISION_CONFLICT'))).toEqual({ kind: 'conflict' })
  })

  it('toNewsEditorFailure_ShouldReturnSessionExpired_WhenStatusIs401', () => {
    expect(toNewsEditorFailure(apiError(401, 'AUTHENTICATION_REQUIRED'))).toEqual({ kind: 'session-expired' })
  })
})
