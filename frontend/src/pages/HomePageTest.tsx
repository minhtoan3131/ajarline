import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  articleCollectionResponse,
  articleSummary,
  standaloneArticlesPath,
} from '../features/article-browsing/fixture/articleCollectionFixture'
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
import { jsonResponse, problemResponse, StubApiServer, type StubResponse } from '../shared/api/testsupport/StubApiServer'

const SOME_STANDALONE = jsonResponse(200, articleCollectionResponse([articleSummary()], { size: 1, hasMore: true }))
const NO_STANDALONE = jsonResponse(200, articleCollectionResponse([], { size: 1 }))
const SOME_SERIES = jsonResponse(200, seriesCollectionResponse([seriesSummary()], { size: 1, hasMore: true }))
const NO_SERIES = jsonResponse(200, seriesCollectionResponse([], { size: 1 }))
const SOME_NEWS = jsonResponse(200, newsCollectionResponse([newsSummary()], { size: 1, hasMore: true }))
const NO_NEWS = jsonResponse(200, newsCollectionResponse([], { size: 1 }))
const FAILURE = problemResponse(500, 'INTERNAL_ERROR')

/** Stubs the three entry requests Home sends, each with its own answer. */
function stubHome(standalone: StubResponse, series: StubResponse, news: StubResponse = NO_NEWS): StubApiServer {
  return new StubApiServer()
    .on('GET', standaloneArticlesPath(0, 1), standalone)
    .on('GET', seriesCollectionApiPath(0, 1), series)
    .on('GET', newsCollectionApiPath(0, 1), news)
    .install()
}

/**
 * Waits until all three Home requests were sent, then one more turn of the
 * event loop so their answers are handled. Needed to check that something is
 * absent: there is no element to wait for.
 */
