import { Link } from 'react-router'

import { Pagination } from '../../../shared/ui/Pagination'
import { seriesIndexPath } from '../learningRoutes'
import type { SeriesCollectionPage } from '../model/seriesCollection'
import { SeriesIndexEntry } from './SeriesIndexEntry'

/** Series Index: a short orientation and the public Series, one page at a time. */
export function SeriesIndexView({ collection }: { collection: SeriesCollectionPage }) {
  return (
    <section className="reader-browse" aria-labelledby="series-index-title">
      <title>Series · Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="series-index-title">Series</h1>
        <p className="reader-browse-intro">
          Lộ trình học theo thứ tự. Mỗi Series gồm các Module, mỗi Module gồm các bài viết.
        </p>
      </header>
      <IndexBody collection={collection} />
    </section>
  )
}

function IndexBody({ collection }: { collection: SeriesCollectionPage }) {
  if (collection.items.length === 0) {
    return collection.page === 0 ? (
      <p className="reader-state">Chưa có Series nào được công khai.</p>
    ) : (
      <section className="reader-state">
        <p>Trang này không còn nội dung.</p>
        <Link to={seriesIndexPath()}>Về trang đầu</Link>
      </section>
    )
  }
  return (
    <>
      <ul className="reader-content-list" aria-label="Các Series">
        {collection.items.map((series) => (
          <SeriesIndexEntry key={series.id} series={series} />
        ))}
      </ul>
      <Pagination page={collection.page} hasMore={collection.hasMore} pathOf={seriesIndexPath} />
    </>
  )
}
