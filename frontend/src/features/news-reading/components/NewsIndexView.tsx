import { Link } from 'react-router'

import { Pagination } from '../../../shared/ui/Pagination'
import type { NewsCollectionPage } from '../model/newsCollection'
import { newsIndexPath } from '../newsRoutes'
import { NewsIndexEntry } from './NewsIndexEntry'

/** News Index: a short orientation and the public News, one page at a time. */
export function NewsIndexView({ collection }: { collection: NewsCollectionPage }) {
  return (
    <section className="reader-browse" aria-labelledby="news-index-title">
      <title>Tin công nghệ · Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="news-index-title">Tin công nghệ</h1>
        <p className="reader-browse-intro">Tin công nghệ kèm nguồn gốc để bạn tự kiểm chứng.</p>
      </header>
      <IndexBody collection={collection} />
    </section>
  )
}

function IndexBody({ collection }: { collection: NewsCollectionPage }) {
  if (collection.items.length === 0) {
    return collection.page === 0 ? (
      <p className="reader-state">Chưa có tin nào được công khai.</p>
    ) : (
      <section className="reader-state">
        <p>Trang này không còn nội dung.</p>
        <Link to={newsIndexPath()}>Về trang đầu</Link>
      </section>
    )
  }
  return (
    <>
      <ul className="reader-content-list" aria-label="Các tin">
        {collection.items.map((news) => (
          <NewsIndexEntry key={news.id} news={news} />
        ))}
      </ul>
      <Pagination page={collection.page} hasMore={collection.hasMore} pathOf={newsIndexPath} />
    </>
  )
}
