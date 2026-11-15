import { Link } from 'react-router'

import type { PublicTopicRef } from '../model/publicTopic'
import { topicHomePath } from '../topicRoutes'

/**
 * Topic labels that lead to their Topic Home, never to Search.
 * Renders nothing for an empty list, so no empty area is left behind.
 */
export function TopicChips({ topics, label }: { topics: PublicTopicRef[]; label: string }) {
  if (topics.length === 0) {
    return null
  }
  return (
    <ul className="reader-topic-chips" aria-label={label}>
      {topics.map((topic) => (
        <li key={topic.id}>
          <Link to={topicHomePath(topic.slug)} className="reader-topic-chip">
            {topic.name}
          </Link>
        </li>
      ))}
    </ul>
  )
}
