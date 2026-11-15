import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { givenAnonymousVisitor, givenSignedInAdmin } from '../features/admin-auth/fixture/adminSessionFixture'
import { activeTopic, TOPICS_PATH, topicList } from '../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'
import { renderAppRoute } from './testsupport/renderAppRoute'

describe('AdminLayout', () => {
  it('navigate_ShouldOpenTopicManagement_WhenTopicLinkIsClicked', async () => {
    // Given
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic())))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/khong-co-trang-nay')
    const link = await screen.findByRole('link', { name: 'Topic' })
    expect(link).not.toHaveAttribute('aria-current')

    // When
    await user.click(link)

    // Then
    expect(await screen.findByRole('heading', { level: 1, name: 'Topic' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/topics')
    expect(screen.getByRole('link', { name: 'Topic' })).toHaveAttribute('aria-current', 'page')
  })

  it('render_ShouldHideNavigation_WhenVisitorIsAnonymous', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer()).install()

    // When
    renderAppRoute('/admin/login')

    // Then
    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Điều hướng quản trị' })).toBeNull()
  })
})
