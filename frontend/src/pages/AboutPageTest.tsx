import { screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'

describe('AboutPage', () => {
  it('render_ShouldShowAboutInReaderFrameWithoutNetwork_WhenAboutIsOpened', () => {
    const fetchSpy = vi.fn()
    vi.stubGlobal('fetch', fetchSpy)

    renderAppRoute('/about')

    expect(screen.getByRole('heading', { level: 1, name: 'Về Ajarline' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Điều hướng chính' })).toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('render_ShouldShowAbout_WhenUrlHasAnyQuery', () => {
    renderAppRoute('/about?ref=header&page=3')

    expect(screen.getByRole('heading', { level: 1, name: 'Về Ajarline' })).toBeInTheDocument()
  })

  it('render_ShouldShowNotFound_WhenUrlIsBelowAbout', () => {
    renderAppRoute('/about/x')

    expect(screen.getByRole('heading', { level: 1, name: 'Không tìm thấy nội dung' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 1, name: 'Về Ajarline' })).toBeNull()
  })
})
