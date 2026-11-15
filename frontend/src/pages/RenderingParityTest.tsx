import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { PublicArticleView } from '../features/article-reading/components/PublicArticleView'
import { ArticlePreview } from '../features/admin-content/components/ArticlePreview'

const FIXTURE = [
  '## Tiêu đề phụ',
  '- một\n- hai',
  '```java\nclass A {}\n```',
  '| A | B |\n|---|---|\n| 1 | 2 |',
  '[Liên kết ngoài](https://example.com) và `mã`',
  '<script>alert(1)</script>',
].join('\n\n')

function rendererHtml(container: HTMLElement): string {
  return container.querySelector('.md-content')?.innerHTML ?? ''
}

describe('Rendering parity', () => {
  it('render_ShouldProduceSameMarkup_WhenPreviewAndPublicPageRenderSameMarkdown', () => {
    // Given
    const preview = render(
      <ArticlePreview fields={{ title: 'T', slug: 't', summary: 'S', bodyMarkdown: FIXTURE, topicIds: [] }} />,
    )
    const previewHtml = rendererHtml(preview.container)
    preview.unmount()

    // When
    const publicPage = render(
      <PublicArticleView
        article={{
          id: 'a',
          slug: 't',
          title: 'T',
          summary: 'S',
          bodyMarkdown: FIXTURE,
          topics: [],
          publishedAt: '2026-11-01T02:00:00Z',
          updatedAt: '2026-11-01T02:00:00Z',
        }}
      />,
    )

    // Then
    expect(previewHtml).not.toBe('')
    expect(rendererHtml(publicPage.container)).toBe(previewHtml)
  })
})
