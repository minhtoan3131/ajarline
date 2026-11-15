import { Link } from 'react-router'

import type { PublicTopic, TopicContentItem, TopicContentPage } from '../model/publicTopic'
import { contextTrailOf, groupTopicContent, readerPathOf, type TopicContentGroup } from '../model/topicContentGroups'
import { topicHomePath } from '../topicRoutes'
import { TopicChips } from './TopicChips'

/**
 * Topic Home. It shows only groups that have items on this page and links an
 * item only when the Reader has a page for its type.
 */
export function TopicHomeView({ topic, content }: { topic: PublicTopic; content: TopicContentPage }) {
  const groups = groupTopicContent(content.items)

  return (
    <section className="reader-browse" aria-labelledby="topic-home-title">
      <title>{`${topic.name} · Ajarline`}</title>
      <header className="reader-browse-header">
        <p className="reader-browse-back">
          <Link to="/topics">Tất cả chủ đề</Link>
        </p>
        <h1 id="topic-home-title">{topic.name}</h1>
        {topic.description && <p className="reader-browse-intro">{topic.description}</p>}
      </header>
      {groups.length === 0 ? (
        <section className="reader-state">
          <p>Trang này không còn nội dung.</p>
          <Link to={topicHomePath(topic.slug)}>Về trang đầu</Link>
        </section>
      ) : (
        <>
          {groups.map((group) => (
            <TopicContentGroupView key={group.key} group={group} topicSlug={topic.slug} />
          ))}
          <TopicPagination slug={topic.slug} page={content.page} hasMore={content.hasMore} />
        </>
      )}
    </section>
  )
}

function TopicContentGroupView({ group, topicSlug }: { group: TopicContentGroup; topicSlug: string }) {
  const headingId = `topic-group-${group.key}`
  return (
    <section className="reader-content-group" aria-labelledby={headingId}>
      <h2 id={headingId}>{group.label}</h2>
      <ul className="reader-content-list">
        {group.items.map((item) => (
          <TopicContentEntry key={item.id} item={item} topicSlug={topicSlug} />
        ))}
      </ul>
    </section>
  )
}

function TopicContentEntry({ item, topicSlug }: { item: TopicContentItem; topicSlug: string }) {
  const path = readerPathOf(item)
  const trail = contextTrailOf(item)
  // The Topic being viewed would repeat on every row, so only the others show.
  const otherTopics = item.topics.filter((other) => other.slug !== topicSlug)

  return (
    <li className="reader-content-entry">
      {trail && <p className="reader-content-context">{trail}</p>}
      <h3>{path === null ? item.title : <Link to={path}>{item.title}</Link>}</h3>
      <p className="reader-content-summary">{item.summary}</p>
      <TopicChips topics={otherTopics} label={`Chủ đề khác của ${item.title}`} />
    </li>
  )
}

/** Previous and next by page and hasMore. There is no total, so no "2 of 14". */
function TopicPagination({ slug, page, hasMore }: { slug: string; page: number; hasMore: boolean }) {
  if (page === 0 && !hasMore) {
    return null
  }
  return (
    <nav className="reader-pagination" aria-label="Phân trang">
      {page > 0 && (
        <Link to={topicHomePath(slug, page - 1)} rel="prev" className="reader-pagination-link">
          Trang trước
        </Link>
      )}
      <span className="reader-pagination-current">Trang {page + 1}</span>
      {hasMore && (
        <Link to={topicHomePath(slug, page + 1)} rel="next" className="reader-pagination-link">
          Trang sau
        </Link>
      )}
    </nav>
  )
}
