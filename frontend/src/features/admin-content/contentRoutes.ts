import type { AdminContentType } from './model/contentIndex'
import { contentIndexSearch, type ContentIndexQuery } from './model/contentIndexQuery'

/** Admin route of the Content Index. */
export const CONTENT_INDEX_PATH = '/admin/content'

/** Admin route of the Content Index with this query. */
export function contentIndexPath(query: ContentIndexQuery): string {
  return CONTENT_INDEX_PATH + contentIndexSearch(query)
}

const SEGMENTS: Record<AdminContentType, string> = {
  article: 'articles',
  module: 'modules',
  series: 'series',
  news: 'news',
}

/** Admin route of the editor of one item. */
export function editorPathOf(type: AdminContentType, id: string): string {
  return `/admin/${SEGMENTS[type]}/${id}`
}

/** Admin route of the empty editor of one type. */
export function newPathOf(type: AdminContentType): string {
  return `/admin/${SEGMENTS[type]}/new`
}
