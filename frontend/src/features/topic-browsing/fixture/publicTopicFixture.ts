import type { PublicTopicRef } from '../model/publicTopic'

export const TOPICS_PATH = '/api/v1/topics'

export const JAVA_TOPIC_REF: PublicTopicRef = {
  id: '3b1e7c52-8f4a-4d2b-9c61-0a5e2f7d8b94',
  slug: 'java',
  name: 'Java',
}

export const SPRING_TOPIC_REF: PublicTopicRef = {
  id: 'e6c0a9d1-24b7-4f3e-8a15-7d92c4b0f638',
  slug: 'spring-boot',
  name: 'Spring Boot',
}

/** Body of the public Topic list. */
export function topicDiscovery(...topics: PublicTopicRef[]): { topics: PublicTopicRef[] } {
  return { topics }
}
