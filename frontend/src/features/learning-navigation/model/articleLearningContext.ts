import { modulePath, seriesPath } from '../learningRoutes'

/**
 * What the Article page gives in. It is a small shape, so this feature does
 * not import the Article model. The response of an Article fits it.
 */
export interface ArticleLearningContextInput {
  series: { slug: string; title: string } | null
  module: { slug: string; title: string; position: number } | null
  previousArticle: { position: number } | null
}

/** What the page shows. The display rules are already applied. */
export interface ArticleLearningContext {
  module: {
    title: string
    path: string
    /** The rank of the Module in its Series. It is null when the Series is hidden. */
    position: number | null
  }
  /** The Series link. It is null when the Series is hidden. */
  series: { title: string; path: string } | null
  /** The rank of this Article in its Module, counted from 1. */
  ownRank: number
}

/**
 * Decides the learning context of an Article.
 *
 * No Module means no context at all, even if a Series came with it. A hidden
 * Series leaves no Series link and no Module position: "Module 3" would tell
 * the reader that a hidden Series exists.
 *
 * The rank of this Article is the rank of the previous Article plus one.
 * The server ranks Articles without gaps, so this sum is exact.
 */
export function articleContextOf(input: ArticleLearningContextInput): ArticleLearningContext | null {
  const { series, module, previousArticle } = input
  if (module === null) {
    return null
  }
  return {
    module: {
      title: module.title,
      path: modulePath(module.slug),
      position: series === null ? null : module.position,
    },
    series: series === null ? null : { title: series.title, path: seriesPath(series.slug) },
    ownRank: previousArticle === null ? 1 : previousArticle.position + 1,
  }
}
