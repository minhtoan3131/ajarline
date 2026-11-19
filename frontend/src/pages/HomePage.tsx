import { Link } from 'react-router'

import { standaloneIndexPath } from '../features/article-browsing/articleRoutes'
import { useStandaloneEntry } from '../features/article-browsing/useStandaloneEntry'
import { seriesIndexPath } from '../features/learning-navigation/learningRoutes'
import { useSeriesEntry } from '../features/learning-navigation/useSeriesEntry'

/**
 * Home. It links to a destination only when that destination has public
 * content, so the page never shows an empty shelf.
 */
export function HomePage() {
  const seriesEntry = useSeriesEntry()
  const standaloneEntry = useStandaloneEntry()

  return (
    <section className="reader-browse" aria-labelledby="home-title">
      <title>Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="home-title">Ajarline</h1>
        <p className="reader-browse-intro">Đọc và tự học công nghệ bằng tiếng Việt.</p>
      </header>
      {(seriesEntry === 'visible' || standaloneEntry === 'visible') && (
        <ul className="reader-topic-list" aria-label="Lối vào">
          {seriesEntry === 'visible' && (
            <li>
              <Link to={seriesIndexPath()} className="reader-topic-link">
                Học theo Series
              </Link>
            </li>
          )}
          {standaloneEntry === 'visible' && (
            <li>
              <Link to={standaloneIndexPath()} className="reader-topic-link">
                Bài viết độc lập
              </Link>
            </li>
          )}
        </ul>
      )}
    </section>
  )
}
