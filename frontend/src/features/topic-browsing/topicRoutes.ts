/** Reader route of a Topic Home. The first page has no page query. */
export function topicHomePath(slug: string, page = 0): string {
  const path = `/topics/${encodeURIComponent(slug)}`
  return page === 0 ? path : `${path}?page=${page}`
}

// At most nine digits, so the value always fits the server's 32-bit page.
const PAGE_QUERY = /^(0|[1-9]\d{0,8})$/

/**
 * Page index from the `page` query value. A missing or malformed value opens
 * the first page, because it comes from a hand-edited or outdated link.
 */
export function pageFromQuery(value: string | null): number {
  return value !== null && PAGE_QUERY.test(value) ? Number(value) : 0
}
