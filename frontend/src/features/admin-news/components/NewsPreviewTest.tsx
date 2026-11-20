import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { EMPTY_NEWS_FIELDS, localInputToIso, type NewsFields } from '../model/news'
import { NewsPreview } from './NewsPreview'

function previewOf(changes: Partial<NewsFields>) {
  return render(<NewsPreview fields={{ ...EMPTY_NEWS_FIELDS, ...changes }} />)
}

describe('NewsPreview', () => {
  it('render_ShouldShowUrlAsTextWithoutLink_WhenUrlIsSafe', () => {
    previewOf({ sourceName: 'Spring Blog', sourceUrl: 'https://spring.io/blog' })

    const source = screen.getByRole('region', { name: 'Nguồn tin trong xem trước' })
    expect(source).toHaveTextContent('Nguồn: Spring Blog')
    expect(source).toHaveTextContent('https://spring.io/blog')
    expect(screen.queryByRole('link')).toBeNull()
    expect(screen.queryByText('Người đọc sẽ không thấy link này.')).toBeNull()
  })

  it.each(['javascript:alert(1)', 'JAVASCRIPT:alert(1)', 'data:text/html,x', '//evil.example'])(
    'render_ShouldWarnAndShowNoLink_WhenUrlIsUnsafe [%s]',
    (url) => {
      previewOf({ sourceName: 'Nguồn', sourceUrl: url })

      expect(screen.queryByRole('link')).toBeNull()
      expect(screen.getByText('Người đọc sẽ không thấy link này.')).toBeInTheDocument()
    },
  )

  it('render_ShouldShowNoSourceBlock_WhenSourceIsEmpty', () => {
    previewOf({ title: 'Tin không nguồn' })

    expect(screen.queryByRole('region', { name: 'Nguồn tin trong xem trước' })).toBeNull()
  })

  it('render_ShouldShowSourceDate_WhenDateIsChosen', () => {
    const { container } = previewOf({ sourceName: 'Nguồn', sourcePublishedAt: '2026-11-18T09:30' })

    expect(container.querySelector('time')).toHaveAttribute('datetime', localInputToIso('2026-11-18T09:30'))
  })

  it('render_ShouldShowNoDate_WhenDateIsEmpty', () => {
    const { container } = previewOf({ sourceName: 'Nguồn' })

    expect(container.querySelector('time')).toBeNull()
  })

  it('render_ShouldRenderBodyAsMarkdown_WhenBodyHasHeading', () => {
    previewOf({ bodyMarkdown: '## Phần một' })

    expect(screen.getByRole('heading', { level: 2, name: 'Phần một' })).toBeInTheDocument()
  })
})
