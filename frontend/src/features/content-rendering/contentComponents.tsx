import type { Element, ElementContent } from 'hast'
import { useState, type ComponentProps } from 'react'
import type { Components, ExtraProps } from 'react-markdown'

import { CodeBlock } from './CodeBlock'
import { isExternalHref } from './urlPolicy'

function ArticleLink({ node, href, children, ...attributes }: ComponentProps<'a'> & ExtraProps) {
  if (!href) {
    // The URL policy removed an unsafe target. Keep the text, never the link.
    return <span className="md-link-disabled">{children}</span>
  }
  if (isExternalHref(href)) {
    // NOTE: external links open in the same tab until the new-tab rule is
    // decided. Change it here so the whole renderer stays consistent.
    return (
      <a {...attributes} href={href} rel="noopener noreferrer external">
        {children}
      </a>
    )
  }
  return (
    <a {...attributes} href={href}>
      {children}
    </a>
  )
}

function ArticleImage({ src, alt, title }: ComponentProps<'img'> & ExtraProps) {
  const [failed, setFailed] = useState(false)

  if (!src || failed) {
    // A blocked or broken image must never break the page.
    const text = alt ? `Không thể hiển thị hình ảnh: ${alt}` : 'Không thể hiển thị hình ảnh'
    return (
      <span className="md-image-fallback" role="img" aria-label={text}>
        {text}
      </span>
    )
  }
  return <img src={src} alt={alt ?? ''} title={title} loading="lazy" onError={() => setFailed(true)} />
}

function ArticleTable({ node, ...attributes }: ComponentProps<'table'> & ExtraProps) {
  // A wide table scrolls inside this container instead of the whole page.
  // The container is focusable so keyboard users can scroll it too.
  return (
    <div className="md-table-scroll" role="region" aria-label="Bảng dữ liệu" tabIndex={0}>
      <table {...attributes} />
    </div>
  )
}

function ArticlePre({ node, children, ...attributes }: ComponentProps<'pre'> & ExtraProps) {
  const code = findCodeElement(node)
  if (code === undefined) {
    return <pre {...attributes}>{children}</pre>
  }
  return <CodeBlock code={rawCode(code)} declaredLanguage={declaredLanguage(code)} />
}

function InlineCode({ node, ...attributes }: ComponentProps<'code'> & ExtraProps) {
  // Fenced code never reaches this component: ArticlePre renders it from
  // the syntax tree and ignores its React children.
  return <code {...attributes} className="md-inline-code" />
}

function findCodeElement(pre: Element | undefined): Element | undefined {
  return pre?.children.find(
    (child): child is Element => child.type === 'element' && child.tagName === 'code',
  )
}

function declaredLanguage(code: Element): string | null {
  const classes = code.properties.className
  if (!Array.isArray(classes)) {
    return null
  }
  for (const value of classes) {
    if (typeof value === 'string' && value.startsWith('language-')) {
      return value.slice('language-'.length)
    }
  }
  return null
}

function textOf(node: ElementContent): string {
  if (node.type === 'text') {
    return node.value
  }
  if (node.type === 'element') {
    return node.children.map(textOf).join('')
  }
  return ''
}

function rawCode(code: Element): string {
  // Markdown keeps one line break before the closing fence. Copy must
  // return only the lines the author wrote between the fences.
  return code.children.map(textOf).join('').replace(/\n$/, '')
}

/**
 * Markdown elements that need behavior beyond their default HTML element.
 *
 * Headings, paragraphs, lists, blockquotes and rules keep their semantic
 * default elements and are styled by CSS only.
 */
export const contentComponents: Components = {
  a: ArticleLink,
  img: ArticleImage,
  table: ArticleTable,
  pre: ArticlePre,
  code: InlineCode,
}
