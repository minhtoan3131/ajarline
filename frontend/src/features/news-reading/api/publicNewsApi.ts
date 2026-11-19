import type { HttpClient } from '../../../shared/api/httpClient'
import type { NewsCollectionPage } from '../model/newsCollection'
import type { PublicNews } from '../model/publicNews'

const NEWS_PATH = '/api/v1/news'

/** News items per Index page. The server accepts 1 to 50. */
export const NEWS_PAGE_SIZE = 20

/**
 * Reads one page of the public News, newest first.
 *
 * An empty page, including a page after the last one, is a normal answer.
 * There is no 404 for this request.
 */
export async function fetchNewsPage(
  client: HttpClient,
  page: number,
  size = NEWS_PAGE_SIZE,
  signal?: AbortSignal,
): Promise<NewsCollectionPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  const response = await client.request<NewsCollectionPage>(`${NEWS_PATH}?${query.toString()}`, { signal })
  return response.body
}

/**
 * Reads one public News item by slug.
 *
 * @throws ApiError with status 404 when the item is missing or not public
 */
export async function fetchPublicNews(
  client: HttpClient,
  slug: string,
  signal?: AbortSignal,
): Promise<PublicNews> {
  const response = await client.request<PublicNews>(`${NEWS_PATH}/${encodeURIComponent(slug)}`, { signal })
  return response.body
}
