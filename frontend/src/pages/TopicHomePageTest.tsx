import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  publicSeries,
  seriesApiPath,
} from '../features/learning-navigation/fixture/publicLearningFixture'
import {
  contentItem,
  JAVA_TOPIC_REF,
  publicTopic,
  SPRING_TOPIC_REF,
  topicContentPath,
  topicContentResponse,
  topicDiscovery,
  topicPath,
  TOPICS_PATH,
} from '../features/topic-browsing/fixture/publicTopicFixture'
import {
  jsonResponse,
  problemResponse,
  StubApiServer,
  type StubResponse,
} from '../shared/api/testsupport/StubApiServer'

/** Stubs both Topic Home requests of the Java Topic for one page. */
function stubJavaTopic(page: number, content: StubResponse): StubApiServer {
  return new StubApiServer()
    .on('GET', topicPath('java'), jsonResponse(200, publicTopic()))
    .on('GET', topicContentPath('java', page), content)
}

function onePage(...items: ReturnType<typeof contentItem>[]): StubResponse {
  return jsonResponse(200, topicContentResponse(items))
}

describe('TopicHomePage', () => {
  it('render_ShouldShowLoadingFirst_WhenTopicIsRequested', () => {
    stubJavaTopic(0, onePage(contentItem())).install()

    renderAppRoute('/topics/java')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải chủ đề…')
  })

  it('render_ShouldShowTopicAndStandaloneArticles_WhenTopicIsPublic', async () => {
    const api = stubJavaTopic(0, onePage(contentItem())).install()

    renderAppRoute('/topics/java')

    expect(await screen.findByRole('heading', { level: 1, name: 'Java' })).toBeInTheDocument()
    expect(screen.getByText('Ngôn ngữ và nền tảng Java.')).toBeInTheDocument()
    const group = screen.getByRole('region', { name: 'Bài viết độc lập' })
    expect(within(group).getByRole('link', { name: 'Hiểu Protobuf với Java' })).toHaveAttribute(
      'href',
      '/articles/hieu-protobuf',
    )
    expect(within(group).getByText('Tóm tắt bài viết.')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { level: 2, name: 'Series' })).toBeNull()
    expect(screen.queryByRole('heading', { level: 2, name: 'Tin công nghệ' })).toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(api.requests.map((request) => request.path)).toEqual([topicPath('java'), topicContentPath('java', 0)])
  })

  it('render_ShouldLinkNewsAndPlaceItLast_WhenTopicHasNewsAndLearningContent', async () => {
    const series = contentItem({
      id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23',
      type: 'series',
      slug: 'java-concurrency',
      title: 'Java Concurrency',
    })
    const news = contentItem({
      id: '6b2d8e40-1a97-4c35-b8f2-3d5e7a9c0b14',
      type: 'news',
      slug: 'spring-boot-4-1',
      title: 'Spring Boot 4.1 ra mắt',
      summary: 'Tin ngắn.',
    })
    stubJavaTopic(0, onePage(series, contentItem(), news)).install()

    renderAppRoute('/topics/java')

    await screen.findByRole('heading', { level: 1, name: 'Java' })
    expect(screen.getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)).toEqual([
      'Series',
      'Bài viết độc lập',
      'Tin công nghệ',
    ])
    const group = screen.getByRole('region', { name: 'Tin công nghệ' })
    expect(within(group).getByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toHaveAttribute(
      'href',
      '/news/spring-boot-4-1',
    )
    expect(within(group).queryByText('Tin')).toBeNull()
  })

  it('render_ShouldShowOnlyOtherTopics_WhenItemHasSeveralTopics', async () => {
    stubJavaTopic(0, onePage(contentItem({ topics: [JAVA_TOPIC_REF, SPRING_TOPIC_REF] }))).install()

    renderAppRoute('/topics/java')

    const chips = await screen.findByRole('list', { name: 'Chủ đề khác của Hiểu Protobuf với Java' })
    const links = within(chips).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Spring Boot'])
    expect(links[0]).toHaveAttribute('href', '/topics/spring-boot')
  })

  it('render_ShouldShowGenericNotFound_WhenTopicIsNotPublic', async () => {
    new StubApiServer()
      .on('GET', topicPath('an-di'), problemResponse(404, 'CONTENT_NOT_FOUND'))
      .on('GET', topicContentPath('an-di', 0), problemResponse(404, 'CONTENT_NOT_FOUND'))
      .install()

    renderAppRoute('/topics/an-di')

    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('retry_ShouldLoadTopic_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on('GET', topicPath('java'), problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, publicTopic()))
      .on('GET', topicContentPath('java', 0), onePage(contentItem()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics/java')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được chủ đề')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java' })).toBeInTheDocument()
  })

  it('navigate_ShouldLoadNextPage_WhenNextIsClicked', async () => {
    const secondPageItem = contentItem({
      id: '2c7e9a14-6b3d-4f80-9a25-d1e8f0b4c6a3',
      slug: 'hieu-grpc',
      title: 'Hiểu gRPC',
    })
    const api = stubJavaTopic(0, jsonResponse(200, topicContentResponse([contentItem()], { hasMore: true })))
      .on('GET', topicContentPath('java', 1), jsonResponse(200, topicContentResponse([secondPageItem], { page: 1 })))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics/java')
    const firstPagination = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(firstPagination).toHaveTextContent('Trang 1')
    expect(within(firstPagination).queryByRole('link', { name: 'Trang trước' })).toBeNull()

    await user.click(within(firstPagination).getByRole('link', { name: 'Trang sau' }))

    expect(await screen.findByRole('link', { name: 'Hiểu gRPC' })).toBeInTheDocument()
    const secondPagination = screen.getByRole('navigation', { name: 'Phân trang' })
    expect(secondPagination).toHaveTextContent('Trang 2')
    expect(within(secondPagination).getByRole('link', { name: 'Trang trước' })).toHaveAttribute('href', '/topics/java')
    expect(within(secondPagination).queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(api.requestsTo('GET', topicContentPath('java', 1))).toHaveLength(1)
  })

  it('render_ShouldRequestPageFromUrl_WhenPageQueryIsGiven', async () => {
    const api = stubJavaTopic(2, jsonResponse(200, topicContentResponse([contentItem()], { page: 2 }))).install()

    renderAppRoute('/topics/java?page=2')

    expect(await screen.findByText('Trang 3')).toBeInTheDocument()
    expect(api.requestsTo('GET', topicContentPath('java', 2))).toHaveLength(1)
  })

  it('render_ShouldOpenFirstPage_WhenPageQueryIsMalformed', async () => {
    const api = stubJavaTopic(0, onePage(contentItem())).install()

    renderAppRoute('/topics/java?page=abc')

    expect(await screen.findByRole('heading', { level: 1, name: 'Java' })).toBeInTheDocument()
    expect(api.requestsTo('GET', topicContentPath('java', 0))).toHaveLength(1)
  })

  it('render_ShouldOfferFirstPage_WhenPageIsBeyondLastOne', async () => {
    stubJavaTopic(5, jsonResponse(200, topicContentResponse([], { page: 5 }))).install()

    renderAppRoute('/topics/java?page=5')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/topics/java')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
  })

  it('navigate_ShouldOpenTopicHome_WhenTopicIsClickedInIndex', async () => {
    stubJavaTopic(0, onePage(contentItem()))
      .on('GET', TOPICS_PATH, jsonResponse(200, topicDiscovery(JAVA_TOPIC_REF)))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics')
    const list = await screen.findByRole('list', { name: 'Các chủ đề' })

    await user.click(within(list).getByRole('link', { name: 'Java' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/topics/java')
    // The header keeps the Topics area marked while a Topic Home is open.
    const header = screen.getByRole('navigation', { name: 'Điều hướng chính' })
    expect(within(header).getByRole('link', { name: 'Chủ đề' })).toHaveAttribute('aria-current', 'page')
  })

  it('navigate_ShouldOpenArticle_WhenArticleIsClicked', async () => {
    stubJavaTopic(0, onePage(contentItem()))
      .on(
        'GET',
        '/api/v1/articles/hieu-protobuf',
        jsonResponse(200, {
          id: '9d4f2a61-7c3e-4b85-a0d2-6e1f8c5b3a47',
          type: 'article',
          slug: 'hieu-protobuf',
          title: 'Hiểu Protobuf với Java',
          summary: 'Tóm tắt bài viết.',
          bodyMarkdown: 'Nội dung bài.',
          topics: [JAVA_TOPIC_REF],
          series: null,
          module: null,
          previousArticle: null,
          nextArticle: null,
          publishedAt: '2026-11-01T02:00:00Z',
          updatedAt: '2026-11-01T02:00:00Z',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics/java')

    await user.click(await screen.findByRole('link', { name: 'Hiểu Protobuf với Java' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/articles/hieu-protobuf')
  })

  it('render_ShouldLinkSeriesAndModuleItemsWithContext_WhenTopicHasThem', async () => {
    stubJavaTopic(
      0,
      onePage(
        contentItem({
          id: 'series-1',
          type: 'series',
          slug: 'java-concurrency',
          title: 'Java Concurrency',
          summary: 'Học xử lý đồng thời trong Java.',
        }),
        contentItem({
          id: 'module-1',
          type: 'module',
          slug: 'thread-co-ban',
          title: 'Thread cơ bản',
          summary: 'Làm quen với Thread.',
          context: {
            series: { id: 'series-1', slug: 'java-concurrency', title: 'Java Concurrency' },
            module: null,
          },
        }),
      ),
    ).install()

    renderAppRoute('/topics/java')

    const seriesGroup = await screen.findByRole('region', { name: 'Series' })
    expect(within(seriesGroup).getByRole('link', { name: 'Java Concurrency' })).toHaveAttribute(
      'href',
      '/series/java-concurrency',
    )
    const inSeries = screen.getByRole('region', { name: 'Trong Series' })
    expect(within(inSeries).getByRole('link', { name: 'Thread cơ bản' })).toHaveAttribute(
      'href',
      '/modules/thread-co-ban',
    )
    expect(within(inSeries).getByText('Java Concurrency')).toBeInTheDocument()
  })

  it('navigate_ShouldOpenSeries_WhenSeriesItemIsClicked', async () => {
    stubJavaTopic(
      0,
      onePage(
        contentItem({
          id: 'series-1',
          type: 'series',
          slug: 'java-concurrency',
          title: 'Java Concurrency',
          summary: 'Học xử lý đồng thời trong Java.',
        }),
      ),
    )
      .on('GET', seriesApiPath('java-concurrency'), jsonResponse(200, publicSeries()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics/java')

    await user.click(await screen.findByRole('link', { name: 'Java Concurrency' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/series/java-concurrency')
  })
})
