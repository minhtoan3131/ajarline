import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** Where a News item came from. Only `publishedAt` can be null. */
export interface NewsSource {
  name: string
  url: string
  /** The date the source published it. It is null when the source gave none. */
  publishedAt: string | null
}

/**
 * Fields of the public News response that the reading page renders.
 *
 * The wire response also has `type`, which the page does not use. A News
 * item has no series, module or previous/next item.
 */
export interface PublicNews {
  id: string
  slug: string
  title: string
  summary: string
  bodyMarkdown: string
  topics: PublicTopicRef[]
  source: NewsSource
  /** When Ajarline first published the item. */
  publishedAt: string
  updatedAt: string
}
