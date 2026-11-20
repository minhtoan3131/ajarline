import { Link } from 'react-router'

import { seriesIndexPath } from '../../learning-navigation/learningRoutes'
import { newsIndexPath } from '../../news-reading/newsRoutes'

/**
 * Where to read next. Every link stays inside Ajarline and opens in the same tab.
 * "/" and "/topics" are written directly, like the Reader header does.
 */
export function AboutNextLinks() {
  return (
    <section className="reader-about-section" aria-labelledby="about-next-title">
      <h2 id="about-next-title">Đi tiếp</h2>
      <ul className="reader-topic-list" aria-label="Các trang để đọc tiếp">
        <li>
          <Link to="/" className="reader-topic-link">
            Trang chủ
          </Link>
        </li>
        <li>
          <Link to="/topics" className="reader-topic-link">
            Chủ đề
          </Link>
        </li>
        <li>
          <Link to={seriesIndexPath()} className="reader-topic-link">
            Series
          </Link>
        </li>
        <li>
          <Link to={newsIndexPath()} className="reader-topic-link">
            Tin công nghệ
          </Link>
        </li>
      </ul>
    </section>
  )
}
