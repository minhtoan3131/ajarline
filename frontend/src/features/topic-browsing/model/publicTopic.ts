/** Public identity of a Topic, as every public response embeds it. */
export interface PublicTopicRef {
  id: string
  slug: string
  name: string
}

/** One public Topic. The public API never says whether it is active. */
export interface PublicTopic {
  id: string
  slug: string
  name: string
  description: string | null
}

export type PublicContentType = 'series' | 'module' | 'article' | 'news'

export interface SeriesContextRef {
  id: string
  slug: string
  title: string
}

export interface ModuleContextRef {
  id: string
  slug: string
  title: string
  position: number
}

/** Public parents of an item. A parent that is not public is null. */
export interface TopicContentContext {
  series: SeriesContextRef | null
  module: ModuleContextRef | null
}

export interface TopicContentItem {
  id: string
  type: PublicContentType
  slug: string
  title: string
  summary: string
  topics: PublicTopicRef[]
  publishedAt: string
  updatedAt: string
  context: TopicContentContext
}

/**
 * One page of the content directly assigned to a Topic. There is no total;
 * `hasMore` says whether a next page exists. The wire response also echoes
 * the type filter, which the Reader does not send yet.
 */
export interface TopicContentPage {
  page: number
  size: number
  hasMore: boolean
  items: TopicContentItem[]
}
