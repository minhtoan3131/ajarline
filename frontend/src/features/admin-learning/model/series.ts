import type { PublicationStatus } from '../../admin-content/model/article'
import type { LearningChild, LearningFields, LearningPayload } from './learning'

/** Full request body of create and update. Update replaces every field. */
export type SeriesEditable = LearningPayload

export interface SeriesAdminDetail extends SeriesEditable {
  id: string
  type: 'series'
  status: PublicationStatus
  revision: number
  publishedAt: string | null
  unpublishedAt: string | null
  createdAt: string
  updatedAt: string
  /** Modules in order, every status. Read-only: a PUT body has no such key. */
  modules: LearningChild[]
}

/**
 * Builds the request body. Empty optional text is sent as null, which is how
 * the server stores it, so saving an unchanged form stays a no-op.
 */
export function toSeriesEditable(fields: LearningFields): SeriesEditable {
  return {
    title: fields.title,
    slug: fields.slug,
    summary: fields.summary === '' ? null : fields.summary,
    overviewMarkdown: fields.overviewMarkdown === '' ? null : fields.overviewMarkdown,
    topicIds: [...fields.topicIds],
  }
}
