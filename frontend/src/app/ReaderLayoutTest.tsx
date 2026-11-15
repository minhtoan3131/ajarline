import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { JAVA_TOPIC_REF, topicDiscovery, TOPICS_PATH } from '../features/topic-browsing/fixture/publicTopicFixture'
import { jsonResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'
import { renderAppRoute } from './testsupport/renderAppRoute'

function mainNavigation(): HTMLElement {
  return screen.getByRole('navigation', { name: 'Điều hướng chính' })
}

describe('ReaderLayout', () => {
  it('render_ShouldLinkHomeAndTopics_WhenReaderPageOpens', () => {
    renderAppRoute('/')

    const home = within(mainNavigation()).getByRole('link', { name: 'Trang chủ' })
    const topics = within(mainNavigation()).getByRole('link', { name: 'Chủ đề' })
    expect(home).toHaveAttribute('href', '/')
    expect(home).toHaveAttribute('aria-current', 'page')
    expect(topics).toHaveAttribute('href', '/topics')
    expect(topics).not.toHaveAttribute('aria-current')
  })

  it('navigate_ShouldOpenTopicIndex_WhenTopicsLinkIsClicked', async () => {
    new StubApiServer().on('GET', TOPICS_PATH, jsonResponse(200, topicDiscovery(JAVA_TOPIC_REF))).install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(within(mainNavigation()).getByRole('link', { name: 'Chủ đề' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Chủ đề' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/topics')
    expect(within(mainNavigation()).getByRole('link', { name: 'Chủ đề' })).toHaveAttribute('aria-current', 'page')
    expect(within(mainNavigation()).getByRole('link', { name: 'Trang chủ' })).not.toHaveAttribute('aria-current')
  })

  it('render_ShouldNotLinkToAdmin_WhenReaderPageOpens', () => {
    renderAppRoute('/')

    expect(screen.queryByRole('link', { name: /quản trị|admin|đăng nhập/i })).toBeNull()
  })
})
