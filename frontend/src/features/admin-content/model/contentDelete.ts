import type { PublicationStatus } from './article'
import type { AdminContentType } from './contentIndex'

/** What a delete needs: which item, its name for the question, and the version the Admin sees. */
export interface DeleteTarget {
  type: AdminContentType
  id: string
  title: string
  etag: string
}

/**
 * True when the screen offers to delete an item with this status. Only a
 * Draft was never published. The server still decides: it can say no for a
 * Draft too, for example when a Series still has a Module.
 */
export function canDeleteContent(status: PublicationStatus): boolean {
  return status === 'DRAFT'
}

/** ETag text of a revision number, as the server sends it. */
export function etagOfRevision(revision: number): string {
  return `"${revision}"`
}
