import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { MarkdownRenderer } from './MarkdownRenderer'

const FOOTNOTE = 'Câu có chú thích[^1].\n\n[^1]: Nội dung chú thích.'

afterEach(() => {
  vi.restoreAllMocks()
  // jsdom has no scrollIntoView. Remove the stub a test added.
  Reflect.deleteProperty(Element.prototype, 'scrollIntoView')
})

function stubScrollIntoView(): string[] {
  const scrolledIds: string[] = []
  Object.defineProperty(Element.prototype, 'scrollIntoView', {
    configurable: true,
    value(this: Element) {
      scrolledIds.push(this.id)
    },
  })
  return scrolledIds
}

describe('Markdown links', () => {
  it('click_ShouldKeepBrowserBehavior_WhenRendererIsInNavigateMode', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const { container } = render(<MarkdownRenderer markdown={FOOTNOTE} />)
    const reference = container.querySelector<HTMLAnchorElement>('sup a')!

    const followed = fireEvent.click(reference)

    expect(followed).toBe(true)
    expect(open).not.toHaveBeenCalled()
  })

  it('click_ShouldOpenNewTab_WhenPreviewLinkPointsToSitePage', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    render(<MarkdownRenderer markdown="[Bài khác](/articles/bai-khac)" linkBehavior="preview" />)
    const link = screen.getByRole('link', { name: 'Bài khác' })

    const followed = fireEvent.click(link)

    expect(followed).toBe(false)
    expect(open).toHaveBeenCalledWith(
      new URL('/articles/bai-khac', window.location.href).href,
      '_blank',
      'noopener,noreferrer',
    )
    expect(link).toHaveAttribute('href', '/articles/bai-khac')
  })

  it('click_ShouldOpenNewTab_WhenPreviewLinkIsExternal', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    render(<MarkdownRenderer markdown="[Tài liệu](https://example.com/docs)" linkBehavior="preview" />)

    const followed = fireEvent.click(screen.getByRole('link', { name: 'Tài liệu' }))

    expect(followed).toBe(false)
    expect(open).toHaveBeenCalledWith('https://example.com/docs', '_blank', 'noopener,noreferrer')
  })

  it('click_ShouldScrollInsideSameContent_WhenPreviewLinkIsFootnote', () => {
    const scrolledIds = stubScrollIntoView()
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const hashBefore = window.location.hash
    const { container } = render(<MarkdownRenderer markdown={FOOTNOTE} linkBehavior="preview" />)
    const reference = container.querySelector<HTMLAnchorElement>('sup a')!

    const followed = fireEvent.click(reference)

    expect(followed).toBe(false)
    expect(open).not.toHaveBeenCalled()
    expect(scrolledIds).toEqual(['user-content-fn-1'])
    expect(window.location.hash).toBe(hashBefore)
  })

  it('click_ShouldLeaveMailLinkToBrowser_WhenPreviewLinkIsMail', () => {
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    render(<MarkdownRenderer markdown="[Gửi thư](mailto:admin@example.com)" linkBehavior="preview" />)

    const followed = fireEvent.click(screen.getByRole('link', { name: 'Gửi thư' }))

    expect(followed).toBe(true)
    expect(open).not.toHaveBeenCalled()
  })
})
