import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { EDITORIAL_PROFILE } from '../editorialProfile'
import type { PublicArticle } from '../model/publicArticle'

const DATE_FORMAT = new Intl.DateTimeFormat('vi-VN', {
  dateStyle: 'long',
  // Ajarline is a Vietnamese site; dates must not change with the reader's
  // device time zone.
  timeZone: 'Asia/Ho_Chi_Minh',
})

/**
 * Reading view of one public Article.
 *
 * Only data returned by the public API is shown. A standalone Article has no
 * series, module or previous/next area at all, not even an empty one.
 */
export function PublicArticleView({ article }: { article: PublicArticle }) {
  const published = DATE_FORMAT.format(new Date(article.publishedAt))
  const updated = DATE_FORMAT.format(new Date(article.updatedAt))

  return (
    <article className="reader-article">
      <title>{`${article.title} · Ajarline`}</title>
      <header className="reader-article-header">
        <h1>{article.title}</h1>
        {article.summary && <p className="reader-article-summary">{article.summary}</p>}
        <p className="reader-article-meta">
          Xuất bản <time dateTime={article.publishedAt}>{published}</time>
          {updated !== published && (
            <>
              {' · Cập nhật '}
              <time dateTime={article.updatedAt}>{updated}</time>
            </>
          )}
        </p>
        <p className="reader-attribution">
          {EDITORIAL_PROFILE.responsibilityLabel}: <strong>{EDITORIAL_PROFILE.displayName}</strong>
        </p>
      </header>
      {article.bodyMarkdown && (
        <div className="reader-article-body">
          <MarkdownRenderer markdown={article.bodyMarkdown} linkBehavior="navigate" />
        </div>
      )}
    </article>
  )
}
