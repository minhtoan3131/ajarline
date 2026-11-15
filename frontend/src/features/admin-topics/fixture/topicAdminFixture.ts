import { jsonResponse, type StubApiServer } from '../../../shared/api/testsupport/StubApiServer'
import type { TopicAdminItem } from '../model/topic'

export const TOPICS_PATH = '/api/admin/v1/topics'
export const TOPIC_ID = '7a3f0c1e-5b2d-4c8e-9a1f-3d6e8b2c4f10'
export const OTHER_TOPIC_ID = 'c2d9e4a7-1b35-4f68-8e0a-5d7c9b1a3e24'

export function topicPath(topicId: string): string {
  return `${TOPICS_PATH}/${topicId}`
}

export function activeTopic(changes: Partial<TopicAdminItem> = {}): TopicAdminItem {
  return {
    id: TOPIC_ID,
    slug: 'java',
    name: 'Java',
    description: 'Ngôn ngữ và nền tảng Java.',
    active: true,
    createdAt: '2026-11-10T02:00:00Z',
    updatedAt: '2026-11-10T02:00:00Z',
    ...changes,
  }
}

/** A Topic that no longer accepts new assignments. It has no description. */
export function inactiveTopic(changes: Partial<TopicAdminItem> = {}): TopicAdminItem {
  return activeTopic({ id: OTHER_TOPIC_ID, slug: 'spring', name: 'Spring', description: null, active: false, ...changes })
}

/** Body of the list endpoint. */
export function topicList(...topics: TopicAdminItem[]) {
  return { topics }
}

/** Stubs the Topic list that the Article editor loads for its Topic picker. */
export function givenTopicCatalog(api: StubApiServer, ...topics: TopicAdminItem[]): StubApiServer {
  return api.on('GET', TOPICS_PATH, jsonResponse(200, topicList(...topics)))
}
