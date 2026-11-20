import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { MarkdownRenderer } from './MarkdownRenderer'

function renderMarkdown(markdown: string) {
  return render(<MarkdownRenderer markdown={markdown} />)
}

describe('MarkdownRenderer', () => {
  it('render_ShouldKeepHeadingLevels_WhenMarkdownHasHeadingHierarchy', () => {
    // When
    renderMarkdown('# Một\n\n## Hai\n\n### Ba')

    // Then
    expect(screen.getByRole('heading', { level: 1, name: 'Một' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Hai' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 3, name: 'Ba' })).toBeInTheDocument()
  })

  it('render_ShouldRenderSemanticElements_WhenMarkdownUsesBasicProfile', () => {
    // Given
    const markdown = [
      'Chữ **đậm**, *nghiêng* và ~~gạch~~.',
      '1. Một\n   - Con\n2. Hai',
      '> Trích dẫn',
      '---',
    ].join('\n\n')

    // When
    const { container } = renderMarkdown(markdown)

    // Then
    expect(container.querySelector('strong')).toHaveTextContent('đậm')
    expect(container.querySelector('em')).toHaveTextContent('nghiêng')
    expect(container.querySelector('del')).toHaveTextContent('gạch')
    expect(container.querySelector('ol > li > ul > li')).toHaveTextContent('Con')
    expect(container.querySelector('blockquote')).toHaveTextContent('Trích dẫn')
    expect(container.querySelector('hr')).not.toBeNull()
  })

  it('render_ShouldShowReadOnlyTaskList_WhenMarkdownHasTaskItems', () => {
    // When
    renderMarkdown('- [x] Xong\n- [ ] Chưa')

    // Then
    const boxes = screen.getAllByRole('checkbox')
    expect(boxes).toHaveLength(2)
    expect(boxes[0]).toBeChecked()
    expect(boxes[0]).toBeDisabled()
    expect(boxes[1]).not.toBeChecked()
  })

  it('render_ShouldWrapTableInScrollRegion_WhenMarkdownHasTable', () => {
    // When
    renderMarkdown('| A | B |\n|---|---|\n| 1 | 2 |')

    // Then
    const region = screen.getByRole('region', { name: 'Bảng dữ liệu' })
    expect(region).toContainElement(screen.getByRole('table'))
    expect(region).toHaveAttribute('tabindex', '0')
    expect(screen.getAllByRole('columnheader')).toHaveLength(2)
  })

  it('render_ShouldLinkFootnoteToItsNote_WhenMarkdownHasFootnote', () => {
    // When
    const { container } = renderMarkdown('Câu có chú thích[^1].\n\n[^1]: Nội dung chú thích.')

    // Then
    const reference = container.querySelector('a[data-footnote-ref]')
    expect(reference).not.toBeNull()
    const targetId = reference?.getAttribute('href')?.slice(1) ?? ''
    expect(container.querySelector(`[id="${targetId}"]`)).toHaveTextContent('Nội dung chú thích.')
  })

  it('render_ShouldMarkOnlyAbsoluteHttpLinksAsExternal_WhenMarkdownHasLinks', () => {
    renderMarkdown('[Trong site](/articles/a), [Bên ngoài](https://example.com) và https://example.org')

    expect(screen.getByRole('link', { name: 'Trong site' })).not.toHaveAttribute('rel')
    expect(screen.getByRole('link', { name: 'Bên ngoài (mở trong tab mới)' })).toHaveAttribute(
      'rel',
      'noopener noreferrer external',
    )
    expect(screen.getByRole('link', { name: 'https://example.org (mở trong tab mới)' })).toHaveAttribute(
      'href',
      'https://example.org',
    )
  })

  it('render_ShouldOpenExternalLinkInNewTabAndKeepOthersInSameTab_WhenMarkdownHasLinks', () => {
    renderMarkdown('[Trong site](/articles/a), [Bên ngoài](https://example.com), [Gửi thư](mailto:a@example.com)')

    expect(screen.getByRole('link', { name: 'Bên ngoài (mở trong tab mới)' })).toHaveAttribute('target', '_blank')
    expect(screen.getByRole('link', { name: 'Trong site' })).not.toHaveAttribute('target')
    expect(screen.getByRole('link', { name: 'Gửi thư' })).not.toHaveAttribute('target')
    expect(screen.getByRole('link', { name: 'Gửi thư' })).not.toHaveAttribute('rel')
  })

  it('render_ShouldKeepFootnoteLinkInSameTab_WhenMarkdownHasFootnote', () => {
    const { container } = renderMarkdown('Câu có chú thích[^1].\n\n[^1]: Nội dung chú thích.')

    expect(container.querySelector('a[data-footnote-ref]')).not.toHaveAttribute('target')
  })

  it('render_ShouldRenderImage_WhenImageUrlIsApproved', () => {
    // When
    renderMarkdown('![Sơ đồ](https://cdn.example.com/so-do.png)')

    // Then
    expect(screen.getByRole('img', { name: 'Sơ đồ' })).toHaveAttribute(
      'src',
      'https://cdn.example.com/so-do.png',
    )
  })

  it('render_ShouldShowFallback_WhenImageFailsToLoad', () => {
    // Given
    renderMarkdown('![Sơ đồ](https://cdn.example.com/so-do.png)')

    // When
    fireEvent.error(screen.getByRole('img', { name: 'Sơ đồ' }))

    // Then
    expect(
      screen.getByRole('img', { name: 'Không thể hiển thị hình ảnh: Sơ đồ' }),
    ).toBeInTheDocument()
  })
})

describe('MarkdownRenderer security', () => {
  it('render_ShouldNotCreateHtmlElements_WhenMarkdownContainsRawHtml', () => {
    // Given
    const markdown = [
      '<script>alert(1)</script>',
      'Đoạn có <b onclick="alert(1)">đậm</b> giả.',
      '<img src="x" onerror="alert(1)">',
      '<style>body { display: none; }</style>',
      '<div style="position: fixed">phủ trang</div>',
      '<iframe src="https://evil.example"></iframe>',
    ].join('\n\n')

    // When
    const { container } = renderMarkdown(markdown)

    // Then
    expect(container.querySelector('script, style, iframe, b, img, .md-content div')).toBeNull()
    expect(container.querySelector('[onclick], [onerror], [style]')).toBeNull()
    expect(container).toHaveTextContent('đậm')
  })

  it.each([
    '[bấm](javascript:alert(1))',
    '[bấm](JaVaScRiPt:alert(1))',
    '[bấm](java&#x09;script:alert(1))',
    '[bấm](data:text/html;base64,PHNjcmlwdD4=)',
    '[bấm](vbscript:msgbox(1))',
    '[bấm][x]\n\n[x]: javascript:alert(1)',
    '[bấm](javascript:alert(1)',
  ])('render_ShouldNotCreateLink_WhenUrlIsUnsafe [%s]', (markdown) => {
    // When
    const { container } = renderMarkdown(markdown)

    // Then
    expect(container.querySelector('a')).toBeNull()
    expect(container).toHaveTextContent('bấm')
  })

  it.each([
    '![ảnh](data:image/png;base64,AAAA)',
    '![ảnh](http://example.com/a.png)',
    '![ảnh](javascript:alert(1))',
  ])('render_ShouldShowImageFallback_WhenImageUrlIsUnsafe [%s]', (markdown) => {
    // When
    const { container } = renderMarkdown(markdown)

    // Then
    expect(container.querySelector('img')).toBeNull()
    expect(screen.getByRole('img', { name: 'Không thể hiển thị hình ảnh: ảnh' })).toBeInTheDocument()
  })

  it('render_ShouldKeepJsxAsPlainText_WhenMarkdownLooksLikeMdx', () => {
    // When
    const { container } = renderMarkdown('Giá trị {alert(1)} và <Widget onLoad={run} />')

    // Then
    expect(container.querySelector('widget')).toBeNull()
    expect(container).toHaveTextContent('Giá trị {alert(1)} và')
  })
})
