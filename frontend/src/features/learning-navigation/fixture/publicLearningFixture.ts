import { JAVA_TOPIC_REF } from '../../topic-browsing/fixture/publicTopicFixture'
import type { SeriesSummary } from '../model/seriesCollection'

/** A Series as the wire sends it: the model fields plus the ones the Reader ignores. */
export type SeriesSummaryWire = SeriesSummary & {
  type: 'series'
  publishedAt: string
  updatedAt: string
}

/** Series Index request exactly as the Reader sends it. */
export function seriesCollectionApiPath(page: number, size = 20): string {
  return `/api/v1/series?page=${page}&size=${size}`
}

/** A Series assigned to the Java Topic. */
export function seriesSummary(changes: Partial<SeriesSummaryWire> = {}): SeriesSummaryWire {
  return {
    id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23',
    type: 'series',
    slug: 'java-concurrency',
    title: 'Java Concurrency',
    summary: 'Học xử lý đồng thời trong Java.',
    topics: [JAVA_TOPIC_REF],
    publishedAt: '2026-11-01T02:00:00Z',
    updatedAt: '2026-11-01T02:00:00Z',
    ...changes,
  }
}

/** Wire body of one page of Series. */
export function seriesCollectionResponse(
  items: SeriesSummaryWire[],
  changes: { page?: number; size?: number; hasMore?: boolean } = {},
): unknown {
  return { page: 0, size: 20, hasMore: false, items, ...changes }
}
