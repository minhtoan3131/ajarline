import { editorPathOf, newPathOf } from '../admin-content/contentRoutes'

/** Admin route of the empty News editor. */
export const NEW_NEWS_PATH = newPathOf('news')

export function newsEditorPath(newsId: string): string {
  return editorPathOf('news', newsId)
}
