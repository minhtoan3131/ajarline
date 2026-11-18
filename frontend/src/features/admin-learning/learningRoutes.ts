/** Admin route of the empty Series editor. */
export const NEW_SERIES_PATH = '/admin/series/new'

/** Admin route of the Series list. The first page has no page query. */
export function seriesListPath(page = 0): string {
  return page === 0 ? '/admin/series' : `/admin/series?page=${page}`
}

export function seriesEditorPath(seriesId: string): string {
  return `/admin/series/${seriesId}`
}
