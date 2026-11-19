import { JAVA_TOPIC_REF } from '../../topic-browsing/fixture/publicTopicFixture'
import type { ModuleArticleItem, ModuleSeriesRef, PublicModule } from '../model/publicModule'
import type { PublicSeries, SeriesModuleItem } from '../model/publicSeries'
import type { SeriesSummary } from '../model/seriesCollection'

const PUBLISHED_AT = '2026-11-01T02:00:00Z'

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
    publishedAt: PUBLISHED_AT,
    updatedAt: PUBLISHED_AT,
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

export function seriesApiPath(slug: string): string {
  return `/api/v1/series/${slug}`
}

export function moduleApiPath(slug: string): string {
  return `/api/v1/modules/${slug}`
}

/** The first Article of the sample Module, rank 1. */
export function moduleArticleItem(changes: Partial<ModuleArticleItem> = {}): ModuleArticleItem {
  return {
    id: '7e5d3a91-2c48-4b06-8f1d-9a0b4c6e2d35',
    slug: 'tao-thread',
    title: 'Tạo Thread',
    summary: 'Cách tạo và chạy một Thread.',
    position: 1,
    ...changes,
  }
}

/** The second Article of the sample Module, rank 2. */
export function secondModuleArticleItem(changes: Partial<ModuleArticleItem> = {}): ModuleArticleItem {
  return moduleArticleItem({
    id: '3b9c1d7e-5a24-4e68-b0f3-8c1d2e4a6b79',
    slug: 'dong-bo-hoa',
    title: 'Đồng bộ hoá',
    summary: 'Dùng synchronized và lock.',
    position: 2,
    ...changes,
  })
}

/** The first Module of the sample Series, with one Article. */
export function seriesModuleItem(changes: Partial<SeriesModuleItem> = {}): SeriesModuleItem {
  return {
    id: '5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d',
    slug: 'thread-co-ban',
    title: 'Thread cơ bản',
    summary: 'Làm quen với Thread.',
    position: 1,
    articles: [moduleArticleItem()],
    ...changes,
  }
}

export type PublicSeriesWire = PublicSeries & { type: 'series'; publishedAt: string; updatedAt: string }

/** A public Series with one Module that has one Article. */
export function publicSeries(changes: Partial<PublicSeriesWire> = {}): PublicSeriesWire {
  return {
    id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23',
    type: 'series',
    slug: 'java-concurrency',
    title: 'Java Concurrency',
    summary: 'Học xử lý đồng thời trong Java.',
    overviewMarkdown: '## Mục tiêu\n\nHiểu mô hình luồng của Java.',
    topics: [JAVA_TOPIC_REF],
    modules: [seriesModuleItem()],
    publishedAt: PUBLISHED_AT,
    updatedAt: PUBLISHED_AT,
    ...changes,
  }
}

/** The public Series of the sample Module. */
export const MODULE_SERIES_REF: ModuleSeriesRef = {
  id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23',
  slug: 'java-concurrency',
  title: 'Java Concurrency',
}

export type PublicModuleWire = PublicModule & { type: 'module'; publishedAt: string; updatedAt: string }

/** A public Module in a public Series, with two Articles. */
export function publicModule(changes: Partial<PublicModuleWire> = {}): PublicModuleWire {
  return {
    id: '5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d',
    type: 'module',
    slug: 'thread-co-ban',
    title: 'Thread cơ bản',
    summary: 'Làm quen với Thread.',
    overviewMarkdown: '## Nội dung\n\nCác bài về Thread.',
    topics: [],
    series: MODULE_SERIES_REF,
    articles: [moduleArticleItem(), secondModuleArticleItem()],
    publishedAt: PUBLISHED_AT,
    updatedAt: PUBLISHED_AT,
    ...changes,
  }
}
