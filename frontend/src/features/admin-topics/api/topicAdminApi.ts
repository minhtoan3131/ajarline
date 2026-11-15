import type { HttpClient } from '../../../shared/api/httpClient'
import type { TopicAdminItem, TopicCreatable, TopicEditable } from '../model/topic'

const TOPICS_PATH = '/api/admin/v1/topics'

interface TopicAdminList {
  topics: TopicAdminItem[]
}

/** Returns every Topic, active or not, in the order the server chose. */
export async function listTopics(client: HttpClient, signal?: AbortSignal): Promise<TopicAdminItem[]> {
  const response = await client.request<TopicAdminList>(TOPICS_PATH, { signal })
  return response.body.topics
}

/**
 * Creates a Topic. The server answers 201 with the stored Topic.
 *
 * @throws ApiError 409 SLUG_CONFLICT when the slug is taken, 422 VALIDATION_FAILED
 */
export async function createTopic(client: HttpClient, creatable: TopicCreatable): Promise<TopicAdminItem> {
  const response = await client.request<TopicAdminItem>(TOPICS_PATH, { method: 'POST', body: creatable })
  return response.body
}

function topicPath(topicId: string): string {
  return `${TOPICS_PATH}/${encodeURIComponent(topicId)}`
}

/**
 * Replaces the editable values of a Topic. The last save wins; Topics have no ETag.
 *
 * @throws ApiError 404 TOPIC_NOT_FOUND, 422 VALIDATION_FAILED
 */
export async function updateTopic(client: HttpClient, topicId: string, editable: TopicEditable): Promise<TopicAdminItem> {
  const response = await client.request<TopicAdminItem>(topicPath(topicId), { method: 'PUT', body: editable })
  return response.body
}
