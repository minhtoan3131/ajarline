import { describe, expect, it } from 'vitest'

import { ApiError } from '../../../shared/api/httpClient'
import { toLearningEditorFailure } from './learningEditorFailure'
import { ParentNotReadyError } from './module'

function apiError(status: number, code: string): ApiError {
  return new ApiError(status, { code, requestId: 'req-1' }, null)
}

describe('toLearningEditorFailure', () => {
  it('toLearningEditorFailure_ShouldPointAtOverview_WhenOverviewIsRequired', () => {
    expect(toLearningEditorFailure(apiError(422, 'SERIES_OVERVIEW_REQUIRED'))).toMatchObject({
      kind: 'field',
      field: 'overviewMarkdown',
    })
  })

  it('toLearningEditorFailure_ShouldPointAtSummary_WhenSummaryIsRequired', () => {
    expect(toLearningEditorFailure(apiError(422, 'CONTENT_SUMMARY_REQUIRED'))).toMatchObject({
      kind: 'field',
      field: 'summary',
    })
  })

  it('toLearningEditorFailure_ShouldPointAtChildren_WhenSeriesHasNoPublicModule', () => {
    expect(toLearningEditorFailure(apiError(422, 'SERIES_HAS_NO_PUBLIC_MODULE'))).toMatchObject({ kind: 'children' })
  })

  it('toLearningEditorFailure_ShouldPointAtSlug_WhenSlugIsTaken', () => {
    expect(toLearningEditorFailure(apiError(409, 'SLUG_CONFLICT'))).toMatchObject({ kind: 'field', field: 'slug' })
  })

  it('toLearningEditorFailure_ShouldReportConflict_WhenRevisionIsStale', () => {
    expect(toLearningEditorFailure(apiError(412, 'REVISION_CONFLICT'))).toEqual({ kind: 'conflict' })
  })

  it('toLearningEditorFailure_ShouldPointAtTopics_WhenTopicDoesNotExist', () => {
    expect(toLearningEditorFailure(apiError(422, 'TOPIC_REFERENCE_NOT_FOUND'))).toMatchObject({
      kind: 'field',
      field: 'topicIds',
    })
  })

  it('toLearningEditorFailure_ShouldPointAtChildList_WhenOrderSetConflicts', () => {
    expect(toLearningEditorFailure(apiError(409, 'ORDER_SET_CONFLICT'))).toMatchObject({ kind: 'order' })
  })

  it('toLearningEditorFailure_ShouldPointAtChildList_WhenModuleHasNoPublicArticle', () => {
    expect(toLearningEditorFailure(apiError(422, 'MODULE_HAS_NO_PUBLIC_ARTICLE'))).toMatchObject({ kind: 'children' })
  })

  it('toLearningEditorFailure_ShouldPointAtOverview_WhenModuleOverviewIsRequired', () => {
    expect(toLearningEditorFailure(apiError(422, 'MODULE_OVERVIEW_REQUIRED'))).toMatchObject({
      kind: 'field',
      field: 'overviewMarkdown',
    })
  })

  it.each(['SERIES_REFERENCE_NOT_FOUND', 'INVALID_POSITION'])(
    'toLearningEditorFailure_ShouldPointAtParent_When%s',
    (code) => {
      expect(toLearningEditorFailure(apiError(422, code))).toMatchObject({ kind: 'field', field: 'parent' })
    },
  )

  it('toLearningEditorFailure_ShouldPointAtParent_WhenParentIsNotReady', () => {
    expect(toLearningEditorFailure(new ParentNotReadyError())).toMatchObject({ kind: 'field', field: 'parent' })
  })
})
