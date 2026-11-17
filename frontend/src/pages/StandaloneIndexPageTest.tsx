import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  articleCollectionResponse,
  articleSummary,
  standaloneArticlesPath,
} from '../features/article-browsing/fixture/articleCollectionFixture'
import { JAVA_TOPIC_REF, SPRING_TOPIC_REF } from '../features/topic-browsing/fixture/publicTopicFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const SECOND_ARTICLE = articleSummary({
  id: '2c7e9a14-6b3d-4f80-9a25-d1e8f0b4c6a3',
  slug: 'hieu-grpc',
  title: 'Hiểu gRPC',
  summary: 'Tóm tắt gRPC.',
  topics: [],
})

describe('StandaloneIndexPage', () => {
  it('render_ShouldShowLoadingFirst_WhenIndexIsRequested', () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .install()

    renderAppRoute('/articles?standalone=true')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách bài viết…')
  })

  it('render_ShouldListArticlesWithTopics_WhenStandaloneArticlesExist', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        standaloneArticlesPath(0),
        jsonResponse(
          200,
          articleCollectionResponse([articleSummary({ topics: [JAVA_TOPIC_REF, SPRING_TOPIC_REF] }), SECOND_ARTICLE]),
        ),
      )
      .install()

    renderAppRoute('/articles?standalone=true')

    expect(await screen.findByRole('heading', { level: 1, name: 'Bài viết độc lập' })).toBeInTheDocument()
    const list = screen.getByRole('list', { name: 'Các bài viết độc lập' })
    expect(within(list).getByRole('link', { name: 'Hiểu Protobuf với Java' })).toHaveAttribute(
      'href',
      '/articles/hieu-protobuf',
    )
    expect(within(list).getByText('Tóm tắt bài viết.')).toBeInTheDocument()
    expect(within(list).getByText('Tóm tắt gRPC.')).toBeInTheDocument()
    const chips = within(list).getByRole('list', { name: 'Chủ đề của Hiểu Protobuf với Java' })
    expect(within(chips).getAllByRole('link').map((link) => link.textContent)).toEqual(['Java', 'Spring Boot'])
    expect(within(list).getByRole('link', { name: 'Hiểu gRPC' })).toBeInTheDocument()
    expect(within(list).queryByRole('list', { name: 'Chủ đề của Hiểu gRPC' })).toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(api.requests.map((request) => request.path)).toEqual([standaloneArticlesPath(0)])
  })

  it.each(['/articles', '/articles?standalone=false', '/articles?standalone=abc'])(
    'render_ShouldRedirectToStandaloneIndexWithoutExtraRequest_WhenStandaloneIsMissingOrWrong (%s)',
    async (url) => {
      const api = new StubApiServer()
        .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
        .install()

      renderAppRoute(url)

      expect(await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
      expect(screen.getByTestId('location')).toHaveTextContent('/articles')
      expect(screen.getByTestId('search')).toHaveTextContent('?standalone=true')
      expect(api.requests.map((request) => request.path)).toEqual([standaloneArticlesPath(0)])
    },
  )

  it('navigate_ShouldLeaveHistoryBeforeIndex_WhenBackIsPressedAfterRedirect', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles', { previous: '/nowhere' })
    await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' })

    await user.click(screen.getByRole('button', { name: 'Back (test)' }))

    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/nowhere')
  })

  it('navigate_ShouldLoadNextPage_WhenNextIsClicked', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        standaloneArticlesPath(0),
        jsonResponse(200, articleCollectionResponse([articleSummary()], { hasMore: true })),
      )
      .on('GET', standaloneArticlesPath(1), jsonResponse(200, articleCollectionResponse([SECOND_ARTICLE], { page: 1 })))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles?standalone=true')
    const firstPagination = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(firstPagination).toHaveTextContent('Trang 1')
    expect(within(firstPagination).queryByRole('link', { name: 'Trang trước' })).toBeNull()

    await user.click(within(firstPagination).getByRole('link', { name: 'Trang sau' }))

    expect(await screen.findByRole('link', { name: 'Hiểu gRPC' })).toBeInTheDocument()
    expect(screen.getByTestId('search')).toHaveTextContent('?standalone=true&page=1')
    const secondPagination = screen.getByRole('navigation', { name: 'Phân trang' })
    expect(secondPagination).toHaveTextContent('Trang 2')
    expect(within(secondPagination).getByRole('link', { name: 'Trang trước' })).toHaveAttribute(
      'href',
      '/articles?standalone=true',
    )
    expect(within(secondPagination).queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(api.requestsTo('GET', standaloneArticlesPath(1))).toHaveLength(1)
  })

  it('render_ShouldRequestPageFromUrl_WhenPageQueryIsGiven', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        standaloneArticlesPath(2),
        jsonResponse(200, articleCollectionResponse([articleSummary()], { page: 2 })),
      )
      .install()

    renderAppRoute('/articles?standalone=true&page=2')

    expect(await screen.findByText('Trang 3')).toBeInTheDocument()
    expect(api.requestsTo('GET', standaloneArticlesPath(2))).toHaveLength(1)
  })

  it('render_ShouldOpenFirstPage_WhenPageQueryIsMalformed', async () => {
    const api = new StubApiServer()
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .install()

    renderAppRoute('/articles?standalone=true&page=abc')

    expect(await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
    expect(api.requestsTo('GET', standaloneArticlesPath(0))).toHaveLength(1)
  })

  it('render_ShouldSayThereAreNoArticles_WhenFirstPageIsEmpty', async () => {
    new StubApiServer().on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([]))).install()

    renderAppRoute('/articles?standalone=true')

    expect(await screen.findByText('Chưa có bài viết độc lập nào được công khai.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Bài viết độc lập' })).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
  })

  it('render_ShouldOfferFirstPage_WhenPageIsBeyondLastOne', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(5), jsonResponse(200, articleCollectionResponse([], { page: 5 })))
      .install()

    renderAppRoute('/articles?standalone=true&page=5')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/articles?standalone=true')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
  })

  it('retry_ShouldLoadList_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on(
        'GET',
        standaloneArticlesPath(0),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, articleCollectionResponse([articleSummary()])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles?standalone=true')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách bài viết')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
  })

  it('navigate_ShouldKeepNewPage_WhenAnOlderRequestFinishesLater', async () => {
    let finishFirst: (response: Response) => void = () => {}
    const slowFirstPage = new Promise<Response>((resolve) => {
      finishFirst = resolve
    })
    vi.stubGlobal('fetch', (input: RequestInfo | URL) => {
      const path = String(input)
      if (path === standaloneArticlesPath(0)) {
        return slowFirstPage
      }
      if (path === standaloneArticlesPath(1)) {
        return Promise.resolve(
          new Response(JSON.stringify(articleCollectionResponse([SECOND_ARTICLE], { page: 1 })), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
        )
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const user = userEvent.setup()
    renderAppRoute('/articles?standalone=true', { previous: '/articles?standalone=true&page=1' })

    await user.click(screen.getByRole('button', { name: 'Back (test)' }))
    expect(await screen.findByRole('link', { name: 'Hiểu gRPC' })).toBeInTheDocument()
    await act(async () => {
      finishFirst(
        new Response(JSON.stringify(articleCollectionResponse([articleSummary()])), {
          status: 200,
          headers: { 'Content-Type': 'application/json' },
        }),
      )
    })

    expect(screen.getByRole('link', { name: 'Hiểu gRPC' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Hiểu Protobuf với Java' })).toBeNull()
  })

  it('navigate_ShouldOpenArticle_WhenArticleIsClicked', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .on(
        'GET',
        '/api/v1/articles/hieu-protobuf',
        jsonResponse(200, {
          ...articleSummary(),
          bodyMarkdown: 'Nội dung bài.',
          series: null,
          module: null,
          previousArticle: null,
          nextArticle: null,
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles?standalone=true')

    await user.click(await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/articles/hieu-protobuf')
  })
})
