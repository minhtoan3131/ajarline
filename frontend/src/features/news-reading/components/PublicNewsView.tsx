import { formatReaderDate } from '../../../shared/lib/formatReaderDate'
import { EDITORIAL_PROFILE } from '../../../shared/content/editorialProfile'
import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { TopicChips } from '../../topic-browsing/components/TopicChips'
import type { PublicNews } from '../model/publicNews'
import { NewsSourceBlock } from './NewsSourceBlock'
import { NewsTypeLabel } from './NewsTypeLabel'

/**
 * Reading view of one public News item.
 *
 * The header says who is responsible for the content on Ajarline and only
 * names the source. The one link to the source is in the source block after
 * the body. A News item has no series, module or previous/next area.
 */
export function PublicNewsView({ news }: { news: PublicNews }) {
  const published = formatReaderDate(news.publishedAt)
  const updated = formatReaderDate(news.updatedAt)

  return (
    <article className="reader-article">
      <title>{`${news.title} · Ajarline`}</title>
      <header className="reader-article-header">
        <p className="reader-news-label">
          <NewsTypeLabel />
        </p>
        <h1>{news.title}</h1>
        <p className="reader-article-summary">{news.summary}</p>
        <p className="reader-article-meta">
          Xuất bản <time dateTime={news.publishedAt}>{published}</time>
          {updated !== published && (
            <>
              {' · Cập nhật '}
              <time dateTime={news.updatedAt}>{updated}</time>
            </>
          )}
        </p>
        <p className="reader-article-meta">Nguồn: {news.source.name}</p>
        <p className="reader-attribution">
          {EDITORIAL_PROFILE.responsibilityLabel}: <strong>{EDITORIAL_PROFILE.displayName}</strong>
        </p>
        <TopicChips topics={news.topics} label="Chủ đề của tin" />
      </header>
      <div className="reader-article-body">
        <MarkdownRenderer markdown={news.bodyMarkdown} linkBehavior="navigate" />
      </div>
      <NewsSourceBlock source={news.source} />
    </article>
  )
}
