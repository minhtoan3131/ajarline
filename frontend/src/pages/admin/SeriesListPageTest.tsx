import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  givenSeriesList,
  seriesItem,
  seriesListApiPath,
  seriesListResponse,
  SERIES_ID,
} from '../../features/admin-learning/fixture/learningAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const OTHER_SERIES_ID = '9d3f5a21-7b48-4c6e-8e10-4a2b6c8d0f57'

describe('SeriesListPage', () => {
  it('render_ShouldListSeriesWithStatusAndLink_WhenSeriesExist', async () => {
    givenSeriesList(givenSignedInAdmin(new StubApiServer()), [
      seriesItem(),
      seriesItem({ id: OTHER_SERIES_ID, title: 'Học gRPC', slug: 'hoc-grpc', status: 'PUBLISHED' }),
    ]).install()

    renderAppRoute('/admin/series')

    const list = await screen.findByRole('list', { name: 'Danh sách Series' })
    expect(within(list).getByRole('link', { name: 'Học Spring Boot' })).toHaveAttribute('href', `/admin/series/${SERIES_ID}`)
    expect(within(list).getByText('Đã xuất bản')).toBeInTheDocument()
    expect(within(list).getByText('Bản nháp')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tạo Series' })).toHaveAttribute('href', '/admin/series/new')
    expect(screen.queryByRole('navigation', { name: 'Phân trang' })).toBeNull()
  })

  it('render_ShouldShowEmptyMessage_WhenFirstPageIsEmpty', async () => {
    givenSeriesList(givenSignedInAdmin(new StubApiServer()), []).install()

    renderAppRoute('/admin/series')

    expect(await screen.findByText('Chưa có Series nào.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tạo Series' })).toBeInTheDocument()
  })

  it('render_ShouldOfferFirstPage_WhenLaterPageIsEmpty', async () => {
    givenSeriesList(givenSignedInAdmin(new StubApiServer()), [], 3).install()

    renderAppRoute('/admin/series?page=3')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/admin/series')
  })

  it('render_ShouldLinkNextPage_WhenMoreSeriesExist', async () => {
    givenSeriesList(givenSignedInAdmin(new StubApiServer()), [seriesItem()], 0, true).install()

    renderAppRoute('/admin/series')

    expect(await screen.findByRole('link', { name: 'Trang sau' })).toHaveAttribute('href', '/admin/series?page=1')
  })

  it('retry_ShouldLoadAgain_WhenFirstRequestFails', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on(
        'GET',
        seriesListApiPath(0),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, seriesListResponse([seriesItem()])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/series')

    await user.click(await screen.findByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Học Spring Boot' })).toBeInTheDocument()
    expect(api.requestsTo('GET', seriesListApiPath(0))).toHaveLength(2)
  })
})
