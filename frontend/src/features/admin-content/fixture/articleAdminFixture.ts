import type { ArticleAdminDetail, PublicationState } from '../model/article'

export const ARTICLE_ID = '0f8fad5b-d9cb-469f-a165-70867728950e'
export const ARTICLES_PATH = '/api/admin/v1/articles'
export const ARTICLE_PATH = `${ARTICLES_PATH}/${ARTICLE_ID}`

export function draftArticle(changes: Partial<ArticleAdminDetail> = {}): ArticleAdminDetail {
  return {
    id: ARTICLE_ID,
    type: 'article',
    title: 'Hiểu Protobuf với Java',
    slug: 'hieu-protobuf',
    summary: 'Tóm tắt bài viết.',
    bodyMarkdown: '## Mở đầu\n\nNội dung bài.',
    moduleId: null,
    position: null,
    topicIds: [],
    status: 'DRAFT',
    revision: 1,
    publishedAt: null,
    unpublishedAt: null,
    createdAt: '2026-11-10T02:00:00Z',
    updatedAt: '2026-11-10T02:00:00Z',
    ...changes,
  }
}

/** Response headers the server sends with an Admin Article. */
export function revisionHeaders(article: ArticleAdminDetail): Record<string, string> {
  return { ETag: `"${article.revision}"` }
}

export function publishPath(action: 'publish' | 'unpublish'): string {
  return `/api/admin/v1/content/${ARTICLE_ID}/${action}`
}

/** Publication response for an Article after a publish or unpublish. */
export function publicationOf(article: ArticleAdminDetail, changes: Partial<PublicationState>): PublicationState {
  return {
    id: article.id,
    type: 'article',
    status: article.status,
    revision: article.revision,
    publishedAt: article.publishedAt,
    unpublishedAt: article.unpublishedAt,
    updatedAt: article.updatedAt,
    ...changes,
  }
}