async function waitForHomeRequests(api: StubApiServer): Promise<void> {
  await waitFor(() => expect(api.requests).toHaveLength(3))
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('HomePage', () => {
  it('render_ShouldShowHeadingWithoutEntriesOrStatus_WhenAnswersAreStillLoading', () => {
    stubHome(SOME_STANDALONE, SOME_SERIES)

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
    expect(screen.queryByRole('link', { name: 'Học theo Series' })).toBeNull()
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('render_ShouldLinkToStandaloneIndex_WhenStandaloneArticlesExist', async () => {
    const api = stubHome(SOME_STANDALONE, NO_SERIES)

    renderAppRoute('/')

    const entry = await screen.findByRole('link', { name: 'Bài viết độc lập' })
    expect(entry).toHaveAttribute('href', '/articles?standalone=true')
    expect(api.requestsTo('GET', standaloneArticlesPath(0, 1))).toHaveLength(1)
    const header = screen.getByRole('navigation', { name: 'Điều hướng chính' })
    expect(within(header).queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
  })

  it('render_ShouldLinkToSeriesIndexWithOneSeriesRequest_WhenSeriesExist', async () => {
    const api = stubHome(NO_STANDALONE, SOME_SERIES)

    renderAppRoute('/')

    const entry = await screen.findByRole('link', { name: 'Học theo Series' })
    expect(entry).toHaveAttribute('href', '/series')
    expect(api.requestsTo('GET', seriesCollectionApiPath(0, 1))).toHaveLength(1)
    expect(screen.queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
  })

  it('render_ShouldShowBothEntriesInOrder_WhenBothDestinationsHaveContent', async () => {
    stubHome(SOME_STANDALONE, SOME_SERIES)

    renderAppRoute('/')

    await screen.findByRole('link', { name: 'Học theo Series' })
    await screen.findByRole('link', { name: 'Bài viết độc lập' })
    const entries = screen.getByRole('list', { name: 'Lối vào' })
    expect(within(entries).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Học theo Series',
      'Bài viết độc lập',
    ])
  })

  it('render_ShouldHideEntries_WhenThereIsNothingToLinkTo', async () => {
    const api = stubHome(NO_STANDALONE, NO_SERIES)

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    await waitForHomeRequests(api)
    expect(screen.queryByRole('list', { name: 'Lối vào' })).toBeNull()
  })

  it('render_ShouldKeepHomeUsableWithoutEntries_WhenBothRequestsFail', async () => {
    const api = stubHome(FAILURE, FAILURE)

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    await waitForHomeRequests(api)
    expect(screen.queryByRole('list', { name: 'Lối vào' })).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('render_ShouldKeepOtherEntry_WhenOnlySeriesRequestFails', async () => {
    const api = stubHome(SOME_STANDALONE, FAILURE)

    renderAppRoute('/')

    expect(await screen.findByRole('link', { name: 'Bài viết độc lập' })).toBeInTheDocument()
    await waitForHomeRequests(api)
    expect(screen.queryByRole('link', { name: 'Học theo Series' })).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('navigate_ShouldOpenStandaloneIndex_WhenEntryIsClicked', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), SOME_STANDALONE)
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .on('GET', seriesCollectionApiPath(0, 1), NO_SERIES)
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(await screen.findByRole('link', { name: 'Bài viết độc lập' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bài viết độc lập' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/articles')
    expect(screen.getByTestId('search')).toHaveTextContent('?standalone=true')
  })

  it('navigate_ShouldOpenSeriesIndex_WhenEntryIsClicked', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), NO_STANDALONE)
      .on('GET', seriesCollectionApiPath(0, 1), SOME_SERIES)
      .on('GET', seriesCollectionApiPath(0), jsonResponse(200, seriesCollectionResponse([seriesSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(await screen.findByRole('link', { name: 'Học theo Series' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Series' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/series')
  })

  it('render_ShouldLinkToNewsIndexWithOneNewsRequest_WhenNewsExist', async () => {
    const api = stubHome(NO_STANDALONE, NO_SERIES, SOME_NEWS)

    renderAppRoute('/')

    const entries = await screen.findByRole('list', { name: 'Lối vào' })
    expect(within(entries).getByRole('link', { name: 'Tin công nghệ' })).toHaveAttribute('href', '/news')
    expect(api.requestsTo('GET', newsCollectionApiPath(0, 1))).toHaveLength(1)
    expect(screen.queryByText('Spring Boot 4.1 ra mắt')).toBeNull()
  })

  it('render_ShouldShowThreeEntriesInOrder_WhenEveryDestinationHasContent', async () => {
    stubHome(SOME_STANDALONE, SOME_SERIES, SOME_NEWS)

    renderAppRoute('/')

    await screen.findByRole('link', { name: 'Học theo Series' })
    await screen.findByRole('link', { name: 'Bài viết độc lập' })
    const entries = await screen.findByRole('list', { name: 'Lối vào' })
    await within(entries).findByRole('link', { name: 'Tin công nghệ' })
    expect(within(entries).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Học theo Series',
      'Bài viết độc lập',
      'Tin công nghệ',
    ])
  })

  it.each([
    ['empty', NO_NEWS],
    ['a failure', FAILURE],
  ])('render_ShouldHideNewsEntry_WhenNewsAnswerIs (%s)', async (_label, news) => {
    const api = stubHome(SOME_STANDALONE, NO_SERIES, news)

    renderAppRoute('/')

    const entries = await screen.findByRole('list', { name: 'Lối vào' })
    await waitForHomeRequests(api)
    expect(within(entries).queryByRole('link', { name: 'Tin công nghệ' })).toBeNull()
    expect(within(entries).getByRole('link', { name: 'Bài viết độc lập' })).toBeInTheDocument()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('navigate_ShouldOpenNewsIndex_WhenEntryIsClicked', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), NO_STANDALONE)
      .on('GET', seriesCollectionApiPath(0, 1), NO_SERIES)
      .on('GET', newsCollectionApiPath(0, 1), SOME_NEWS)
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([newsSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    const entries = await screen.findByRole('list', { name: 'Lối vào' })
    await user.click(within(entries).getByRole('link', { name: 'Tin công nghệ' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Tin công nghệ' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/news')
  })
})
