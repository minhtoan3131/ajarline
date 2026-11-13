/**
 * Fields of the public Article response that the reading page renders.
 *
 * The wire response has more fields (topics, series, module, previous and
 * next Article). They are added here together with the UI that renders them.
 */
export interface PublicArticle {
  id: string
  slug: string
  title: string
  summary: string | null
  bodyMarkdown: string | null
  publishedAt: string
  updatedAt: string
}
