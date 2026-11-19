import { Link } from 'react-router'

import { articlePath } from '../../article-reading/articleRoutes'

interface SequenceLink {
  slug: string
  title: string
}

/** Links to the previous and next Article of the same Module. A missing side is left out. */
export function ArticleSequenceNav({ previous, next }: { previous: SequenceLink | null; next: SequenceLink | null }) {
  if (previous === null && next === null) {
    return null
  }
  return (
    <nav className="reader-sequence" aria-label="Bài trước và bài sau">
      {previous !== null && (
        <Link to={articlePath(previous.slug)} rel="prev" className="reader-sequence-link">
          <span className="reader-sequence-label">Bài trước</span> <span>{previous.title}</span>
        </Link>
      )}
      {next !== null && (
        <Link to={articlePath(next.slug)} rel="next" className="reader-sequence-link reader-sequence-next">
          <span className="reader-sequence-label">Bài sau</span> <span>{next.title}</span>
        </Link>
      )}
    </nav>
  )
}
