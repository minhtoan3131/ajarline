/** Admin route of the empty News editor. */
export const NEW_NEWS_PATH = '/admin/news/new'

/** Admin route of the News list. The first page has no page query. */
export function newsListPath(page = 0): string {
  return page === 0 ? '/admin/news' : `/admin/news?page=${page}`
}

export function newsEditorPath(newsId: string): string {
  return `/admin/news/${newsId}`
}
