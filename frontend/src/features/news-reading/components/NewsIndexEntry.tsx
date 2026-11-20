import { Link } from 'react-router'

import { formatReaderDate } from '../../../shared/lib/formatReaderDate'
import { TopicChips } from '../../topic-browsing/components/TopicChips'
import type { NewsSummary } from '../model/newsCollection'
import { newsPath } from '../newsRoutes'
import { NewsTypeLabel } from './NewsTypeLabel'

/** One News row: type label, title, summary, publish date and Topics. No source, no badge. */
export function NewsIndexEntry({ news }: { news: NewsSummary }) {
  return (
    <li className="reader-content-entry">
      <p className="reader-content-context">
        <NewsTypeLabel />
      </p>
      <h2>
        <Link to={newsPath(news.slug)}>{news.title}</Link>
      </h2>
      <p className="reader-content-summary">{news.summary}</p>
      <p className="reader-content-meta">
        Xuất bản <time dateTime={news.publishedAt}>{formatReaderDate(news.publishedAt)}</time>
      </p>
      <TopicChips topics={news.topics} label={`Chủ đề của ${news.title}`} />
    </li>
  )
}
