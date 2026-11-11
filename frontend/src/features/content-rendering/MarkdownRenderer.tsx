import Markdown, { type Options } from 'react-markdown'
import rehypeSanitize from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'

import { contentComponents, previewContentComponents } from './contentComponents'
import { contentSanitizeSchema } from './contentSanitizeSchema'
import { transformContentUrl } from './urlPolicy'
import './content-rendering.css'

const REMARK_PLUGINS: Options['remarkPlugins'] = [remarkGfm]
// The sanitizer must stay the last rehype plugin. A plugin added after it
// could create HTML that was never checked.
const REHYPE_PLUGINS: Options['rehypePlugins'] = [[rehypeSanitize, contentSanitizeSchema]]
const REMARK_REHYPE_OPTIONS: Options['remarkRehypeOptions'] = {
  footnoteLabel: 'Chú thích',
  footnoteBackLabel: 'Quay lại nội dung',
}

/**
 * What happens when a reader uses a link.
 *
 * - `navigate`: normal browser behavior, for reading pages.
 * - `preview`: the current page stays in place. Page links open in a new tab
 *   and in-page anchors only scroll. Used where unsaved text is on screen.
 */
export type LinkBehavior = 'navigate' | 'preview'

export interface MarkdownRendererProps {
  markdown: string
  /** Default: `navigate`. */
  linkBehavior?: LinkBehavior
}

/**
 * Renders canonical Markdown as safe semantic React elements.
 *
 * This is the only Markdown renderer of the application. The public Article
 * page and the Admin Preview must both use it, so they show the same result.
 * Raw HTML is never rendered and every URL passes the content URL policy.
 * Link behavior changes only how a click is handled, never the HTML output.
 */
export function MarkdownRenderer({ markdown, linkBehavior = 'navigate' }: MarkdownRendererProps) {
  return (
    <div className="md-content">
      <Markdown
        remarkPlugins={REMARK_PLUGINS}
        remarkRehypeOptions={REMARK_REHYPE_OPTIONS}
        rehypePlugins={REHYPE_PLUGINS}
        components={linkBehavior === 'preview' ? previewContentComponents : contentComponents}
        urlTransform={transformContentUrl}
        /* Raw HTML is outside the Markdown profile. Drop it explicitly. */
        skipHtml={true}
      >
        {markdown}
      </Markdown>
    </div>
  )
}
