import type { HttpClient } from '../../../shared/api/httpClient'
import type { Versioned } from '../../admin-content/model/article'
import type { AdminContentList, ChildOrder, ChildPosition } from '../model/learning'
import type { ModuleAdminDetail, ModuleEditable } from '../model/module'
import type { SeriesAdminDetail, SeriesEditable } from '../model/series'

const SERIES_PATH = '/api/admin/v1/series'
const MODULES_PATH = '/api/admin/v1/modules'
const CONTENT_PATH = '/api/admin/v1/content'

/** Series per list page. The server accepts 1 to 100. */
export const SERIES_LIST_PAGE_SIZE = 20

function seriesPath(seriesId: string): string {
  return `${SERIES_PATH}/${encodeURIComponent(seriesId)}`
}

function modulePath(moduleId: string): string {
  return `${MODULES_PATH}/${encodeURIComponent(moduleId)}`
}

function requireEtag(etag: string | null): string {
  if (etag === null) {
    // Every Admin Series and Module response carries an ETag. Without it the
    // next write could not be protected, so the response is treated as broken.
    throw new Error('Admin learning response has no ETag header')
  }
  return etag
}

/**
 * Reads one page of Series of every status, newest change first. An empty
 * page, including a page after the last one, is a normal answer.
 */
export async function listSeries(client: HttpClient, page: number, signal?: AbortSignal): Promise<AdminContentList> {
  const query = new URLSearchParams({ types: 'series', page: String(page), size: String(SERIES_LIST_PAGE_SIZE) })
  const response = await client.request<AdminContentList>(`${CONTENT_PATH}?${query.toString()}`, { signal })
  return response.body
}

/** Creates a Draft. The server answers 201 with revision 1 and ETag "1". */
export async function createSeries(client: HttpClient, editable: SeriesEditable): Promise<Versioned<SeriesAdminDetail>> {
  const response = await client.request<SeriesAdminDetail>(SERIES_PATH, { method: 'POST', body: editable })
  return { value: response.body, etag: requireEtag(response.etag) }
}

export async function getSeries(
  client: HttpClient,
  seriesId: string,
  signal?: AbortSignal,
): Promise<Versioned<SeriesAdminDetail>> {
  const response = await client.request<SeriesAdminDetail>(seriesPath(seriesId), { signal })
  return { value: response.body, etag: requireEtag(response.etag) }
}

/**
 * Replaces the Series when its current ETag still matches.
 *
 * @throws ApiError 412 REVISION_CONFLICT when the Series changed elsewhere
 */
export async function updateSeries(
  client: HttpClient,
  seriesId: string,
  editable: SeriesEditable,
  etag: string,
): Promise<Versioned<SeriesAdminDetail>> {
  const response = await client.request<SeriesAdminDetail>(seriesPath(seriesId), {
    method: 'PUT',
    body: editable,
    ifMatch: etag,
  })
  return { value: response.body, etag: requireEtag(response.etag) }
}

/**
 * Creates a Draft Module at the given position of its Series.
 *
 * @throws ApiError 422 SERIES_REFERENCE_NOT_FOUND or INVALID_POSITION
 */
export async function createModule(client: HttpClient, editable: ModuleEditable): Promise<Versioned<ModuleAdminDetail>> {
  const response = await client.request<ModuleAdminDetail>(MODULES_PATH, { method: 'POST', body: editable })
  return { value: response.body, etag: requireEtag(response.etag) }
}

export async function getModule(
  client: HttpClient,
  moduleId: string,
  signal?: AbortSignal,
): Promise<Versioned<ModuleAdminDetail>> {
  const response = await client.request<ModuleAdminDetail>(modulePath(moduleId), { signal })
  return { value: response.body, etag: requireEtag(response.etag) }
}

/**
 * Replaces the Module when its current ETag still matches. It may also move
 * the Module to another Series or position.
 *
 * @throws ApiError 412 REVISION_CONFLICT when the Module changed elsewhere
 */
export async function updateModule(
  client: HttpClient,
  moduleId: string,
  editable: ModuleEditable,
  etag: string,
): Promise<Versioned<ModuleAdminDetail>> {
  const response = await client.request<ModuleAdminDetail>(modulePath(moduleId), {
    method: 'PUT',
    body: editable,
    ifMatch: etag,
  })
  return { value: response.body, etag: requireEtag(response.etag) }
}

interface ModuleOrderBody {
  revision: number
  modules: ChildPosition[]
}

interface ArticleOrderBody {
  revision: number
  articles: ChildPosition[]
}

/**
 * Saves a new order of the Modules of a Series. `etag` is the ETag of the
 * Series, not of a Module. The ids must be exactly the current Modules.
 *
 * @throws ApiError 409 ORDER_SET_CONFLICT when the set of Modules differs,
 *         412 REVISION_CONFLICT when the Series changed
 */
export async function reorderModules(
  client: HttpClient,
  seriesId: string,
  moduleIds: string[],
  etag: string,
): Promise<Versioned<ChildOrder>> {
  const response = await client.request<ModuleOrderBody>(`${seriesPath(seriesId)}/module-order`, {
    method: 'PUT',
    body: { moduleIds },
    ifMatch: etag,
  })
  return { value: { revision: response.body.revision, children: response.body.modules }, etag: requireEtag(response.etag) }
}

/**
 * Saves a new order of the Articles of a Module. `etag` is the ETag of the Module.
 *
 * @throws ApiError 409 ORDER_SET_CONFLICT, 412 REVISION_CONFLICT
 */
export async function reorderArticles(
  client: HttpClient,
  moduleId: string,
  articleIds: string[],
  etag: string,
): Promise<Versioned<ChildOrder>> {
  const response = await client.request<ArticleOrderBody>(`${modulePath(moduleId)}/article-order`, {
    method: 'PUT',
    body: { articleIds },
    ifMatch: etag,
  })
  return { value: { revision: response.body.revision, children: response.body.articles }, etag: requireEtag(response.etag) }
}
