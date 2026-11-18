import type { PublicationState } from '../../admin-content/model/article'
import { givenSignedInAdmin } from '../../admin-auth/fixture/adminSessionFixture'
import { givenTopicCatalog } from '../../admin-topics/fixture/topicAdminFixture'
import { jsonResponse, type StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { AdminContentItem, LearningChild } from '../model/learning'
import type { SeriesAdminDetail } from '../model/series'

export const SERIES_ID = '5b1d7e3a-9c24-4f08-8a6e-2d4c0b9f1e73'
export const SERIES_API_PATH = '/api/admin/v1/series'
export const SERIES_PATH = `${SERIES_API_PATH}/${SERIES_ID}`
export const MODULE_ID = 'c8a2f6d1-3e57-4b90-a1d4-7f0e5b2c9a36'
export const OTHER_MODULE_ID = 'e4b9d0a7-6c13-4258-9f8e-1a3d5c7b0e24'

export function draftSeries(changes: Partial<SeriesAdminDetail> = {}): SeriesAdminDetail {
  return {
    id: SERIES_ID,
    type: 'series',
    title: 'Học Spring Boot',
    slug: 'hoc-spring-boot',
    summary: 'Lộ trình học Spring Boot từ đầu.',
    overviewMarkdown: '## Giới thiệu\n\nBắt đầu từ đâu.',
    topicIds: [],
    status: 'DRAFT',
    revision: 1,
    publishedAt: null,
    unpublishedAt: null,
    createdAt: '2026-11-18T02:00:00Z',
    updatedAt: '2026-11-18T02:00:00Z',
    modules: [],
    ...changes,
  }
}

export function moduleChild(changes: Partial<LearningChild> = {}): LearningChild {
  return { id: MODULE_ID, title: 'Nền tảng', slug: 'nen-tang', status: 'DRAFT', position: 1, ...changes }
}

/** Response headers the server sends with an Admin Series. */
export function seriesHeaders(series: SeriesAdminDetail): Record<string, string> {
  return { ETag: `"${series.revision}"` }
}

export function seriesPublishPath(action: 'publish' | 'unpublish'): string {
  return `/api/admin/v1/content/${SERIES_ID}/${action}`
}

/** Publication response for a Series after a publish or unpublish. */
export function publicationOfSeries(series: SeriesAdminDetail, changes: Partial<PublicationState>): PublicationState {
  return {
    id: series.id,
    type: 'series',
    status: series.status,
    revision: series.revision,
    publishedAt: series.publishedAt,
    unpublishedAt: series.unpublishedAt,
    updatedAt: series.updatedAt,
    ...changes,
  }
}

/** Request path of the Series list at one page. The page size is the one the app sends. */
export function seriesListApiPath(page = 0): string {
  return `/api/admin/v1/content?types=series&page=${page}&size=20`
}

export function seriesItem(changes: Partial<AdminContentItem> = {}): AdminContentItem {
  return { id: SERIES_ID, slug: 'hoc-spring-boot', title: 'Học Spring Boot', status: 'DRAFT', ...changes }
}

/** Body of the list endpoint. */
export function seriesListResponse(items: AdminContentItem[], options: { page?: number; hasMore?: boolean } = {}) {
  return { page: options.page ?? 0, size: 20, hasMore: options.hasMore ?? false, items }
}

/** Session plus the Topic list that every Series editor loads for its Topic picker. */
export function givenSeriesEditorSession(api: StubApiServer): StubApiServer {
  return givenTopicCatalog(givenSignedInAdmin(api))
}

/** Stubs the Series list at one page. */
export function givenSeriesList(api: StubApiServer, items: AdminContentItem[], page = 0, hasMore = false): StubApiServer {
  return api.on('GET', seriesListApiPath(page), jsonResponse(200, seriesListResponse(items, { page, hasMore })))
}
