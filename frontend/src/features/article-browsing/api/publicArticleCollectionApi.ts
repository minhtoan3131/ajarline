import type { HttpClient } from '../../../shared/api/httpClient'
import type { ArticleCollectionPage } from '../model/articleCollection'

const ARTICLES_PATH = '/api/v1/articles'

/** Articles per Index page. The server accepts 1 to 50. */
export const ARTICLE_COLLECTION_PAGE_SIZE = 20

/**
 * Reads one page of the public standalone Articles.
 *
 * An empty page, including a page after the last one, is a normal answer.
 * There is no 404 for this request.
 */
export async function fetchStandaloneArticles(
  client: HttpClient,
  page: number,
  size = ARTICLE_COLLECTION_PAGE_SIZE,
  signal?: AbortSignal,
): Promise<ArticleCollectionPage> {
  const query = new URLSearchParams({ standalone: 'true', page: String(page), size: String(size) })
  const response = await client.request<ArticleCollectionPage>(`${ARTICLES_PATH}?${query.toString()}`, { signal })
  return response.body
}
