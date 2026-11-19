import { Link } from 'react-router'

import type { ArticleLearningContext } from '../model/articleLearningContext'

/** Where this Article sits in the learning path. It shows ranks, never a total. */
export function ArticleLearningTrail({ context }: { context: ArticleLearningContext }) {
  const rank = [context.module.position === null ? null : `Module ${context.module.position}`, `Bài ${context.ownRank}`]
    .filter((part): part is string => part !== null)
    .join(' · ')

  return (
    <nav className="reader-learning-trail" aria-label="Vị trí trong lộ trình học">
      <ol className="reader-learning-crumbs">
        {context.series !== null && (
          <li>
            <Link to={context.series.path}>{context.series.title}</Link>
          </li>
        )}
        <li>
          <Link to={context.module.path}>{context.module.title}</Link>
        </li>
      </ol>
      <p className="reader-learning-rank">{rank}</p>
    </nav>
  )
}
