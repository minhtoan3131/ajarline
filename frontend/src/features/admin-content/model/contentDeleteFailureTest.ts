import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import {
  allowsRetry,
  describeDeleteFailure,
  needsReload,
  type DeleteFailure,
  type DeleteFailureKind,
} from './contentDeleteFailure'

function apiError(status: number, code: string | null = null): ApiError {
  return new ApiError(status, code === null ? null : { code }, null)
}

function failureOf(kind: DeleteFailureKind): DeleteFailure {
  return { kind, message: 'message' }
}

describe('describeDeleteFailure', () => {
  it('describeDeleteFailure_ShouldSayGone_WhenItemIsNotFound', () => {
    expect(describeDeleteFailure(apiError(404, 'CONTENT_NOT_FOUND'), 'news').kind).toBe('gone')
  })

  it('describeDeleteFailure_ShouldPointToUnpublish_WhenItemWasEverPublished', () => {
    const failure = describeDeleteFailure(apiError(409, 'CONTENT_ALREADY_EVER_PUBLISHED'), 'article')

    expect(failure.kind).toBe('ever-published')
    expect(failure.message).toContain('gỡ xuất bản')
  })

  it('describeDeleteFailure_ShouldNameModules_WhenSeriesIsInUse', () => {
    const failure = describeDeleteFailure(apiError(409, 'CONTENT_IN_USE'), 'series')

    expect(failure.kind).toBe('in-use')
    expect(failure.message).toContain('còn Module')
  })

  it('describeDeleteFailure_ShouldNameArticles_WhenModuleIsInUse', () => {
    expect(describeDeleteFailure(apiError(409, 'CONTENT_IN_USE'), 'module').message).toContain('còn bài viết')
  })

  it('describeDeleteFailure_ShouldSayConflict_WhenRevisionIsStale', () => {
    expect(describeDeleteFailure(apiError(412, 'REVISION_CONFLICT'), 'news').kind).toBe('conflict')
  })

  it('describeDeleteFailure_ShouldSayExpired_WhenSessionIsMissing', () => {
    expect(describeDeleteFailure(apiError(401, 'AUTHENTICATION_REQUIRED'), 'news').kind).toBe('session-expired')
  })

  it('describeDeleteFailure_ShouldSayRenewed_WhenCsrfTokenIsInvalid', () => {
    expect(describeDeleteFailure(apiError(403, 'CSRF_INVALID'), 'news').kind).toBe('session-renewed')
  })

  it('describeDeleteFailure_ShouldBeGeneral_WhenServerSaysSomethingUnknown', () => {
    expect(describeDeleteFailure(apiError(409, 'SOMETHING_NEW'), 'news').kind).toBe('general')
    expect(describeDeleteFailure(apiError(422, 'INVALID_REQUEST'), 'news').kind).toBe('general')
    expect(describeDeleteFailure(apiError(403, 'FORBIDDEN'), 'news').kind).toBe('general')
    expect(describeDeleteFailure(apiError(500, 'INTERNAL_ERROR'), 'news').kind).toBe('general')
  })

  it('describeDeleteFailure_ShouldBeGeneral_WhenNoResponseArrives', () => {
    expect(describeDeleteFailure(new ApiError(0, null, null), 'news').kind).toBe('general')
    expect(describeDeleteFailure(new Error('boom'), 'news').kind).toBe('general')
  })
})

describe('allowsRetry', () => {
  it('allowsRetry_ShouldBeFalse_WhenItemWasPublishedOnceOrIsGone', () => {
    expect(allowsRetry(failureOf('ever-published'))).toBe(false)
    expect(allowsRetry(failureOf('gone'))).toBe(false)
  })

  it('allowsRetry_ShouldBeTrue_WhenTheAdminCanFixTheCause', () => {
    expect(allowsRetry(failureOf('in-use'))).toBe(true)
    expect(allowsRetry(failureOf('conflict'))).toBe(true)
    expect(allowsRetry(failureOf('general'))).toBe(true)
  })
})

describe('needsReload', () => {
  it('needsReload_ShouldBeTrue_WhenTheListOnScreenIsOutOfDate', () => {
    expect(needsReload(failureOf('conflict'))).toBe(true)
    expect(needsReload(failureOf('gone'))).toBe(true)
    expect(needsReload(failureOf('ever-published'))).toBe(true)
  })

  it('needsReload_ShouldBeFalse_WhenTheListIsStillRight', () => {
    expect(needsReload(failureOf('in-use'))).toBe(false)
    expect(needsReload(failureOf('general'))).toBe(false)
  })
})
