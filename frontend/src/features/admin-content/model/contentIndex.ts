import type { PublicationStatus } from './article'

/** The four kinds of content that the Admin API lists. */
export type AdminContentType = 'article' | 'module' | 'series' | 'news'

/** Every type, in the order that the URL and the screen use. */
export const ADMIN_CONTENT_TYPES: readonly AdminContentType[] = ['article', 'module', 'series', 'news']

/** Every status, in the order that the URL and the screen use. */
export const PUBLICATION_STATUSES: readonly PublicationStatus[] = ['DRAFT', 'PUBLISHED', 'UNPUBLISHED']

export const CONTENT_TYPE_LABELS: Record<AdminContentType, string> = {
  article: 'Bài viết',
  module: 'Module',
  series: 'Series',
  news: 'Tin',
}

/** One row of the Content Index. */
export interface AdminContentItem {
  id: string
  type: AdminContentType
  slug: string
  title: string
  status: PublicationStatus
  /** Version of the item. The screen never shows it; a delete sends it back. */
  revision: number
  publishedAt: string | null
  updatedAt: string
}

/** One page of the Content Index. There is no total, only `hasMore`. */
export interface AdminContentIndex {
  page: number
  size: number
  hasMore: boolean
  items: AdminContentItem[]
}
