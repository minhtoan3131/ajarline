import { Link } from 'react-router'

import type { PublicTopicRef } from '../model/publicTopic'
import { topicHomePath } from '../topicRoutes'

/**
 * Topic Index. It lists only Topics that have public content and shows no
 * counts, so a small library never looks bigger or emptier than it is.
 */
export function TopicIndexView({ topics }: { topics: PublicTopicRef[] }) {
  return (
    <section className="reader-browse" aria-labelledby="topic-index-title">
      <title>Chủ đề · Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="topic-index-title">Chủ đề</h1>
        <p className="reader-browse-intro">
          Mỗi chủ đề gom những nội dung được gắn cùng một nhãn. Chủ đề không phải lộ trình học: mở một chủ đề để
          xem các nội dung liên quan.
        </p>
      </header>
      {topics.length === 0 ? (
        <p className="reader-state">Chưa có chủ đề nào có nội dung công khai.</p>
      ) : (
        <ul className="reader-topic-list" aria-label="Các chủ đề">
          {topics.map((topic) => (
            <li key={topic.id}>
              <Link to={topicHomePath(topic.slug)} className="reader-topic-link">
                {topic.name}
              </Link>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
