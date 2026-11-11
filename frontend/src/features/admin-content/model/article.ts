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

/** Text fields the editor lets the Admin change. */
export interface ArticleFields {
  title: string
  slug: string
  summary: string
  bodyMarkdown: string
}

/** Relationship values the editor has no control for. They are sent back unchanged. */
export type ArticleRelationships = Pick<ArticleEditable, 'moduleId' | 'position' | 'topicIds'>

export const EMPTY_FIELDS: ArticleFields = { title: '', slug: '', summary: '', bodyMarkdown: '' }

export const STANDALONE: ArticleRelationships = { moduleId: null, position: null, topicIds: [] }

export function fieldsOf(article: ArticleAdminDetail): ArticleFields {
  return {
    title: article.title,
    slug: article.slug,
    summary: article.summary ?? '',
    bodyMarkdown: article.bodyMarkdown ?? '',
  }
}

export function relationshipsOf(article: ArticleAdminDetail): ArticleRelationships {
  return { moduleId: article.moduleId, position: article.position, topicIds: article.topicIds }
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
    topicIds: [...relationships.topicIds],
  }
}

export function sameFields(left: ArticleFields, right: ArticleFields): boolean {
  return (
    left.title === right.title &&
    left.slug === right.slug &&
    left.summary === right.summary &&
    left.bodyMarkdown === right.bodyMarkdown
  )
}
