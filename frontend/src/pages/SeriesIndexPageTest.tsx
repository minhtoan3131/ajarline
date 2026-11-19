import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  seriesCollectionApiPath,
  seriesCollectionResponse,
  seriesSummary,
} from '../features/learning-navigation/fixture/publicLearningFixture'
import { JAVA_TOPIC_REF, SPRING_TOPIC_REF } from '../features/topic-browsing/fixture/publicTopicFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const SECOND_SERIES = seriesSummary({
  id: '2c7e9a14-6b3d-4f80-9a25-d1e8f0b4c6a3',
  slug: 'spring-security',
  title: 'Spring Security',
  summary: 'Bảo mật ứng dụng Spring.',
  topics: [],
})

describe('SeriesIndexPage', () => {
  it('render_ShouldShowLoadingFirst_WhenIndexIsRequested', () => {
    new StubApiServer()
      .on('GET', seriesCollectionApiPath(0), jsonResponse(200, seriesCollectionResponse([seriesSummary()])))
      .install()

    renderAppRoute('/series')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách Series…')
  })

  it('render_ShouldListSeriesAsLinksWithTopics_WhenSeriesExist', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        seriesCollectionApiPath(0),
        jsonResponse(
          200,
          seriesCollectionResponse([seriesSummary({ topics: [JAVA_TOPIC_REF, SPRING_TOPIC_REF] }), SECOND_SERIES]),
        ),
      )
      .install()

    renderAppRoute('/series')

    expect(await screen.findByRole('heading', { level: 1, name: 'Series' })).toBeInTheDocument()
    const list = screen.getByRole('list', { name: 'Các Series' })
    expect(within(list).getByRole('link', { name: 'Java Concurrency' })).toHaveAttribute(
      'href',
      '/series/java-concurrency',
    )
    expect(within(list).getByText('Học xử lý đồng thời trong Java.')).toBeInTheDocument()
    expect(within(list).getByRole('link', { name: 'Spring Security' })).toHaveAttribute(
      'href',
      '/series/spring-security',
    )
    const chips = within(list).getByRole('list', { name: 'Chủ đề của Java Concurrency' })
    expect(within(chips).getAllByRole('link').map((link) => link.textContent)).toEqual(['Java', 'Spring Boot'])
    expect(within(list).queryByRole('list', { name: 'Chủ đề của Spring Security' })).toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(api.requests.map((request) => request.path)).toEqual([seriesCollectionApiPath(0)])
  })

  it('navigate_ShouldLoadNextPageAndKeepFirstPageWithoutPageQuery_WhenNextIsClicked', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        seriesCollectionApiPath(0),
        jsonResponse(200, seriesCollectionResponse([seriesSummary()], { hasMore: true })),
      )
      .on('GET', seriesCollectionApiPath(1), jsonResponse(200, seriesCollectionResponse([SECOND_SERIES], { page: 1 })))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/series')
    const firstPagination = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(firstPagination).toHaveTextContent('Trang 1')
    expect(within(firstPagination).queryByRole('link', { name: 'Trang trước' })).toBeNull()

    await user.click(within(firstPagination).getByRole('link', { name: 'Trang sau' }))

    expect(await screen.findByRole('link', { name: 'Spring Security' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/series')
    expect(screen.getByTestId('search')).toHaveTextContent('?page=1')
    const secondPagination = screen.getByRole('navigation', { name: 'Phân trang' })
    expect(secondPagination).toHaveTextContent('Trang 2')
    expect(within(secondPagination).getByRole('link', { name: 'Trang trước' })).toHaveAttribute('href', '/series')
    expect(within(secondPagination).queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(api.requestsTo('GET', seriesCollectionApiPath(1))).toHaveLength(1)
  })

  it('render_ShouldRequestPageFromUrl_WhenPageQueryIsGiven', async () => {
    const api = new StubApiServer()
      .on('GET', seriesCollectionApiPath(2), jsonResponse(200, seriesCollectionResponse([seriesSummary()], { page: 2 })))
      .install()

    renderAppRoute('/series?page=2')

    expect(await screen.findByText('Trang 3')).toBeInTheDocument()
    expect(api.requestsTo('GET', seriesCollectionApiPath(2))).toHaveLength(1)
  })

  it.each(['abc', '-1', '1.5', ''])('render_ShouldOpenFirstPage_WhenPageQueryIsMalformed (%j)', async (value) => {
    const api = new StubApiServer()
      .on('GET', seriesCollectionApiPath(0), jsonResponse(200, seriesCollectionResponse([seriesSummary()])))
      .install()

    renderAppRoute(`/series?page=${value}`)

    expect(await screen.findByRole('link', { name: 'Java Concurrency' })).toBeInTheDocument()
    expect(api.requestsTo('GET', seriesCollectionApiPath(0))).toHaveLength(1)
  })

  it('render_ShouldSayThereAreNoSeries_WhenFirstPageIsEmpty', async () => {
    new StubApiServer().on('GET', seriesCollectionApiPath(0), jsonResponse(200, seriesCollectionResponse([]))).install()

    renderAppRoute('/series')

    expect(await screen.findByText('Chưa có Series nào được công khai.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Series' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('render_ShouldOfferFirstPage_WhenPageIsBeyondLastOne', async () => {
    new StubApiServer()
      .on('GET', seriesCollectionApiPath(5), jsonResponse(200, seriesCollectionResponse([], { page: 5 })))
      .install()

    renderAppRoute('/series?page=5')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/series')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
  })

  it('retry_ShouldLoadList_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on(
        'GET',
        seriesCollectionApiPath(0),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, seriesCollectionResponse([seriesSummary()])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/series')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách Series')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Java Concurrency' })).toBeInTheDocument()
  })

  it('navigate_ShouldKeepNewPage_WhenAnOlderRequestFinishesLater', async () => {
    let finishFirst: (response: Response) => void = () => {}
    const slowFirstPage = new Promise<Response>((resolve) => {
      finishFirst = resolve
    })
    vi.stubGlobal('fetch', (input: RequestInfo | URL) => {
      const path = String(input)
      if (path === seriesCollectionApiPath(0)) {
        return slowFirstPage
      }
      if (path === seriesCollectionApiPath(1)) {
        return Promise.resolve(
          new Response(JSON.stringify(seriesCollectionResponse([SECOND_SERIES], { page: 1 })), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        )
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const user = userEvent.setup()
    renderAppRoute('/series', { previous: '/series?page=1' })

    await user.click(screen.getByRole('button', { name: 'Back (test)' }))
    expect(await screen.findByRole('link', { name: 'Spring Security' })).toBeInTheDocument()
    await act(async () => {
      finishFirst(
        new Response(JSON.stringify(seriesCollectionResponse([seriesSummary()])), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    })

    expect(screen.getByRole('link', { name: 'Spring Security' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Java Concurrency' })).toBeNull()
  })
})
