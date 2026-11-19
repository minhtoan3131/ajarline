import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  givenNewsList,
  newsItem,
  newsListApiPath,
  newsListResponse,
  NEWS_ID,
} from '../../features/admin-news/fixture/newsAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const OTHER_NEWS_ID = '2c7e9a41-5d38-4b16-9f0a-8e1b3d5f7a62'

describe('NewsListPage', () => {
  it('render_ShouldListNewsWithStatusAndLink_WhenNewsExist', async () => {
    givenNewsList(givenSignedInAdmin(new StubApiServer()), [
      newsItem(),
      newsItem({ id: OTHER_NEWS_ID, title: 'gRPC 2.0 phát hành', slug: 'grpc-2-0', status: 'PUBLISHED' }),
    ]).install()

    renderAppRoute('/admin/news')

    const list = await screen.findByRole('list', { name: 'Danh sách tin' })
    expect(within(list).getByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toHaveAttribute('href', `/admin/news/${NEWS_ID}`)
    expect(within(list).getByText('Đã xuất bản')).toBeInTheDocument()
    expect(within(list).getByText('Bản nháp')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tạo tin' })).toHaveAttribute('href', '/admin/news/new')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
  })

  it('render_ShouldShowEmptyMessage_WhenFirstPageIsEmpty', async () => {
    givenNewsList(givenSignedInAdmin(new StubApiServer()), []).install()

    renderAppRoute('/admin/news')

    expect(await screen.findByText('Chưa có tin nào.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tạo tin' })).toBeInTheDocument()
  })

  it('render_ShouldOfferFirstPage_WhenLaterPageIsEmpty', async () => {
    givenNewsList(givenSignedInAdmin(new StubApiServer()), [], 3).install()

    renderAppRoute('/admin/news?page=3')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/admin/news')
  })

  it('render_ShouldLinkNextPage_WhenMoreNewsExist', async () => {
    givenNewsList(givenSignedInAdmin(new StubApiServer()), [newsItem()], 0, true).install()

    renderAppRoute('/admin/news')

    expect(await screen.findByRole('link', { name: 'Trang sau' })).toHaveAttribute('href', '/admin/news?page=1')
  })

  it('render_ShouldShowFailureNotEmptyList_WhenRequestFails', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', newsListApiPath(0), problemResponse(500, 'INTERNAL_ERROR')).install()

    renderAppRoute('/admin/news')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách tin')
    expect(screen.queryByText('Chưa có tin nào.')).toBeNull()
  })

  it('retry_ShouldLoadAgain_WhenFirstRequestFails', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on(
        'GET',
        newsListApiPath(0),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, newsListResponse([newsItem()])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/news')

    await user.click(await screen.findByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(api.requestsTo('GET', newsListApiPath(0))).toHaveLength(2)
  })
})
