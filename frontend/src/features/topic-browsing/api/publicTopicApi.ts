import type { HttpClient } from '../../../shared/api/httpClient'
import type { PublicTopic, PublicTopicRef, TopicContentPage } from '../model/publicTopic'

const TOPICS_PATH = '/api/v1/topics'

interface TopicDiscovery {
  topics: PublicTopicRef[]
}

/** Returns the Topics that have public content, in the order the server chose. */
export async function fetchPublicTopics(client: HttpClient, signal?: AbortSignal): Promise<PublicTopicRef[]> {
  const response = await client.request<TopicDiscovery>(TOPICS_PATH, { signal })
  return response.body.topics
}

/** Items per Topic Home page. The server accepts 1 to 50. */
export const TOPIC_CONTENT_PAGE_SIZE = 20

function topicPath(slug: string): string {
  return `${TOPICS_PATH}/${encodeURIComponent(slug)}`
}

/**
 * Reads one public Topic.
 *
 * @throws ApiError with status 404 when the Topic is missing or has no public content
 */
export async function fetchPublicTopic(client: HttpClient, slug: string, signal?: AbortSignal): Promise<PublicTopic> {
  const response = await client.request<PublicTopic>(topicPath(slug), { signal })
  return response.body
}

/**
 * Reads one page of the public content directly assigned to a Topic.
 *
 * @throws ApiError with status 404 when the Topic is missing or has no public content
 */
export async function fetchTopicContent(
  client: HttpClient,
  slug: string,
  page: number,
  signal?: AbortSignal,
): Promise<TopicContentPage> {
  const query = new URLSearchParams({ page: String(page), size: String(TOPIC_CONTENT_PAGE_SIZE) })
  const response = await client.request<TopicContentPage>(`${topicPath(slug)}/content?${query.toString()}`, { signal })
  return response.body
}
