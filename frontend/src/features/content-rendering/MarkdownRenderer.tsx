import Markdown, { type Options } from 'react-markdown'
import rehypeSanitize from 'rehype-sanitize'
import remarkGfm from 'remark-gfm'

import { contentComponents } from './contentComponents'
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

export interface MarkdownRendererProps {
  markdown: string
}

/**
 * Renders canonical Markdown as safe semantic React elements.
 *
 * This is the only Markdown renderer of the application. The public Article
 * page and the Admin Preview must both use it, so they show the same result.
 * Raw HTML is never rendered and every URL passes the content URL policy.
 */
export function MarkdownRenderer({ markdown }: MarkdownRendererProps) {
  return (
    <div className="md-content">
      <Markdown
        remarkPlugins={REMARK_PLUGINS}
        remarkRehypeOptions={REMARK_REHYPE_OPTIONS}
        rehypePlugins={REHYPE_PLUGINS}
        components={contentComponents}
        urlTransform={transformContentUrl}
        /* Raw HTML is outside the Markdown profile. Drop it explicitly. */
        skipHtml={true}
      >
        {markdown}
      </Markdown>
    </div>
  )
}
