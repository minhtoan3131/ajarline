import { MarkdownRenderer } from '../../content-rendering/MarkdownRenderer'
import type { ArticleFields } from '../model/article'

/**
 * Renders the current, possibly unsaved, editor values.
 * It reads values only; it never saves, publishes or changes editor state.
 */
export function ArticlePreview({ fields }: { fields: ArticleFields }) {
  return (
    <div className="article-preview">
      {fields.title !== '' && <h2 className="article-preview-title">{fields.title}</h2>}
      {fields.summary !== '' && <p className="article-preview-summary">{fields.summary}</p>}
      <MarkdownRenderer markdown={fields.bodyMarkdown} />
    </div>
  )
}
