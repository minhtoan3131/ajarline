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

/** Text and switch values of the Topic form. */
export interface TopicFields {
  slug: string
  name: string
  description: string
  active: boolean
}

/** A new Topic starts active, so it can be assigned right away. */
export const EMPTY_TOPIC_FIELDS: TopicFields = { slug: '', name: '', description: '', active: true }

/** Full request body of create. Every field is required; description may be null. */
export interface TopicCreatable {
  slug: string
  name: string
  description: string | null
  active: boolean
}

/**
 * Builds the create body. An empty description is sent as null, which is how
 * the server stores it. Slug and name go as typed; the server normalizes them.
 */
export function toCreatable(fields: TopicFields): TopicCreatable {
  return {
    slug: fields.slug,
    name: fields.name,
    description: fields.description === '' ? null : fields.description,
    active: fields.active,
  }
}

export function sameTopicFields(left: TopicFields, right: TopicFields): boolean {
  return (
    left.slug === right.slug &&
    left.name === right.name &&
    left.description === right.description &&
    left.active === right.active
  )
}
