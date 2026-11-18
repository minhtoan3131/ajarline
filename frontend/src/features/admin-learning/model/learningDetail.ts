import { learningFieldsOf, type LearningChild, type LearningFields } from './learning'
import { moduleFieldsOf, type ModuleAdminDetail } from './module'
import type { SeriesAdminDetail } from './series'

/** A Series or a Module as the Admin API returns it. `type` tells which. */
export type LearningDetail = SeriesAdminDetail | ModuleAdminDetail

/** Form values of a saved record. Only a Module has a parent. */
export function fieldsOfDetail(detail: LearningDetail): LearningFields {
  return detail.type === 'series' ? learningFieldsOf(detail) : moduleFieldsOf(detail)
}

/** The ordered children: Modules of a Series, Articles of a Module. */
export function childrenOf(detail: LearningDetail): LearningChild[] {
  return detail.type === 'series' ? detail.modules : detail.articles
}

/** The same record with another list of children. */
export function withChildren(detail: LearningDetail, children: LearningChild[]): LearningDetail {
  return detail.type === 'series' ? { ...detail, modules: children } : { ...detail, articles: children }
}
