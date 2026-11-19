/** Reader route of a Topic Home. The first page has no page query. */
export function topicHomePath(slug: string, page = 0): string {
  const path = `/topics/${encodeURIComponent(slug)}`
  return page === 0 ? path : `${path}?page=${page}`
}
