import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import type { NewsSource } from '../model/publicNews'
import { NewsSourceBlock } from './NewsSourceBlock'

function source(changes: Partial<NewsSource> = {}): NewsSource {
  return {
    name: 'Spring Blog',
    url: 'https://spring.io/blog/spring-boot-4-1',
    publishedAt: '2026-11-03T00:00:00Z',
    ...changes,
  }
}

function sourceRegion(): HTMLElement {
  return screen.getByRole('region', { name: 'Nguồn gốc' })
}

describe('NewsSourceBlock', () => {
  it('render_ShouldLinkSourceNameThroughExternalLink_WhenUrlIsSafe', () => {
    render(<NewsSourceBlock source={source()} />)

    const link = within(sourceRegion()).getByRole('link', { name: 'Spring Blog (mở trong tab mới)' })
    expect(link).toHaveAttribute('href', 'https://spring.io/blog/spring-boot-4-1')
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer external')
    expect(within(sourceRegion()).getByText('https://spring.io/blog/spring-boot-4-1')).toBeInTheDocument()
  })

  it.each(['javascript:alert(1)', 'data:text/html,x', '//evil.example/x'])(
    'render_ShouldShowNameWithoutLinkOrUrl_WhenUrlIsUnsafe (%s)',
    (url) => {
      render(<NewsSourceBlock source={source({ url })} />)

      const region = sourceRegion()
      expect(within(region).queryByRole('link')).toBeNull()
      expect(within(region).getByText('Spring Blog')).toBeInTheDocument()
      expect(region).not.toHaveTextContent(url)
    },
  )

  it('render_ShouldShowSourceDate_WhenSourceHasPublishedAt', () => {
    render(<NewsSourceBlock source={source()} />)

    expect(sourceRegion()).toHaveTextContent(/Nguồn đăng\s+3 tháng 11/)
  })

  it('render_ShouldShowNoSourceDate_WhenSourcePublishedAtIsNull', () => {
    const { container } = render(<NewsSourceBlock source={source({ publishedAt: null })} />)

    expect(screen.queryByText(/Nguồn đăng/)).toBeNull()
    expect(container.querySelector('time')).toBeNull()
  })
})
