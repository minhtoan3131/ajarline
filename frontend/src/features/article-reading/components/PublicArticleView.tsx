import { TopicChips } from '../../topic-browsing/components/TopicChips'
import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { formatReaderDate } from '../../../shared/lib/formatReaderDate'
import { EDITORIAL_PROFILE } from '../editorialProfile'
import type { PublicArticle } from '../model/publicArticle'

/**
 * Reading view of one public Article.
 *
 * Only data returned by the public API is shown. A standalone Article has no
 * series, module or previous/next area at all, not even an empty one.
 */
export function PublicArticleView({ article }: { article: PublicArticle }) {
  const published = formatReaderDate(article.publishedAt)
  const updated = formatReaderDate(article.updatedAt)

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
        <TopicChips topics={article.topics} label="Chủ đề của bài viết" />
      </header>
      {article.bodyMarkdown && (
        <div className="reader-article-body">
          <MarkdownRenderer markdown={article.bodyMarkdown} linkBehavior="navigate" />
        </div>
      )}
    </article>
  )
}
