import { sameIds, type PublicationStatus } from '../../admin-content/model/article'

/** The editable text and Topics that a Series and a Module share on the wire. */
export interface LearningPayload {
  title: string
  slug: string
  summary: string | null
  overviewMarkdown: string | null
  topicIds: string[]
}

/** One child in the ordered list of a Series. It is read-only here. */
export interface LearningChild {
  id: string
  title: string
  slug: string
  /** The child's own status, not whether readers see it. */
  status: PublicationStatus
  position: number
}

/** Values the editor lets the Admin change. Optional text is an empty string here. */
export interface LearningFields {
  title: string
  slug: string
  summary: string
  overviewMarkdown: string
  /** Ids of the chosen Topics. Their order carries no meaning. */
  topicIds: string[]
}

/** Fields that are edited as text. The Topic choice has its own control. */
export type LearningTextField = Exclude<keyof LearningFields, 'topicIds'>

export const EMPTY_LEARNING_FIELDS: LearningFields = {
  title: '',
  slug: '',
  summary: '',
  overviewMarkdown: '',
  topicIds: [],
}

export function learningFieldsOf(record: LearningPayload): LearningFields {
  return {
    title: record.title,
    slug: record.slug,
    summary: record.summary ?? '',
    overviewMarkdown: record.overviewMarkdown ?? '',
    topicIds: [...record.topicIds],
  }
}

export function sameLearningFields(left: LearningFields, right: LearningFields): boolean {
  return (
    left.title === right.title &&
    left.slug === right.slug &&
    left.summary === right.summary &&
    left.overviewMarkdown === right.overviewMarkdown &&
    sameIds(left.topicIds, right.topicIds)
  )
}

/** One row of the Admin content list. Only the values the list shows. */
export interface AdminContentItem {
  id: string
  slug: string
  title: string
  status: PublicationStatus
}

/** One page of the Admin content list. There is no total, only `hasMore`. */
export interface AdminContentList {
  page: number
  hasMore: boolean
  items: AdminContentItem[]
}
