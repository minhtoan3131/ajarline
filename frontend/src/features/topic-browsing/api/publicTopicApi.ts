import type { HttpClient } from '../../../shared/api/httpClient'
import type { PublicTopicRef } from '../model/publicTopic'

const TOPICS_PATH = '/api/v1/topics'

interface TopicDiscovery {
  topics: PublicTopicRef[]
}

/** Returns the Topics that have public content, in the order the server chose. */
export async function fetchPublicTopics(client: HttpClient, signal?: AbortSignal): Promise<PublicTopicRef[]> {
  const response = await client.request<TopicDiscovery>(TOPICS_PATH, { signal })
  return response.body.topics
}
