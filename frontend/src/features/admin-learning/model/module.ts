import type { PublicationStatus } from '../../admin-content/model/article'
import {
  EMPTY_LEARNING_FIELDS,
  learningFieldsOf,
  type LearningChild,
  type LearningFields,
  type LearningPayload,
  type ParentFields,
} from './learning'

/** Full request body of create and update. Update replaces every field. */
export interface ModuleEditable extends LearningPayload {
  seriesId: string
  /** Position among the Modules of the Series, from 1. */
  position: number
}

export interface ModuleAdminDetail extends ModuleEditable {
  id: string
  type: 'module'
  status: PublicationStatus
  revision: number
  publishedAt: string | null
  unpublishedAt: string | null
  createdAt: string
  updatedAt: string
  /** Articles in order, every status. Read-only: a PUT body has no such key. */
  articles: LearningChild[]
}

/** Form values of a saved Module. The parent is text, like the form holds it. */
export function moduleFieldsOf(detail: ModuleAdminDetail): LearningFields {
  return {
    ...learningFieldsOf(detail),
    parent: { seriesId: detail.seriesId, position: String(detail.position) },
  }
}

/** Form values of a new Module. `parent` is the Series chosen on entry, if there is one. */
export function blankModuleFields(parent: ParentFields | null): LearningFields {
  return { ...EMPTY_LEARNING_FIELDS, parent: parent ?? { seriesId: '', position: '' } }
}

/** The Admin has not chosen a Series, or the position is not a whole number from 1. */
export class ParentNotReadyError extends Error {
  constructor() {
    super('The Module has no Series or no valid position yet')
    this.name = 'ParentNotReadyError'
  }
}

/**
 * Builds the request body. Empty optional text is sent as null, which is how
 * the server stores it, so saving an unchanged form stays a no-op.
 *
 * @throws ParentNotReadyError when the Series or the position is not usable
 */
export function toModuleEditable(fields: LearningFields): ModuleEditable {
  const parent = fields.parent
  const position = parent === null ? Number.NaN : Number(parent.position)
  if (parent === null || parent.seriesId === '' || !Number.isInteger(position) || position < 1) {
    throw new ParentNotReadyError()
  }
  return {
    title: fields.title,
    slug: fields.slug,
    summary: fields.summary === '' ? null : fields.summary,
    overviewMarkdown: fields.overviewMarkdown === '' ? null : fields.overviewMarkdown,
    topicIds: [...fields.topicIds],
    seriesId: parent.seriesId,
    position,
  }
}
