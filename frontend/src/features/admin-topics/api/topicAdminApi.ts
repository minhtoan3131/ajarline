import type { HttpClient } from '../../../shared/api/httpClient'
import type { TopicAdminItem } from '../model/topic'

const TOPICS_PATH = '/api/admin/v1/topics'

interface TopicAdminList {
  topics: TopicAdminItem[]
}

/** Returns every Topic, active or not, in the order the server chose. */
export async function listTopics(client: HttpClient, signal?: AbortSignal): Promise<TopicAdminItem[]> {
  const response = await client.request<TopicAdminList>(TOPICS_PATH, { signal })
  return response.body.topics
}
