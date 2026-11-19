import { sameIds, type PublicationStatus } from '../../admin-content/model/article'

/** The editable text and Topics that a Series and a Module share on the wire. */
export interface LearningPayload {
  title: string
  slug: string
  summary: string | null
  overviewMarkdown: string | null
  topicIds: string[]
}

/** One child in the ordered list of a Series or a Module. It is read-only here. */
export interface LearningChild {
  id: string
  title: string
  slug: string
  /** The child's own status, not whether readers see it. */
  status: PublicationStatus
  position: number
}

/** The parent of a Module as the form holds it. Both values are text while the Admin types. */
export interface ParentFields {
  /** Empty until the Admin chooses a Series. */
  seriesId: string
  position: string
}

/** Values the editor lets the Admin change. Optional text is an empty string here. */
export interface LearningFields {
  title: string
  slug: string
  summary: string
  overviewMarkdown: string
  /** Ids of the chosen Topics. Their order carries no meaning. */
  topicIds: string[]
  /** Only a Module has a parent. It is `null` for a Series. */
  parent: ParentFields | null
}

/** Fields that are edited as text. The Topic choice and the parent have their own controls. */
export type LearningTextField = Exclude<keyof LearningFields, 'topicIds' | 'parent'>

export const EMPTY_LEARNING_FIELDS: LearningFields = {
  title: '',
  slug: '',
  summary: '',
  overviewMarkdown: '',
  topicIds: [],
  parent: null,
}

/** Form values of a saved record that has no parent. A Module adds its parent in `moduleFieldsOf`. */
export function learningFieldsOf(record: LearningPayload): LearningFields {
  return {
    title: record.title,
    slug: record.slug,
    summary: record.summary ?? '',
    overviewMarkdown: record.overviewMarkdown ?? '',
    topicIds: [...record.topicIds],
    parent: null,
  }
}

function sameParent(left: ParentFields | null, right: ParentFields | null): boolean {
  if (left === null || right === null) {
    return left === right
  }
  return left.seriesId === right.seriesId && left.position === right.position
}

export function sameLearningFields(left: LearningFields, right: LearningFields): boolean {
  return (
    left.title === right.title &&
    left.slug === right.slug &&
    left.summary === right.summary &&
    left.overviewMarkdown === right.overviewMarkdown &&
    sameIds(left.topicIds, right.topicIds) &&
    sameParent(left.parent, right.parent)
  )
}

/** Where one child stands after a reorder. `revision` is the child's own revision. */
export interface ChildPosition {
  id: string
  position: number
  revision: number
}

/** Result of a reorder. `revision` is the revision of the parent, the same as its new ETag. */
export interface ChildOrder {
  revision: number
  children: ChildPosition[]
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
