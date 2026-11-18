import type { HttpClient } from '../../../shared/api/httpClient'
import type { Versioned } from '../../admin-content/model/article'
import type { AdminContentList } from '../model/learning'
import type { SeriesAdminDetail, SeriesEditable } from '../model/series'

const SERIES_PATH = '/api/admin/v1/series'
const CONTENT_PATH = '/api/admin/v1/content'

/** Series per list page. The server accepts 1 to 100. */
export const SERIES_LIST_PAGE_SIZE = 20

function seriesPath(seriesId: string): string {
  return `${SERIES_PATH}/${encodeURIComponent(seriesId)}`
}

function requireEtag(etag: string | null): string {
  if (etag === null) {
    // Every Admin Series response carries an ETag. Without it the next
    // write could not be protected, so the response is treated as broken.
    throw new Error('Admin Series response has no ETag header')
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
