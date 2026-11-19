import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** One public News item as the Index row shows it. */
export interface NewsSummary {
  id: string
  slug: string
  title: string
  /** Every public News item has a summary, so the contract never sends null. */
  summary: string
  topics: PublicTopicRef[]
  /** When Ajarline first published the item. It is not the date of the source. */
  publishedAt: string
}

/**
 * One page of public News. There is no total; `hasMore` says whether a next
 * page exists. The wire response also has `size`, `type` and `updatedAt` of
 * each item, which the Index does not use.
 */
export interface NewsCollectionPage {
  page: number
  hasMore: boolean
  items: NewsSummary[]
}
