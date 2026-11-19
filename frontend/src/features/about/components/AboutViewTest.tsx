import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'

import { EDITORIAL_PROFILE } from '../../../shared/content/editorialProfile'
import { AboutView } from './AboutView'

// Only a router: there is no HttpClientProvider, so the page cannot call the API.
function renderAbout() {
  return render(
    <MemoryRouter>
      <AboutView />
    </MemoryRouter>,
  )
}

describe('AboutView', () => {
  it('render_ShouldShowPageWithoutHttpClient_WhenAboutIsRendered', () => {
    renderAbout()

    expect(screen.getByRole('heading', { level: 1, name: 'Về Ajarline' })).toBeInTheDocument()
  })

  it('render_ShouldShowFourSectionsInOrder_WhenAboutIsRendered', () => {
    renderAbout()

    const headings = screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)
    expect(headings).toEqual(['Ajarline là gì', 'Cách làm nội dung', 'Trách nhiệm', 'Đi tiếp'])
  })

  it('render_ShouldShowProfileFromEditorialProfile_WhenAboutIsRendered', () => {
    renderAbout()

    const section = screen.getByRole('region', { name: 'Trách nhiệm' })
    expect(within(section).getByText(EDITORIAL_PROFILE.responsibilityLabel)).toBeInTheDocument()
    expect(within(section).getByText(EDITORIAL_PROFILE.displayName)).toBeInTheDocument()
    expect(within(section).getByText(EDITORIAL_PROFILE.shortBio)).toBeInTheDocument()
  })

  it('render_ShouldLinkFourInternalPagesInSameTab_WhenAboutIsRendered', () => {
    renderAbout()

    const next = within(screen.getByRole('region', { name: 'Đi tiếp' }))
    const expected = [
      ['Trang chủ', '/'],
      ['Chủ đề', '/topics'],
      ['Series', '/series'],
      ['Tin công nghệ', '/news'],
    ]
    expect(next.getAllByRole('link')).toHaveLength(expected.length)
    for (const [name, href] of expected) {
      const link = next.getByRole('link', { name })
      expect(link).toHaveAttribute('href', href)
      expect(link).not.toHaveAttribute('target')
    }
  })

  it('render_ShouldSetTabTitle_WhenAboutIsRendered', () => {
    renderAbout()

    expect(document.title).toBe('Về Ajarline · Ajarline')
  })
})
