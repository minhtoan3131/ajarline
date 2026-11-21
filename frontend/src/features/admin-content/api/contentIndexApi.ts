import type { HttpClient } from '../../../shared/api/httpClient'
import type { AdminContentIndex, AdminContentType } from '../model/contentIndex'
import type { ContentIndexQuery } from '../model/contentIndexQuery'

const CONTENT_PATH = '/api/admin/v1/content'

/** Items per page. The server accepts 1 to 100. */
export const CONTENT_INDEX_PAGE_SIZE = 20

/**
 * Reads one page of the Content Index, newest change first. A filter with no
 * value is not sent. An empty page, including a page after the last one, is a
 * normal answer.
 */
export async function listContent(
  client: HttpClient,
  query: ContentIndexQuery,
  signal?: AbortSignal,
): Promise<AdminContentIndex> {
  const parts: string[] = []
  if (query.types.length > 0) {
    parts.push(`types=${query.types.join(',')}`)
  }
  if (query.statuses.length > 0) {
    parts.push(`statuses=${query.statuses.join(',')}`)
  }
  if (query.q !== '') {
    parts.push(`q=${encodeURIComponent(query.q)}`)
  }
  parts.push(`page=${query.page}`, `size=${CONTENT_INDEX_PAGE_SIZE}`)
  const response = await client.request<AdminContentIndex>(`${CONTENT_PATH}?${parts.join('&')}`, { signal })
  return response.body
}

const DELETE_PATHS: Record<AdminContentType, string> = {
  article: '/api/admin/v1/articles',
  module: '/api/admin/v1/modules',
  series: '/api/admin/v1/series',
  news: '/api/admin/v1/news',
}

/**
 * Deletes one Draft that was never published. `etag` is the ETag of the
 * version the Admin sees. The server answers 204 with no body.
 *
 * @throws ApiError 404 CONTENT_NOT_FOUND, 409 CONTENT_ALREADY_EVER_PUBLISHED,
 *         409 CONTENT_IN_USE (Series and Module), 412 REVISION_CONFLICT
 */
export async function deleteContent(
  client: HttpClient,
  type: AdminContentType,
  id: string,
  etag: string,
): Promise<void> {
  await client.request<void>(`${DELETE_PATHS[type]}/${encodeURIComponent(id)}`, { method: 'DELETE', ifMatch: etag })
}
