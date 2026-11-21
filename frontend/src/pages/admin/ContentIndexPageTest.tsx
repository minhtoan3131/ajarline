import { act, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  contentIndexApiPath,
  contentIndexResponse,
  contentItem,
  givenContentIndex,
  holdRequestsTo,
  ITEM_IDS,
  OTHER_ITEM_ID,
} from '../../features/admin-content/fixture/contentIndexFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

function givenSession(): StubApiServer {
  return givenSignedInAdmin(new StubApiServer())
}

const NEWS_DELETE_PATH = `/api/admin/v1/news/${ITEM_IDS.news}`
const SERIES_DELETE_PATH = `/api/admin/v1/series/${ITEM_IDS.series}`

function rowOf(list: HTMLElement, title: string): HTMLElement {
  const row = within(list).getByRole('link', { name: title }).closest('li')
  if (row === null) {
    throw new Error(`No row for ${title}`)
  }
  return row
}

describe('ContentIndexPage', () => {
  it('render_ShouldListEveryTypeWithStatusAndEditorLink_WhenItemsExist', async () => {
    givenContentIndex(givenSession(), [
      contentItem({ id: ITEM_IDS.article, type: 'article', title: 'Bài về Spring', slug: 'bai-ve-spring', status: 'PUBLISHED' }),
      contentItem({ id: ITEM_IDS.module, type: 'module', title: 'Module Web', slug: 'module-web' }),
      contentItem({ id: ITEM_IDS.series, type: 'series', title: 'Series Java', slug: 'series-java' }),
      contentItem({ id: ITEM_IDS.news, type: 'news', title: 'Tin mới', slug: 'tin-moi', status: 'UNPUBLISHED' }),
    ]).install()

    renderAppRoute('/admin/content')

    const list = await screen.findByRole('list', { name: 'Danh sách nội dung' })
    expect(within(list).getByRole('link', { name: 'Bài về Spring' })).toHaveAttribute('href', `/admin/articles/${ITEM_IDS.article}`)
    expect(within(list).getByRole('link', { name: 'Module Web' })).toHaveAttribute('href', `/admin/modules/${ITEM_IDS.module}`)
    expect(within(list).getByRole('link', { name: 'Series Java' })).toHaveAttribute('href', `/admin/series/${ITEM_IDS.series}`)
    expect(within(list).getByRole('link', { name: 'Tin mới' })).toHaveAttribute('href', `/admin/news/${ITEM_IDS.news}`)
    expect(rowOf(list, 'Bài về Spring')).toHaveTextContent('Bài viết')
    expect(rowOf(list, 'Bài về Spring')).toHaveTextContent('Đã xuất bản')
    expect(rowOf(list, 'Module Web')).toHaveTextContent('Bản nháp')
    expect(rowOf(list, 'Tin mới')).toHaveTextContent('Đã gỡ xuất bản')
    expect(rowOf(list, 'Tin mới')).toHaveTextContent('tin-moi')
  })

  it('render_ShouldAskServerWithUrlValues_WhenUrlHasFilters', async () => {
    const api = givenContentIndex(givenSession(), [contentItem()], {
      filters: 'types=news&statuses=DRAFT&q=spring',
      page: 2,
    }).install()

    renderAppRoute('/admin/content?types=news&statuses=DRAFT&q=spring&page=2')

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(api.requestsTo('GET', contentIndexApiPath('types=news&statuses=DRAFT&q=spring', 2))).toHaveLength(1)
    expect(screen.getByRole('checkbox', { name: 'Tin' })).toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Bài viết' })).not.toBeChecked()
    expect(screen.getByRole('checkbox', { name: 'Bản nháp' })).toBeChecked()
    expect(screen.getByLabelText('Tìm theo tiêu đề hoặc slug')).toHaveValue('spring')
  })

  it('render_ShouldIgnoreUnknownValues_WhenUrlWasEditedByHand', async () => {
    const api = givenContentIndex(givenSession(), [contentItem()], { filters: 'types=news' }).install()

    renderAppRoute('/admin/content?types=video,news&statuses=ARCHIVED&page=abc')

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(api.requestsTo('GET', contentIndexApiPath('types=news'))).toHaveLength(1)
  })

  it('filter_ShouldChangeUrlAndGoToFirstPage_WhenCheckboxIsClicked', async () => {
    const api = givenContentIndex(
      givenContentIndex(givenSession(), [contentItem()], { filters: 'types=news', page: 2 }),
      [contentItem({ id: OTHER_ITEM_ID, type: 'article', title: 'Bài mới' })],
      { filters: 'types=article,news' },
    ).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content?types=news&page=2')
    await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })

    await user.click(screen.getByRole('checkbox', { name: 'Bài viết' }))

    expect(await screen.findByRole('link', { name: 'Bài mới' })).toBeInTheDocument()
    expect(screen.getByTestId('search')).toHaveTextContent('?types=article,news')
    expect(api.requestsTo('GET', contentIndexApiPath('types=article,news'))).toHaveLength(1)
  })

  it('search_ShouldChangeUrl_WhenSearchFormIsSubmitted', async () => {
    const api = givenContentIndex(
      givenContentIndex(givenSession(), [contentItem()]),
      [contentItem({ title: 'Kết quả tìm' })],
      { filters: 'q=spring' },
    ).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })

    await user.type(screen.getByLabelText('Tìm theo tiêu đề hoặc slug'), 'spring{Enter}')

    expect(await screen.findByRole('link', { name: 'Kết quả tìm' })).toBeInTheDocument()
    expect(screen.getByTestId('search')).toHaveTextContent('?q=spring')
    expect(api.requestsTo('GET', contentIndexApiPath('q=spring'))).toHaveLength(1)
  })

  it('render_ShouldLinkOnlyNextPage_WhenFirstPageHasMore', async () => {
    givenContentIndex(givenSession(), [contentItem()], { hasMore: true }).install()

    renderAppRoute('/admin/content')

    expect(await screen.findByRole('link', { name: 'Trang sau' })).toHaveAttribute('href', '/admin/content?page=1')
    expect(screen.queryByRole('link', { name: 'Trang trước' })).toBeNull()
  })

  it('render_ShouldLinkOnlyPreviousPageAndShowNoTotal_WhenLastPageIsShown', async () => {
    givenContentIndex(givenSession(), [contentItem()], { page: 2 }).install()

    renderAppRoute('/admin/content?page=2')

    const pagination = await screen.findByRole('navigation', { name: 'Phân trang' })
    expect(within(pagination).getByRole('link', { name: 'Trang trước' })).toHaveAttribute('href', '/admin/content?page=1')
    expect(within(pagination).queryByRole('link', { name: 'Trang sau' })).toBeNull()
    expect(pagination).toHaveTextContent('Trang 3')
    expect(pagination).not.toHaveTextContent(/\d\s*\/\s*\d/)
  })

  it('render_ShouldSayStoreIsEmpty_WhenNothingExistsAndNothingIsFiltered', async () => {
    givenContentIndex(givenSession(), []).install()

    renderAppRoute('/admin/content')

    expect(await screen.findByText('Chưa có nội dung nào.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Bỏ lọc' })).toBeNull()
  })

  it('render_ShouldOfferClearingFilters_WhenNoItemMatches', async () => {
    const api = givenContentIndex(
      givenContentIndex(givenSession(), [], { filters: 'q=zzz' }),
      [contentItem()],
    ).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content?q=zzz')

    expect(await screen.findByText('Không có nội dung nào khớp bộ lọc.')).toBeInTheDocument()
    expect(screen.queryByText('Chưa có nội dung nào.')).toBeNull()
    expect(screen.getByLabelText('Tìm theo tiêu đề hoặc slug')).toHaveValue('zzz')

    await user.click(screen.getByRole('button', { name: 'Bỏ lọc' }))

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(screen.getByTestId('search')).toBeEmptyDOMElement()
    expect(screen.getByLabelText('Tìm theo tiêu đề hoặc slug')).toHaveValue('')
    expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(1)
  })

  it('render_ShouldOfferFirstPage_WhenPageIsAfterTheLastOne', async () => {
    givenContentIndex(givenSession(), [], { page: 3 }).install()

    renderAppRoute('/admin/content?page=3')

    expect(await screen.findByText('Trang này không còn nội dung.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Về trang đầu' })).toHaveAttribute('href', '/admin/content')
  })

  it('render_ShouldShowFailureNotEmptyMessage_WhenRequestFails', async () => {
    givenSession().on('GET', contentIndexApiPath(), problemResponse(500, 'INTERNAL_ERROR')).install()

    renderAppRoute('/admin/content')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách nội dung')
    expect(screen.queryByText('Chưa có nội dung nào.')).toBeNull()
  })

  it('retry_ShouldLoadAgain_WhenFirstRequestFails', async () => {
    const api = givenSession()
      .on(
        'GET',
        contentIndexApiPath(),
        problemResponse(500, 'INTERNAL_ERROR'),
        jsonResponse(200, contentIndexResponse([contentItem()])),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')

    await user.click(await screen.findByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('link', { name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(2)
  })

  it('render_ShouldShowLatestAnswer_WhenEarlierAnswerArrivesLast', async () => {
    const api = givenContentIndex(
      givenContentIndex(givenSession(), [contentItem({ title: 'Kết quả cũ' })], { filters: 'types=news' }),
      [contentItem({ id: OTHER_ITEM_ID, title: 'Kết quả mới' })],
      { filters: 'types=article,news' },
    ).install()
    const releaseFirst = holdRequestsTo('GET', contentIndexApiPath('types=news'))
    const user = userEvent.setup()
    renderAppRoute('/admin/content?types=news')
    await screen.findByText('Đang tải danh sách nội dung…')

    await user.click(screen.getByRole('checkbox', { name: 'Bài viết' }))
    expect(await screen.findByRole('link', { name: 'Kết quả mới' })).toBeInTheDocument()
    releaseFirst()
    await waitFor(() => expect(api.requestsTo('GET', contentIndexApiPath('types=news'))).toHaveLength(1))
    await act(async () => {
      await new Promise((resolve) => setTimeout(resolve, 0))
    })

    expect(screen.getByRole('link', { name: 'Kết quả mới' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Kết quả cũ' })).toBeNull()
  })

  it('create_ShouldOfferEveryType_WhenNewMenuIsOpened', async () => {
    givenContentIndex(givenSession(), []).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')

    await user.click(await screen.findByRole('button', { name: 'Tạo mới' }))

    const menu = screen.getByRole('list', { name: 'Chọn loại nội dung' })
    expect(within(menu).getByRole('link', { name: 'Bài viết' })).toHaveAttribute('href', '/admin/articles/new')
    expect(within(menu).getByRole('link', { name: 'Module' })).toHaveAttribute('href', '/admin/modules/new')
    expect(within(menu).getByRole('link', { name: 'Series' })).toHaveAttribute('href', '/admin/series/new')
    expect(within(menu).getByRole('link', { name: 'Tin' })).toHaveAttribute('href', '/admin/news/new')
  })

  it('render_ShouldShowDeleteOnlyOnDrafts_WhenItemsHaveDifferentStatus', async () => {
    givenContentIndex(givenSession(), [
      contentItem({ title: 'Bản nháp A' }),
      contentItem({ id: OTHER_ITEM_ID, title: 'Đã đăng B', status: 'PUBLISHED' }),
    ]).install()

    renderAppRoute('/admin/content')

    const list = await screen.findByRole('list', { name: 'Danh sách nội dung' })
    expect(within(rowOf(list, 'Bản nháp A')).getByRole('button', { name: 'Xóa Bản nháp A' })).toBeInTheDocument()
    expect(within(rowOf(list, 'Đã đăng B')).queryByRole('button', { name: /^Xóa/ })).toBeNull()
  })

  it('delete_ShouldSendRowRevisionAndReadListAgain_WhenServerReturnsNoContent', async () => {
    const published = contentItem({ id: OTHER_ITEM_ID, title: 'Đã đăng B', status: 'PUBLISHED' })
    const api = givenSession()
      .on(
        'GET',
        contentIndexApiPath(),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A', revision: 4 }), published])),
        jsonResponse(200, contentIndexResponse([published])),
      )
      .on('DELETE', NEWS_DELETE_PATH, { status: 204 })
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await user.click(await screen.findByRole('button', { name: 'Xóa Bản nháp A' }))
    expect(api.requestsTo('DELETE', NEWS_DELETE_PATH)).toHaveLength(0)

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByText('Đã xóa “Bản nháp A”.')).toBeInTheDocument()
    await waitFor(() => expect(screen.queryByRole('link', { name: 'Bản nháp A' })).toBeNull())
    expect(screen.getByRole('link', { name: 'Đã đăng B' })).toBeInTheDocument()
    expect(api.requestsTo('DELETE', NEWS_DELETE_PATH)[0].headers.get('If-Match')).toBe('"4"')
    expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(2)
  })

  it('delete_ShouldExplainChildrenAndKeepRow_WhenSeriesStillHasModules', async () => {
    const api = givenContentIndex(givenSession(), [
      contentItem({ id: ITEM_IDS.series, type: 'series', title: 'Series Java' }),
    ])
      .on('DELETE', SERIES_DELETE_PATH, problemResponse(409, 'CONTENT_IN_USE'))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await user.click(await screen.findByRole('button', { name: 'Xóa Series Java' }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('còn Module')
    expect(screen.getByRole('link', { name: 'Series Java' })).toBeInTheDocument()
    expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(1)
  })

  it('delete_ShouldReadListAgainAndTellToUnpublish_WhenItemWasPublishedElsewhere', async () => {
    const api = givenSession()
      .on(
        'GET',
        contentIndexApiPath(),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A' })])),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A', status: 'PUBLISHED', revision: 2 })])),
      )
      .on('DELETE', NEWS_DELETE_PATH, problemResponse(409, 'CONTENT_ALREADY_EVER_PUBLISHED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await user.click(await screen.findByRole('button', { name: 'Xóa Bản nháp A' }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('gỡ xuất bản')
    await waitFor(() =>
      expect(screen.getByRole('link', { name: 'Bản nháp A' }).closest('li')).toHaveTextContent('Đã xuất bản'),
    )
    expect(screen.queryByRole('button', { name: 'Xóa Bản nháp A' })).toBeNull()
    expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(2)
  })

  it('delete_ShouldReadListAgainAndUseNewRevision_WhenRevisionIsStale', async () => {
    const api = givenSession()
      .on(
        'GET',
        contentIndexApiPath(),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A', revision: 1 })])),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A', revision: 5 })])),
      )
      .on('DELETE', NEWS_DELETE_PATH, problemResponse(412, 'REVISION_CONFLICT'), { status: 204 })
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await user.click(await screen.findByRole('button', { name: 'Xóa Bản nháp A' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('vừa được thay đổi')
    await waitFor(() => expect(api.requestsTo('GET', contentIndexApiPath())).toHaveLength(2))

    await user.click(await screen.findByRole('button', { name: 'Xóa Bản nháp A' }))
    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    await waitFor(() => expect(api.requestsTo('DELETE', NEWS_DELETE_PATH)).toHaveLength(2))
    expect(api.requestsTo('DELETE', NEWS_DELETE_PATH).map((request) => request.headers.get('If-Match'))).toEqual([
      '"1"',
      '"5"',
    ])
  })

  it('delete_ShouldReadListAgainAndSayItemIsGone_WhenAnotherTabDeletedIt', async () => {
    givenSession()
      .on(
        'GET',
        contentIndexApiPath(),
        jsonResponse(200, contentIndexResponse([contentItem({ title: 'Bản nháp A' })])),
        jsonResponse(200, contentIndexResponse([])),
      )
      .on('DELETE', NEWS_DELETE_PATH, problemResponse(404, 'CONTENT_NOT_FOUND'))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/content')
    await user.click(await screen.findByRole('button', { name: 'Xóa Bản nháp A' }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('không còn nữa')
    expect(await screen.findByText('Chưa có nội dung nào.')).toBeInTheDocument()
  })
})
