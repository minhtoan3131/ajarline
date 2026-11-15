import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toEditorFailure } from './editorFailure'

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
})
