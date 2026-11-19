import type { HttpClient } from '../../../shared/api/httpClient'
import type { SeriesCollectionPage } from '../model/seriesCollection'

const SERIES_PATH = '/api/v1/series'

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
