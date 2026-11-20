import { JAVA_TOPIC_REF } from '../../topic-browsing/fixture/publicTopicFixture'
import type { NewsSummary } from '../model/newsCollection'
import type { PublicNews } from '../model/publicNews'

/** A News item as the wire sends it in a list: the model fields plus the ones the Reader ignores. */
export type NewsSummaryWire = NewsSummary & {
  type: 'news'
  updatedAt: string
}

/** Index request exactly as the Reader sends it. */
export function newsCollectionApiPath(page: number, size = 20): string {
  return `/api/v1/news?page=${page}&size=${size}`
}

/** A News item assigned to the Java Topic. */
export function newsSummary(changes: Partial<NewsSummaryWire> = {}): NewsSummaryWire {
  return {
    id: '6b2d8e40-1a97-4c35-b8f2-3d5e7a9c0b14',
    type: 'news',
    slug: 'spring-boot-4-1',
    title: 'Spring Boot 4.1 ra mắt',
    summary: 'Bản phát hành mới có nhiều thay đổi.',
    topics: [JAVA_TOPIC_REF],
    publishedAt: '2026-11-04T02:00:00Z',
    updatedAt: '2026-11-04T02:00:00Z',
    ...changes,
  }
}

/** Wire body of one page of News. */
export function newsCollectionResponse(
  items: NewsSummaryWire[],
  changes: { page?: number; size?: number; hasMore?: boolean } = {},
): unknown {
  return { page: 0, size: 20, hasMore: false, items, ...changes }
}

export function newsApiPath(slug: string): string {
  return `/api/v1/news/${slug}`
}

export type PublicNewsWire = PublicNews & { type: 'news' }

/** A public News item with a safe source and a source date. */
export function publicNews(changes: Partial<PublicNewsWire> = {}): PublicNewsWire {
  return {
    id: '6b2d8e40-1a97-4c35-b8f2-3d5e7a9c0b14',
    type: 'news',
    slug: 'spring-boot-4-1',
    title: 'Spring Boot 4.1 ra mắt',
    summary: 'Bản phát hành mới có nhiều thay đổi.',
    bodyMarkdown: '## Điểm mới\n\nNội dung tin.',
    topics: [JAVA_TOPIC_REF],
    source: {
      name: 'Spring Blog',
      url: 'https://spring.io/blog/spring-boot-4-1',
      publishedAt: '2026-11-03T00:00:00Z',
    },
    publishedAt: '2026-11-04T02:00:00Z',
    updatedAt: '2026-11-04T02:00:00Z',
    ...changes,
  }
}
