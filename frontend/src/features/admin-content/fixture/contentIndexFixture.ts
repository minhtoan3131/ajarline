import { vi } from 'vitest'

import { jsonResponse, type StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { AdminContentItem, AdminContentType } from '../model/contentIndex'

export const CONTENT_INDEX_API = '/api/admin/v1/content'

export const ITEM_IDS: Record<AdminContentType, string> = {
  article: '0f8fad5b-d9cb-469f-a165-70867728950e',
  module: 'c8a2f6d1-3e57-4b90-a1d4-7f0e5b2c9a36',
  series: '5b1d7e3a-9c24-4f08-8a6e-2d4c0b9f1e73',
  news: '6e4b2c90-1d7a-4f35-8b02-9c3a5d7e1f48',
}

export const OTHER_ITEM_ID = '2c7e9a41-5d38-4b16-9f0a-8e1b3d5f7a62'

/** A Draft News item. Change only what a test cares about. */
export function contentItem(changes: Partial<AdminContentItem> = {}): AdminContentItem {
  return {
    id: ITEM_IDS.news,
    type: 'news',
    slug: 'spring-boot-4-1-ra-mat',
    title: 'Spring Boot 4.1 ra mắt',
    status: 'DRAFT',
    revision: 1,
    publishedAt: null,
    updatedAt: '2026-11-19T02:00:00Z',
    ...changes,
  }
}

/** Request path of the Index. `filters` is the part before `page`, for example `types=news&q=x`. */
export function contentIndexApiPath(filters = '', page = 0): string {
  const prefix = filters === '' ? '' : `${filters}&`
  return `${CONTENT_INDEX_API}?${prefix}page=${page}&size=20`
}

/** Body of the Index endpoint. */
export function contentIndexResponse(items: AdminContentItem[], options: { page?: number; hasMore?: boolean } = {}) {
  return { page: options.page ?? 0, size: 20, hasMore: options.hasMore ?? false, items }
}

/** Stubs one Index page. A second call for the same path queues a later answer. */
export function givenContentIndex(
  api: StubApiServer,
  items: AdminContentItem[],
  options: { filters?: string; page?: number; hasMore?: boolean } = {},
): StubApiServer {
  const page = options.page ?? 0
  return api.on(
    'GET',
    contentIndexApiPath(options.filters, page),
    jsonResponse(200, contentIndexResponse(items, { page, hasMore: options.hasMore })),
  )
}

/**
 * Holds back the requests with this method until the returned function is
 * called. With a path, only the requests to exactly that path are held.
 * Install the stub first.
 */
export function holdRequestsTo(method: string, path?: string): () => void {
  const respond = globalThis.fetch
  let release: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    const held = (init?.method ?? 'GET') === method && (path === undefined || String(input) === path)
    if (held) {
      await gate
    }
    return respond(input, init)
  })
  return release
}
