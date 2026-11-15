/** Reader route of a Topic Home. */
export function topicHomePath(slug: string): string {
  return `/topics/${encodeURIComponent(slug)}`
}
