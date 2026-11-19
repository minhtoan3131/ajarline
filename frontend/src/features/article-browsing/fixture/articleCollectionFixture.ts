import { JAVA_TOPIC_REF } from '../../topic-browsing/fixture/publicTopicFixture'
import type { ArticleSummary } from '../model/articleCollection'

/** An Article as the wire sends it: the model fields plus the ones the Reader ignores. */
export type ArticleSummaryWire = ArticleSummary & {
  type: 'article'
  publishedAt: string
  updatedAt: string
}

/** Request exactly as the Reader sends it. */
export function standaloneArticlesPath(page: number, size = 20): string {
  return `/api/v1/articles?standalone=true&page=${page}&size=${size}`
}

/** A standalone Article assigned to the Java Topic. */
export function articleSummary(changes: Partial<ArticleSummaryWire> = {}): ArticleSummaryWire {
  return {
    id: '9d4f2a61-7c3e-4b85-a0d2-6e1f8c5b3a47',
    type: 'article',
    slug: 'hieu-protobuf',
    title: 'Hiểu Protobuf với Java',
    summary: 'Tóm tắt bài viết.',
    topics: [JAVA_TOPIC_REF],
    publishedAt: '2026-11-01T02:00:00Z',
    updatedAt: '2026-11-01T02:00:00Z',
    ...changes,
  }
}

/** Wire body of one page of standalone Articles. */
export function articleCollectionResponse(
  items: ArticleSummaryWire[],
  changes: { page?: number; size?: number; hasMore?: boolean } = {},
): unknown {
  return { page: 0, size: 20, hasMore: false, items, ...changes }
}
