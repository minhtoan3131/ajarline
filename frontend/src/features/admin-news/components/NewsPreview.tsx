import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import { safeLinkHref } from '../../content-rendering/urlPolicy'
import { localInputToIso, type NewsFields } from '../model/news'

/**
 * Renders the current, possibly unsaved, editor values.
 * It reads values only; it never saves, publishes or changes editor state.
 * Links keep the editor in place so unsaved text is never lost.
 */
export function NewsPreview({ fields }: { fields: NewsFields }) {
  return (
    <div className="article-preview">
      {fields.title !== '' && <h2 className="article-preview-title">{fields.title}</h2>}
      {fields.summary !== '' && <p className="article-preview-summary">{fields.summary}</p>}
      <SourcePreview name={fields.sourceName} url={fields.sourceUrl} publishedAt={fields.sourcePublishedAt} />
      <MarkdownRenderer markdown={fields.bodyMarkdown} linkBehavior="preview" />
    </div>
  )
}

interface SourcePreviewProps {
  name: string
  url: string
  publishedAt: string
}

function SourcePreview({ name, url, publishedAt }: SourcePreviewProps) {
  if (name === '' && url === '' && publishedAt === '') {
    return null
  }
  const iso = localInputToIso(publishedAt)
  // The URL is shown as text. A link would leave the editor and lose unsaved text.
  const hidden = url !== '' && safeLinkHref(url) === null
  return (
    <section className="news-preview-source" aria-label="Nguồn tin trong xem trước">
      <p>Nguồn: {name === '' ? 'chưa có tên nguồn' : name}</p>
      {url !== '' && <p className="news-preview-url">{url}</p>}
      {hidden && <p className="news-preview-warning">Người đọc sẽ không thấy link này.</p>}
      {iso !== null && (
        <p>
          Đăng ngày{' '}
          <time dateTime={iso}>{new Date(iso).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })}</time>
        </p>
      )}
    </section>
  )
}
