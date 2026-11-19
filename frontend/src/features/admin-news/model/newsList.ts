import type { PublicationStatus } from '../../admin-content/model/article'

/** One row of the Admin News list. Only the values the list shows. */
export interface AdminNewsItem {
  id: string
  slug: string
  title: string
  status: PublicationStatus
}

/** One page of the Admin News list. There is no total, only `hasMore`. */
export interface AdminNewsList {
  page: number
  hasMore: boolean
  items: AdminNewsItem[]
}
