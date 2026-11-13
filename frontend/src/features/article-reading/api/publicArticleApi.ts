import type { HttpClient } from '../../../shared/api/httpClient'
import type { PublicArticle } from '../model/publicArticle'

/**
 * Reads one public Article by slug.
 *
 * @throws ApiError with status 404 when the Article is missing or not public
 */
export async function fetchPublicArticle(
  client: HttpClient,
  slug: string,
  signal?: AbortSignal,
): Promise<PublicArticle> {
  const response = await client.request<PublicArticle>(`/api/v1/articles/${encodeURIComponent(slug)}`, { signal })
  return response.body
}
