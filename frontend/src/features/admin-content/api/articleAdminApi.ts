import type { HttpClient } from '../../../shared/api/httpClient'
import type { ArticleAdminDetail, ArticleEditable, Versioned } from '../model/article'

const ARTICLES_PATH = '/api/admin/v1/articles'

function articlePath(articleId: string): string {
  return `${ARTICLES_PATH}/${encodeURIComponent(articleId)}`
}

function requireEtag(etag: string | null): string {
  if (etag === null) {
    // Every Admin Article response carries an ETag. Without it the next
    // write could not be protected, so the response is treated as broken.
    throw new Error('Admin Article response has no ETag header')
  }
  return etag
}

/** Creates a Draft. The server answers 201 with revision 1 and ETag "1". */
export async function createArticle(client: HttpClient, editable: ArticleEditable): Promise<Versioned<ArticleAdminDetail>> {
  const response = await client.request<ArticleAdminDetail>(ARTICLES_PATH, { method: 'POST', body: editable })
  return { value: response.body, etag: requireEtag(response.etag) }
}

export async function getArticle(
  client: HttpClient,
  articleId: string,
  signal?: AbortSignal,
): Promise<Versioned<ArticleAdminDetail>> {
  const response = await client.request<ArticleAdminDetail>(articlePath(articleId), { signal })
  return { value: response.body, etag: requireEtag(response.etag) }
}

/**
 * Replaces the Article when its current ETag still matches.
 *
 * @throws ApiError 412 REVISION_CONFLICT when the Article changed elsewhere
 */
export async function updateArticle(
  client: HttpClient,
  articleId: string,
  editable: ArticleEditable,
  etag: string,
): Promise<Versioned<ArticleAdminDetail>> {
  const response = await client.request<ArticleAdminDetail>(articlePath(articleId), {
    method: 'PUT',
    body: editable,
    ifMatch: etag,
  })
  return { value: response.body, etag: requireEtag(response.etag) }
}
