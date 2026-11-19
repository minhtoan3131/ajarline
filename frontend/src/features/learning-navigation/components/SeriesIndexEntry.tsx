import { Link } from 'react-router'

import { TopicChips } from '../../topic-browsing/components/TopicChips'
import { seriesPath } from '../learningRoutes'
import type { SeriesSummary } from '../model/seriesCollection'

/** One Series row: title, summary and Topics. No count and no progress. */
export function SeriesIndexEntry({ series }: { series: SeriesSummary }) {
  return (
    <li className="reader-content-entry">
      <h2>
        <Link to={seriesPath(series.slug)}>{series.title}</Link>
      </h2>
      <p className="reader-content-summary">{series.summary}</p>
      <TopicChips topics={series.topics} label={`Chủ đề của ${series.title}`} />
    </li>
  )
}
