import { Link } from 'react-router'

import { Pagination } from '../../../shared/ui/Pagination'
import { standaloneIndexPath } from '../articleRoutes'
import type { ArticleCollectionPage } from '../model/articleCollection'
import { StandaloneArticleEntry } from './StandaloneArticleEntry'

/**
 * Standalone Article Index. It shows no counts and no sequence: each Article
 * stands alone.
 */
export function StandaloneIndexView({ collection }: { collection: ArticleCollectionPage }) {
  return (
    <section className="reader-browse" aria-labelledby="standalone-index-title">
      <title>Bài viết độc lập · Ajarline</title>
      <header className="reader-browse-header">
        <h1 id="standalone-index-title">Bài viết độc lập</h1>
        <p className="reader-browse-intro">Những bài đọc riêng lẻ. Mỗi bài đứng một mình, không theo trình tự nào.</p>
      </header>
      <IndexBody collection={collection} />
    </section>
  )
}

function IndexBody({ collection }: { collection: ArticleCollectionPage }) {
  if (collection.items.length === 0) {
    return collection.page === 0 ? (
      <p className="reader-state">Chưa có bài viết độc lập nào được công khai.</p>
    ) : (
      <section className="reader-state">
        <p>Trang này không còn nội dung.</p>
        <Link to={standaloneIndexPath()}>Về trang đầu</Link>
      </section>
    )
  }
  return (
    <>
      <ul className="reader-content-list" aria-label="Các bài viết độc lập">
        {collection.items.map((article) => (
          <StandaloneArticleEntry key={article.id} article={article} />
        ))}
      </ul>
      <Pagination page={collection.page} hasMore={collection.hasMore} pathOf={standaloneIndexPath} />
    </>
  )
}
