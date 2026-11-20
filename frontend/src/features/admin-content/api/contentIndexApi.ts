import type { HttpClient } from '../../../shared/api/httpClient'
import type { AdminContentIndex } from '../model/contentIndex'
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
