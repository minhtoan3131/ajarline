import type { HttpClient } from '../../../shared/api/httpClient'
import type { Versioned } from '../../admin-content/model/article'
import type { NewsAdminDetail, NewsEditable } from '../model/news'
import type { AdminNewsList } from '../model/newsList'

const NEWS_PATH = '/api/admin/v1/news'
const CONTENT_PATH = '/api/admin/v1/content'

/** News per list page. The server accepts 1 to 100. */
export const NEWS_LIST_PAGE_SIZE = 20

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

/**
 * Reads one page of News of every status, newest change first. An empty
 * page, including a page after the last one, is a normal answer.
 */
export async function listNews(client: HttpClient, page: number, signal?: AbortSignal): Promise<AdminNewsList> {
  const query = new URLSearchParams({ types: 'news', page: String(page), size: String(NEWS_LIST_PAGE_SIZE) })
  const response = await client.request<AdminNewsList>(`${CONTENT_PATH}?${query.toString()}`, { signal })
  return response.body
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
