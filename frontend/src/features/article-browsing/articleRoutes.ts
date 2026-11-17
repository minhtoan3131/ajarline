/** Reader route of the standalone Article Index. The first page has no page query. */
export function standaloneIndexPath(page = 0): string {
  const path = '/articles?standalone=true'
  return page === 0 ? path : `${path}&page=${page}`
}

/**
 * True when the URL query names the standalone Index. Any other value, or no
 * value, is a link the page should redirect to the canonical URL.
 */
export function isStandaloneIndexQuery(params: URLSearchParams): boolean {
  return params.get('standalone') === 'true'
}
