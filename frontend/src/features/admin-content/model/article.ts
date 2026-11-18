export type PublicationStatus = 'DRAFT' | 'PUBLISHED' | 'UNPUBLISHED'

/** Value with the ETag of the server version it came from. */
export interface Versioned<T> {
  value: T
  etag: string
}

/** Full request body of create and update. Update replaces every field. */
export interface ArticleEditable {
  title: string
  slug: string
  summary: string | null
  bodyMarkdown: string | null
  moduleId: string | null
  position: number | null
  topicIds: string[]
}

export interface ArticleAdminDetail extends ArticleEditable {
  id: string
  type: 'article'
  status: PublicationStatus
  revision: number
  publishedAt: string | null
  unpublishedAt: string | null
  createdAt: string
  updatedAt: string
}

/** Values the editor lets the Admin change. */
export interface ArticleFields {
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
  /** Ids of the chosen Topics. Their order carries no meaning. */
  topicIds: string[]
}

/** Fields that are edited as text. The Topic choice has its own control. */
export type ArticleTextField = Exclude<keyof ArticleFields, 'topicIds'>

/** Relationship values the editor has no control for. They are sent back unchanged. */
export type ArticleRelationships = Pick<ArticleEditable, 'moduleId' | 'position'>

export const EMPTY_FIELDS: ArticleFields = { title: '', slug: '', summary: '', bodyMarkdown: '', topicIds: [] }

export const STANDALONE: ArticleRelationships = { moduleId: null, position: null }

export function fieldsOf(article: ArticleAdminDetail): ArticleFields {
  return {
    title: article.title,
    slug: article.slug,
    summary: article.summary ?? '',
    bodyMarkdown: article.bodyMarkdown ?? '',
    topicIds: [...article.topicIds],
  }
}

export function relationshipsOf(article: ArticleAdminDetail): ArticleRelationships {
  return { moduleId: article.moduleId, position: article.position }
}

/**
 * Builds the request body. Empty optional text is sent as null, which is
 * how the server stores it, so saving an unchanged form stays a no-op.
 */
export function toEditable(fields: ArticleFields, relationships: ArticleRelationships): ArticleEditable {
  return {
    title: fields.title,
    slug: fields.slug,
    summary: fields.summary === '' ? null : fields.summary,
    bodyMarkdown: fields.bodyMarkdown === '' ? null : fields.bodyMarkdown,
    moduleId: relationships.moduleId,
    position: relationships.position,
    topicIds: [...fields.topicIds],
  }
}

/** True when both lists hold the same ids, whatever their order. */
export function sameIds(left: string[], right: string[]): boolean {
  return left.length === right.length && [...left].sort().join(',') === [...right].sort().join(',')
}

export function sameFields(left: ArticleFields, right: ArticleFields): boolean {
  return (
    left.title === right.title &&
    left.slug === right.slug &&
    left.summary === right.summary &&
    left.bodyMarkdown === right.bodyMarkdown &&
    sameIds(left.topicIds, right.topicIds)
  )
}

/** Publication result returned by publish and unpublish. It has no content fields. */
export interface PublicationState {
  id: string
  type: 'article' | 'series' | 'module'
  status: PublicationStatus
  revision: number
  publishedAt: string | null
  unpublishedAt: string | null
  updatedAt: string
}
