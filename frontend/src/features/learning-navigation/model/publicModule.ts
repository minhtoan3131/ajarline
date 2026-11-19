import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'

/** One public Article inside a Module. `position` is its rank among the public Articles (1, 2, 3…). */
export interface ModuleArticleItem {
  id: string
  slug: string
  title: string
  summary: string
  position: number
}

/** The Series of a Module. It exists only when that Series is public. */
export interface ModuleSeriesRef {
  id: string
  slug: string
  title: string
}

/**
 * Public Module as the Module page shows it. `articles` always has at least
 * one item, because a public Module has a public Article.
 */
export interface PublicModule {
  id: string
  slug: string
  title: string
  summary: string
  overviewMarkdown: string
  topics: PublicTopicRef[]
  series: ModuleSeriesRef | null
  articles: ModuleArticleItem[]
}
