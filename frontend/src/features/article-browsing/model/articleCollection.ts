import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** One standalone Article as the Index row shows it. */
export interface ArticleSummary {
  id: string
  slug: string
  title: string
  /** Every public Article has a summary, so the contract never sends null. */
  summary: string
  topics: PublicTopicRef[]
}

/**
 * One page of standalone Articles. There is no total; `hasMore` says whether
 * a next page exists.
 */
export interface ArticleCollectionPage {
  page: number
  hasMore: boolean
  items: ArticleSummary[]
}
