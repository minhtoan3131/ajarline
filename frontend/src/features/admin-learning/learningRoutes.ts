import { editorPathOf, newPathOf } from '../admin-content/contentRoutes'

/** Admin route of the empty Series editor. */
export const NEW_SERIES_PATH = newPathOf('series')

/** Admin route of the Series list. The first page has no page query. */
export function seriesListPath(page = 0): string {
  return page === 0 ? '/admin/series' : `/admin/series?page=${page}`
}

export function seriesEditorPath(seriesId: string): string {
  return editorPathOf('series', seriesId)
}

/** Admin route of the Module editor for a new Module. With a Series id, that Series is chosen at the start. */
export function newModulePath(seriesId?: string): string {
  const path = newPathOf('module')
  return seriesId === undefined ? path : `${path}?seriesId=${encodeURIComponent(seriesId)}`
}

export function moduleEditorPath(moduleId: string): string {
  return editorPathOf('module', moduleId)
}
