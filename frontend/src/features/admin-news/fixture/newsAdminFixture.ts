import type { PublicationState } from '../../admin-content/model/article'
import { givenSignedInAdmin } from '../../admin-auth/fixture/adminSessionFixture'
import { givenTopicCatalog } from '../../admin-topics/fixture/topicAdminFixture'
import type { StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { NewsAdminDetail } from '../model/news'
import { vi } from 'vitest'

export const NEWS_ID = '6e4b2c90-1d7a-4f35-8b02-9c3a5d7e1f48'
export const NEWS_API_PATH = '/api/admin/v1/news'
export const NEWS_PATH = `${NEWS_API_PATH}/${NEWS_ID}`

export function draftNews(changes: Partial<NewsAdminDetail> = {}): NewsAdminDetail {
  return {
    id: NEWS_ID,
    type: 'news',
    title: 'Spring Boot 4.1 ra mắt',
    slug: 'spring-boot-4-1-ra-mat',
    summary: 'Bản phát hành mới của Spring Boot.',
    bodyMarkdown: '## Điểm mới\n\nNội dung tin.',
    source: { name: 'Spring Blog', url: 'https://spring.io/blog/spring-boot-4-1', publishedAt: '2026-11-18T02:00:00Z' },
    topicIds: [],
    status: 'DRAFT',
    revision: 1,
    publishedAt: null,
    unpublishedAt: null,
    createdAt: '2026-11-19T02:00:00Z',
    updatedAt: '2026-11-19T02:00:00Z',
    ...changes,
  }
}

/** Response headers the server sends with an Admin News item. */
export function newsHeaders(news: NewsAdminDetail): Record<string, string> {
  return { ETag: `"${news.revision}"` }
}

export function newsPublishPath(action: 'publish' | 'unpublish'): string {
  return `/api/admin/v1/content/${NEWS_ID}/${action}`
}

/** Publication response for a News item after a publish or unpublish. */
export function publicationOfNews(news: NewsAdminDetail, changes: Partial<PublicationState>): PublicationState {
  return {
    id: news.id,
    type: 'news',
    status: news.status,
    revision: news.revision,
    publishedAt: news.publishedAt,
    unpublishedAt: news.unpublishedAt,
    updatedAt: news.updatedAt,
    ...changes,
  }
}

/** Session plus the Topic list that every News editor loads for its Topic picker. */
export function givenNewsEditorSession(api: StubApiServer): StubApiServer {
  return givenTopicCatalog(givenSignedInAdmin(api))
}

/** Holds back every request with this method until the returned function is called. Install the stub first. */
export function holdRequests(method: string): () => void {
  const respond = globalThis.fetch
  let release: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    if ((init?.method ?? 'GET') === method) {
      await gate
    }
    return respond(input, init)
  })
  return release
}
