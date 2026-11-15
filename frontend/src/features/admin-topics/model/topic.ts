/** One Topic as the Admin API returns it, active or not. */
export interface TopicAdminItem {
  id: string
  slug: string
  name: string
  description: string | null
  active: boolean
  createdAt: string
  updatedAt: string
}
