import type { PublicTopicRef } from '../../topic-browsing/model/publicTopic'
import type { ModuleArticleItem } from './publicModule'

/** One public Module inside a Series. `position` is its rank among the public Modules. */
export interface SeriesModuleItem {
  id: string
  slug: string
  title: string
  summary: string
  position: number
  articles: ModuleArticleItem[]
}

/**
 * Public Series as the Series page shows it. `modules` always has at least
 * one item, and each Module has at least one Article.
 */
export interface PublicSeries {
  id: string
  slug: string
  title: string
  summary: string
  overviewMarkdown: string
  topics: PublicTopicRef[]
  modules: SeriesModuleItem[]
}
