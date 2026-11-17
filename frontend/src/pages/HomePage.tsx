import { Link } from 'react-router'

import { standaloneIndexPath } from '../features/article-browsing/articleRoutes'
import { useStandaloneEntry } from '../features/article-browsing/useStandaloneEntry'

/**
 * Home. It links to a destination only when that destination has public
 * content, so the page never shows an empty shelf.
 */
export function HomePage() {
  const standaloneEntry = useStandaloneEntry()

  return (
    <section className="reader-browse" aria-labelledby="home-title">
      <title>Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="home-title">Ajarline</h1>
        <p className="reader-browse-intro">Đọc và tự học công nghệ bằng tiếng Việt.</p>
      </header>
      {standaloneEntry === 'visible' && (
        <ul className="reader-topic-list" aria-label="Lối vào">
          <li>
            <Link to={standaloneIndexPath()} className="reader-topic-link">
              Bài viết độc lập
            </Link>
          </li>
        </ul>
      )}
    </section>
  )
}
