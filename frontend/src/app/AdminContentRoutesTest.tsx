import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { givenAnonymousVisitor, givenSignedInAdmin } from '../features/admin-auth/fixture/adminSessionFixture'
import { contentItem, givenContentIndex } from '../features/admin-content/fixture/contentIndexFixture'
import { StubApiServer } from '../shared/api/testsupport/StubApiServer'
import { renderAppRoute } from './testsupport/renderAppRoute'

describe('Admin content routes', () => {
  it('open_ShouldShowContentIndex_WhenSignedInAdminOpensAdminRoot', async () => {
    givenContentIndex(givenSignedInAdmin(new StubApiServer()), [contentItem()]).install()

    renderAppRoute('/admin')

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
  })

  it('open_ShouldShowSeriesOnly_WhenOldSeriesListUrlIsOpened', async () => {
    givenContentIndex(givenSignedInAdmin(new StubApiServer()), [contentItem({ type: 'series', title: 'Series Java' })], {
      filters: 'types=series',
    }).install()

    renderAppRoute('/admin/series')

    expect(await screen.findByRole('link', { name: 'Series Java' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
    expect(screen.getByTestId('search')).toHaveTextContent('?types=series')
    expect(screen.getByRole('checkbox', { name: 'Series' })).toBeChecked()
  })

  it('open_ShouldShowNewsOnly_WhenOldNewsListUrlIsOpened', async () => {
    givenContentIndex(givenSignedInAdmin(new StubApiServer()), [contentItem()], { filters: 'types=news' }).install()

    renderAppRoute('/admin/news')

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(screen.getByTestId('search')).toHaveTextContent('?types=news')
    expect(screen.getByRole('checkbox', { name: 'Tin' })).toBeChecked()
  })

  it('open_ShouldLeaveNoOldUrlInHistory_WhenOldListUrlIsRedirected', async () => {
    givenContentIndex(givenSignedInAdmin(new StubApiServer()), [contentItem()], { filters: 'types=news' }).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/news', { previous: '/admin/khong-co-trang-nay' })
    await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })

    await user.click(screen.getByRole('button', { name: 'Back (test)' }))

    expect(screen.getByTestId('location')).toHaveTextContent('/admin/khong-co-trang-nay')
  })

  it('open_ShouldGoToLogin_WhenVisitorIsAnonymous', async () => {
    givenAnonymousVisitor(new StubApiServer()).install()

    renderAppRoute('/admin')

    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })
})
