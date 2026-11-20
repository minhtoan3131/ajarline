import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { ExternalLink } from './ExternalLink'

describe('ExternalLink', () => {
  it('render_ShouldOpenNewTabWithoutAccessToThisPage_WhenLinkIsRendered', () => {
    render(<ExternalLink href="https://example.com/docs">Tài liệu</ExternalLink>)

    const link = screen.getByRole('link', { name: 'Tài liệu (mở trong tab mới)' })
    expect(link).toHaveAttribute('href', 'https://example.com/docs')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer external')
  })

  it('render_ShouldHideArrowFromAssistiveTechnology_WhenLinkIsRendered', () => {
    const { container } = render(<ExternalLink href="https://example.com/docs">Tài liệu</ExternalLink>)

    const mark = container.querySelector('.external-link-mark')
    expect(mark).toHaveAttribute('aria-hidden', 'true')
    expect(mark).toHaveTextContent('↗')
  })

  it('render_ShouldKeepCallerAttributes_WhenCallerPassesHandlerAndClassName', () => {
    const onClick = vi.fn()
    render(
      <ExternalLink href="https://example.com/docs" className="custom" onClick={onClick}>
        Tài liệu
      </ExternalLink>,
    )
    const link = screen.getByRole('link', { name: 'Tài liệu (mở trong tab mới)' })

    fireEvent.click(link)

    expect(onClick).toHaveBeenCalledOnce()
    expect(link).toHaveClass('custom')
  })
})
