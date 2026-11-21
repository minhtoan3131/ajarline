import type { HttpClient } from '../../../shared/api/httpClient'
import type { Versioned } from '../../admin-content/model/article'
import type { NewsAdminDetail, NewsEditable } from '../model/news'

const NEWS_PATH = '/api/admin/v1/news'

function newsPath(newsId: string): string {
  return `${NEWS_PATH}/${encodeURIComponent(newsId)}`
}

function requireEtag(etag: string | null): string {
  if (etag === null) {
    // Every Admin News response carries an ETag. Without it the next
    // write could not be protected, so the response is treated as broken.
    throw new Error('Admin News response has no ETag header')
  }
  return etag
}

/** Creates a Draft. The server answers 201 with revision 1 and ETag "1". */
export async function createNews(client: HttpClient, editable: NewsEditable): Promise<Versioned<NewsAdminDetail>> {
  const response = await client.request<NewsAdminDetail>(NEWS_PATH, { method: 'POST', body: editable })
  return { value: response.body, etag: requireEtag(response.etag) }
}

export async function getNews(
  client: HttpClient,
  newsId: string,
  signal?: AbortSignal,
): Promise<Versioned<NewsAdminDetail>> {
  const response = await client.request<NewsAdminDetail>(newsPath(newsId), { signal })
  return { value: response.body, etag: requireEtag(response.etag) }
}

/**
 * Replaces the News item when its current ETag still matches.
 *
 * @throws ApiError 412 REVISION_CONFLICT when the News item changed elsewhere
 */
export async function updateNews(
  client: HttpClient,
  newsId: string,
  editable: NewsEditable,
  etag: string,
): Promise<Versioned<NewsAdminDetail>> {
  const response = await client.request<NewsAdminDetail>(newsPath(newsId), {
    method: 'PUT',
    body: editable,
    ifMatch: etag,
  })
  return { value: response.body, etag: requireEtag(response.etag) }
}
