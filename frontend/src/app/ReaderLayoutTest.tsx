import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import {
  seriesCollectionApiPath,
  seriesCollectionResponse,
  seriesSummary,
} from '../features/learning-navigation/fixture/publicLearningFixture'
import {
  newsCollectionApiPath,
  newsCollectionResponse,
  newsSummary,
} from '../features/news-reading/fixture/publicNewsFixture'
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

  it('render_ShouldLinkSeries_WhenTheApiHasNotAnswered', () => {
    vi.stubGlobal('fetch', () => new Promise<Response>(() => {}))

    renderAppRoute('/')

    const series = within(mainNavigation()).getByRole('link', { name: 'Series' })
    expect(series).toHaveAttribute('href', '/series')
    expect(series).not.toHaveAttribute('aria-current')
  })

  it('navigate_ShouldOpenSeriesIndex_WhenSeriesLinkIsClicked', async () => {
    new StubApiServer()
      .on('GET', seriesCollectionApiPath(0), jsonResponse(200, seriesCollectionResponse([seriesSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(within(mainNavigation()).getByRole('link', { name: 'Series' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Series' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/series')
    expect(within(mainNavigation()).getByRole('link', { name: 'Series' })).toHaveAttribute('aria-current', 'page')
    expect(within(mainNavigation()).getByRole('link', { name: 'Trang chủ' })).not.toHaveAttribute('aria-current')
  })

  it('render_ShouldLinkNews_WhenTheApiHasNotAnswered', () => {
    vi.stubGlobal('fetch', () => new Promise<Response>(() => {}))

    renderAppRoute('/')

    const news = within(mainNavigation()).getByRole('link', { name: 'Tin công nghệ' })
    expect(news).toHaveAttribute('href', '/news')
    expect(news).not.toHaveAttribute('aria-current')
  })

  it('navigate_ShouldOpenNewsIndex_WhenNewsLinkIsClicked', async () => {
    new StubApiServer()
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([newsSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(within(mainNavigation()).getByRole('link', { name: 'Tin công nghệ' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Tin công nghệ' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/news')
    expect(within(mainNavigation()).getByRole('link', { name: 'Tin công nghệ' })).toHaveAttribute(
      'aria-current',
      'page',
    )
    expect(within(mainNavigation()).getByRole('link', { name: 'Trang chủ' })).not.toHaveAttribute('aria-current')
  })
})
