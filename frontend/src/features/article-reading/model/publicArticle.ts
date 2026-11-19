import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** The Series of an Article. It exists only when the Module and the Series are public. */
export interface ArticleSeriesRef {
  id: string
  slug: string
  title: string
}

/** The Module of an Article. `position` is the rank of the Module in its Series. */
export interface ArticleModuleRef {
  id: string
  slug: string
  title: string
  position: number
}

/** The previous or next Article in the same Module. `position` is the rank of that Article. */
export interface ArticleSiblingRef {
  id: string
  slug: string
  title: string
  position: number
}

/**
 * Fields of the public Article response that the reading page renders.
 *
 * The wire response also has `type`, which the page does not use. When the
 * Module is not public, `series`, `module`, `previousArticle` and
 * `nextArticle` are all null.
 */
export interface PublicArticle {
  id: string
  slug: string
  title: string
  summary: string | null
  bodyMarkdown: string | null
  topics: PublicTopicRef[]
  series: ArticleSeriesRef | null
  module: ArticleModuleRef | null
  previousArticle: ArticleSiblingRef | null
  nextArticle: ArticleSiblingRef | null
  publishedAt: string
  updatedAt: string
}
