import type { PublicationState } from '../../admin-content/model/article'
import { givenSignedInAdmin } from '../../admin-auth/fixture/adminSessionFixture'
import { givenTopicCatalog } from '../../admin-topics/fixture/topicAdminFixture'
import { jsonResponse, type StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { AdminContentItem, ChildPosition, LearningChild } from '../model/learning'
import type { ModuleAdminDetail } from '../model/module'
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

export const ARTICLE_ONE_ID = '3f2c9a10-7d4e-4b18-9c65-0e1a2b3c4d5f'
export const ARTICLE_TWO_ID = '8a6d1f20-2b9c-4e37-b4a1-6c5d7e8f9a01'
export const MODULES_API_PATH = '/api/admin/v1/modules'
export const MODULE_PATH = `${MODULES_API_PATH}/${MODULE_ID}`
/** Where the order of the Modules of the Series is saved. */
export const SERIES_ORDER_PATH = `${SERIES_PATH}/module-order`
/** Where the order of the Articles of the Module is saved. */
export const MODULE_ORDER_PATH = `${MODULE_PATH}/article-order`

export function draftModule(changes: Partial<ModuleAdminDetail> = {}): ModuleAdminDetail {
  return {
    id: MODULE_ID,
    type: 'module',
    title: 'Nền tảng',
    slug: 'nen-tang',
    summary: 'Những khái niệm đầu tiên.',
    overviewMarkdown: '## Nền tảng\n\nBắt đầu từ đây.',
    topicIds: [],
    seriesId: SERIES_ID,
    position: 1,
    status: 'DRAFT',
    revision: 1,
    publishedAt: null,
    unpublishedAt: null,
    createdAt: '2026-11-18T02:00:00Z',
    updatedAt: '2026-11-18T02:00:00Z',
    articles: [],
    ...changes,
  }
}

export function articleChild(changes: Partial<LearningChild> = {}): LearningChild {
  return { id: ARTICLE_ONE_ID, title: 'Bài mở đầu', slug: 'bai-mo-dau', status: 'DRAFT', position: 1, ...changes }
}

/** Response headers the server sends with an Admin Module. */
export function moduleHeaders(module: ModuleAdminDetail): Record<string, string> {
  return { ETag: `"${module.revision}"` }
}

export function modulePublishPath(action: 'publish' | 'unpublish'): string {
  return `/api/admin/v1/content/${MODULE_ID}/${action}`
}

/** Body of the reorder response of a Series. */
export function seriesOrderResponse(revision: number, modules: ChildPosition[]) {
  return { seriesId: SERIES_ID, revision, modules }
}

/** Body of the reorder response of a Module. */
export function moduleOrderResponse(revision: number, articles: ChildPosition[]) {
  return { moduleId: MODULE_ID, revision, articles }
}

/** Session, the Topic list and the Series list that every Module editor loads. */
export function givenModuleEditorSession(api: StubApiServer, series: AdminContentItem[] = [seriesItem()]): StubApiServer {
  return givenSeriesList(givenSeriesEditorSession(api), series)
}
