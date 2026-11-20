/** Reader route of the News Index. The first page has no page query. */
export function newsIndexPath(page = 0): string {
  return page === 0 ? '/news' : `/news?page=${page}`
}

/** Reader route of one News item. This is the page URL, not the API URL. */
export function newsPath(slug: string): string {
  return `/news/${encodeURIComponent(slug)}`
}
