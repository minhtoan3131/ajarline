import type { HttpClient } from '../../../shared/api/httpClient'
import type { PublicModule } from '../model/publicModule'
import type { PublicSeries } from '../model/publicSeries'
import type { SeriesCollectionPage } from '../model/seriesCollection'

const SERIES_PATH = '/api/v1/series'
const MODULES_PATH = '/api/v1/modules'

/** Series per Index page. The server accepts 1 to 50. */
export const SERIES_PAGE_SIZE = 20

/**
 * Reads one page of the public Series.
 *
 * An empty page, including a page after the last one, is a normal answer.
 * There is no 404 for this request.
 */
export async function fetchSeriesPage(
  client: HttpClient,
  page: number,
  size = SERIES_PAGE_SIZE,
  signal?: AbortSignal,
): Promise<SeriesCollectionPage> {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  const response = await client.request<SeriesCollectionPage>(`${SERIES_PATH}?${query.toString()}`, { signal })
  return response.body
}

/**
 * Reads one public Series by slug.
 *
 * @throws ApiError with status 404 when the Series is missing or not public
 */
export async function fetchPublicSeries(
  client: HttpClient,
  slug: string,
  signal?: AbortSignal,
): Promise<PublicSeries> {
  const response = await client.request<PublicSeries>(`${SERIES_PATH}/${encodeURIComponent(slug)}`, { signal })
  return response.body
}

/**
 * Reads one public Module by slug.
 *
 * @throws ApiError with status 404 when the Module is missing or not public
 */
export async function fetchPublicModule(
  client: HttpClient,
  slug: string,
  signal?: AbortSignal,
): Promise<PublicModule> {
  const response = await client.request<PublicModule>(`${MODULES_PATH}/${encodeURIComponent(slug)}`, { signal })
  return response.body
}
