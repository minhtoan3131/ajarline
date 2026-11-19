/** Reader route of one Article. This is the page URL, not the API URL. */
export function articlePath(slug: string): string {
  return `/articles/${encodeURIComponent(slug)}`
}
