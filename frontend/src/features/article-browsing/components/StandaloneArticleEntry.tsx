import { Link } from 'react-router'

import { articlePath } from '../../article-reading/articleRoutes'
import { TopicChips } from '../../topic-browsing/components/TopicChips'
import type { ArticleSummary } from '../model/articleCollection'

/** One standalone Article row: title, summary and Topics. No order number, no hierarchy. */
export function StandaloneArticleEntry({ article }: { article: ArticleSummary }) {
  return (
    <li className="reader-content-entry">
      <h2>
        <Link to={articlePath(article.slug)}>{article.title}</Link>
      </h2>
      <p className="reader-content-summary">{article.summary}</p>
      <TopicChips topics={article.topics} label={`Chủ đề của ${article.title}`} />
    </li>
  )
}
