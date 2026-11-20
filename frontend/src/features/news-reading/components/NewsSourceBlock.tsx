import { formatReaderDate } from '../../../shared/lib/formatReaderDate'
import { ExternalLink } from '../../content-rendering/ExternalLink'
import { safeLinkHref } from '../../content-rendering/urlPolicy'
import type { NewsSource } from '../model/publicNews'

/**
 * Where a News item came from. The source name is a link only when its URL
 * passes the URL policy. A URL that does not pass is neither linked nor shown.
 */
export function NewsSourceBlock({ source }: { source: NewsSource }) {
  const href = safeLinkHref(source.url)

  return (
    <section className="reader-news-source" aria-labelledby="news-source-title">
      <h2 id="news-source-title">Nguồn gốc</h2>
      {href === null ? (
        <p className="reader-news-source-name">{source.name}</p>
      ) : (
        <>
          <p className="reader-news-source-name">
            <ExternalLink href={href}>{source.name}</ExternalLink>
          </p>
          <p className="reader-news-source-url">{href}</p>
        </>
      )}
      {source.publishedAt !== null && (
        <p className="reader-article-meta">
          Nguồn đăng <time dateTime={source.publishedAt}>{formatReaderDate(source.publishedAt)}</time>
        </p>
      )}
    </section>
  )
}
