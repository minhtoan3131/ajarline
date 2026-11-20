import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  newsCollectionApiPath,
  newsCollectionResponse,
  newsSummary,
} from '../features/news-reading/fixture/publicNewsFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const FIRST = newsSummary({ topics: [{ id: '3b1e7c52-8f4a-4d2b-9c61-0a5e2f7d8b94', slug: 'java', name: 'Java' }] })
const SECOND = newsSummary({
  id: '2c7e9a14-6b3d-4f80-9a25-d1e8f0b4c6a3',
  slug: 'java-26-ra-mat',
  title: 'Java 26 ra mắt',
  summary: 'Các thay đổi chính của bản mới.',
  topics: [],
  publishedAt: '2026-11-02T02:00:00Z',
})

describe('NewsIndexPage', () => {
  it('render_ShouldShowLoadingFirst_WhenIndexIsRequested', () => {
    new StubApiServer().on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([FIRST]))).install()

    renderAppRoute('/news')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách tin…')
  })

  it('render_ShouldKeepServerOrderAndLinkEachNews_WhenNewsExist', async () => {
    const api = new StubApiServer()
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([FIRST, SECOND])))
      .install()

    renderAppRoute('/news')

    expect(await screen.findByRole('heading', { level: 1, name: 'Tin công nghệ' })).toBeInTheDocument()
    const list = screen.getByRole('list', { name: 'Các tin' })
    expect(within(list).getAllByRole('heading', { level: 2 }).map((heading) => heading.textContent)).toEqual([
      'Spring Boot 4.1 ra mắt',
      'Java 26 ra mắt',
    ])
    expect(within(list).getByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toHaveAttribute(
      'href',
      '/news/spring-boot-4-1',
    )
    expect(within(list).getByRole('link', { name: 'Java 26 ra mắt' })).toHaveAttribute('href', '/news/java-26-ra-mat')
    expect(within(list).getByText('Các thay đổi chính của bản mới.')).toBeInTheDocument()
    expect(list).toHaveTextContent(/Xuất bản\s+4 tháng 11/)
    expect(list).toHaveTextContent(/Xuất bản\s+2 tháng 11/)
    const chips = within(list).getByRole('list', { name: 'Chủ đề của Spring Boot 4.1 ra mắt' })
    expect(within(chips).getByRole('link', { name: 'Java' })).toHaveAttribute('href', '/topics/java')
    expect(within(list).queryByRole('list', { name: 'Chủ đề của Java 26 ra mắt' })).toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
    expect(api.requests.map((request) => request.path)).toEqual([newsCollectionApiPath(0)])
  })

  it('render_ShouldLabelEveryRowWithTypeText_WhenNewsExist', async () => {
    new StubApiServer()
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([FIRST, SECOND])))
      .install()

    renderAppRoute('/news')

    await screen.findByRole('list', { name: 'Các tin' })
    expect(screen.getAllByText('Tin')).toHaveLength(2)
  })

  it('render_ShouldSayThereAreNoNews_WhenFirstPageIsEmpty', async () => {
    new StubApiServer().on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([]))).install()

    renderAppRoute('/news')

    expect(await screen.findByText('Chưa có tin nào được công khai.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Tin công nghệ' })).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Các tin' })).toBeNull()
  })

  it('render_ShouldOfferFirstPage_WhenPageAfterLastIsEmpty', async () => {
    new StubApiServer()
      .on('GET', newsCollectionApiPath(2), jsonResponse(200, newsCollectionResponse([], { page: 2 })))
      .install()

    renderAppRoute('/news?page=2')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/news')
    expect(screen.queryByText('Chưa có tin nào được công khai.')).toBeNull()
  })

  it('render_ShouldShowFailureWithRetryAndNoEmptyMessage_WhenServerFails', async () => {
    new StubApiServer()
      .on(
        'GET',
        newsCollectionApiPath(0),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, newsCollectionResponse([FIRST])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/news')

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('Không tải được danh sách tin do lỗi kết nối hoặc máy chủ.')
    expect(screen.queryByText(/Chưa có tin/)).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
  })

  it('navigate_ShouldLoadNextPageAndKeepFirstPageWithoutPageQuery_WhenNextIsClicked', async () => {
    const api = new StubApiServer()
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([FIRST], { hasMore: true })))
      .on('GET', newsCollectionApiPath(1), jsonResponse(200, newsCollectionResponse([SECOND], { page: 1 })))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/news')
    const firstPagination = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(firstPagination).toHaveTextContent('Trang 1')

    await user.click(within(firstPagination).getByRole('link', { name: 'Trang sau' }))

    expect(await screen.findByRole('link', { name: 'Java 26 ra mắt' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/news')
    expect(screen.getByTestId('search')).toHaveTextContent('?page=1')
    const secondPagination = screen.getByRole('navigation', { name: 'Phân trang' })
    expect(within(secondPagination).getByRole('link', { name: 'Trang trước' })).toHaveAttribute('href', '/news')
    expect(within(secondPagination).queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(api.requestsTo('GET', newsCollectionApiPath(1))).toHaveLength(1)
  })

  it.each(['abc', '-1', '1.5', ''])('render_ShouldOpenFirstPage_WhenPageQueryIsMalformed (%j)', async (value) => {
    const api = new StubApiServer()
      .on('GET', newsCollectionApiPath(0), jsonResponse(200, newsCollectionResponse([FIRST])))
      .install()

    renderAppRoute(`/news?page=${value}`)

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(api.requestsTo('GET', newsCollectionApiPath(0))).toHaveLength(1)
  })
})
