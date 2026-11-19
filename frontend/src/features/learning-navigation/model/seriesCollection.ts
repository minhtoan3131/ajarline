import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** One public Series as the Index row shows it. */
export interface SeriesSummary {
  id: string
  slug: string
  title: string
  /** Every public Series has a summary, so the contract never sends null. */
  summary: string
  topics: PublicTopicRef[]
}

/**
 * One page of public Series. There is no total; `hasMore` says whether a next
 * page exists.
 */
export interface SeriesCollectionPage {
  page: number
  hasMore: boolean
  items: SeriesSummary[]
}
