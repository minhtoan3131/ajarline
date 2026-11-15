import type { PublicTopic, PublicTopicRef, TopicContentItem } from '../model/publicTopic'

export const TOPICS_PATH = '/api/v1/topics'

export const JAVA_TOPIC_REF: PublicTopicRef = {
  id: '3b1e7c52-8f4a-4d2b-9c61-0a5e2f7d8b94',
  slug: 'java',
  name: 'Java',
}

export const SPRING_TOPIC_REF: PublicTopicRef = {
  id: 'e6c0a9d1-24b7-4f3e-8a15-7d92c4b0f638',
  slug: 'spring-boot',
  name: 'Spring Boot',
}

/** Body of the public Topic list. */
export function topicDiscovery(...topics: PublicTopicRef[]): { topics: PublicTopicRef[] } {
  return { topics }
}

export function topicPath(slug: string): string {
  return `${TOPICS_PATH}/${slug}`
}

/** Content request exactly as the Reader sends it, size included. */
export function topicContentPath(slug: string, page: number): string {
  return `${TOPICS_PATH}/${slug}/content?page=${page}&size=20`
}

export function publicTopic(changes: Partial<PublicTopic> = {}): PublicTopic {
  return { ...JAVA_TOPIC_REF, description: 'Ngôn ngữ và nền tảng Java.', ...changes }
}

/** A public standalone Article assigned to the Java Topic. */
export function contentItem(changes: Partial<TopicContentItem> = {}): TopicContentItem {
  return {
    id: '9d4f2a61-7c3e-4b85-a0d2-6e1f8c5b3a47',
    type: 'article',
    slug: 'hieu-protobuf',
    title: 'Hiểu Protobuf với Java',
    summary: 'Tóm tắt bài viết.',
    topics: [JAVA_TOPIC_REF],
    publishedAt: '2026-11-01T02:00:00Z',
    updatedAt: '2026-11-01T02:00:00Z',
    context: { series: null, module: null },
    ...changes,
  }
}

/** Wire body of one content page, including the echoed filter. */
export function topicContentResponse(
  items: TopicContentItem[],
  changes: { page?: number; hasMore?: boolean } = {},
): unknown {
  return { filters: { types: [] }, page: 0, size: 20, hasMore: false, items, ...changes }
}
