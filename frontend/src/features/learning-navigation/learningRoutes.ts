/** Reader route of the Series Index. The first page has no page query. */
export function seriesIndexPath(page = 0): string {
  return page === 0 ? '/series' : `/series?page=${page}`
}

/** Reader route of one Series. This is the page URL, not the API URL. */
export function seriesPath(slug: string): string {
  return `/series/${encodeURIComponent(slug)}`
}

/** Reader route of one Module. */
export function modulePath(slug: string): string {
  return `/modules/${encodeURIComponent(slug)}`
}
