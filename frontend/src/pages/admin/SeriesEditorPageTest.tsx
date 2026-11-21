import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN } from '../../features/admin-auth/fixture/adminSessionFixture'
import { CONTENT_INDEX_PATH } from '../../features/admin-content/contentRoutes'
import { givenContentIndex } from '../../features/admin-content/fixture/contentIndexFixture'
import {
  draftSeries,
  givenSeriesEditorSession,
  MODULE_ID,
  moduleChild,
  OTHER_MODULE_ID,
  publicationOfSeries,
  SERIES_API_PATH,
  SERIES_ID,
  SERIES_ORDER_PATH,
  SERIES_PATH,
  seriesHeaders,
  seriesOrderResponse,
  seriesPublishPath,
} from '../../features/admin-learning/fixture/learningAdminFixture'
import type { SeriesAdminDetail } from '../../features/admin-learning/model/series'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/series/${SERIES_ID}`
const PUBLISHED_AT = '2026-11-18T03:00:00Z'

function givenLoaded(series: SeriesAdminDetail): StubApiServer {
  return givenSeriesEditorSession(new StubApiServer()).on('GET', SERIES_PATH, jsonResponse(200, series, seriesHeaders(series)))
}

function publishedSeries(): SeriesAdminDetail {
  return draftSeries({ status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT })
}

/** Holds back every request with this method until the returned function is called. Install the stub first. */
function holdRequests(method: string): () => void {
  const respond = globalThis.fetch
  let release: () => void = () => undefined
  const gate = new Promise<void>((resolve) => {
    release = resolve
  })
  vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
    if ((init?.method ?? 'GET') === method) {
      await gate
    }
    return respond(input, init)
  })
  return release
}

describe('SeriesEditorPage', () => {
  it('save_ShouldCreateDraftAndReplaceUrl_WhenNewSeriesIsSaved', async () => {
    const created = draftSeries()
    const api = givenContentIndex(
      givenSeriesEditorSession(new StubApiServer())
        .on('POST', SERIES_API_PATH, jsonResponse(201, created, { ...seriesHeaders(created), Location: SERIES_PATH }))
        .on('GET', SERIES_PATH, jsonResponse(200, created, seriesHeaders(created))),
      [],
    ).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/series/new', { previous: CONTENT_INDEX_PATH })
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Học Spring Boot')
    await user.type(screen.getByLabelText('Slug'), 'hoc-spring-boot')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE))
    expect(await screen.findByText('Đã tạo bản nháp.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', SERIES_API_PATH)
    expect(create.body).toEqual({
      title: 'Học Spring Boot',
      slug: 'hoc-spring-boot',
      summary: null,
      overviewMarkdown: null,
      topicIds: [],
    })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    await user.click(screen.getByRole('button', { name: 'Back (test)' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Nội dung' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
  })

  it('render_ShouldLoadSeriesAndListModulesInOrder_WhenEditorIsOpenedDirectly', async () => {
    const series = draftSeries({
      modules: [
        moduleChild({ status: 'PUBLISHED' }),
        moduleChild({ id: OTHER_MODULE_ID, title: 'Nâng cao', slug: 'nang-cao', position: 2 }),
      ],
    })
    const api = givenLoaded(series).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByLabelText('Tiêu đề')).toHaveValue('Học Spring Boot')
    expect(screen.getByLabelText('Giới thiệu (overview)')).toHaveValue('## Giới thiệu\n\nBắt đầu từ đâu.')
    expect(screen.getByText('Đã lưu mọi thay đổi')).toBeInTheDocument()
    const modules = within(screen.getByRole('list', { name: 'Module của Series' })).getAllByRole('listitem')
    expect(modules).toHaveLength(2)
    expect(modules[0]).toHaveTextContent('1.Nền tảngnen-tangĐã xuất bản')
    expect(modules[1]).toHaveTextContent('2.Nâng caonang-caoBản nháp')
    expect(api.requestsTo('POST', SERIES_API_PATH)).toHaveLength(0)
  })

  it('save_ShouldSendIfMatchAndUseNewEtag_WhenSeriesIsSavedTwice', async () => {
    const second = draftSeries({ revision: 2, title: 'Tiêu đề mới' })
    const third = draftSeries({ revision: 3, title: 'Tiêu đề mới hơn' })
    const api = givenLoaded(draftSeries())
      .on('PUT', SERIES_PATH, jsonResponse(200, second, seriesHeaders(second)), jsonResponse(200, third, seriesHeaders(third)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')

    await user.clear(title)
    await user.type(title, 'Tiêu đề mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await screen.findByText('Đã lưu.')
    await user.type(title, ' hơn')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.requestsTo('PUT', SERIES_PATH)).toHaveLength(2))
    const [first, next] = api.requestsTo('PUT', SERIES_PATH)
    expect(first.headers.get('If-Match')).toBe('"1"')
    expect(next.headers.get('If-Match')).toBe('"2"')
    expect(first.body).not.toHaveProperty('modules')
  })

  it('save_ShouldShowErrorUnderOverview_WhenOverviewIsRequired', async () => {
    givenLoaded(publishedSeries())
      .on('PUT', SERIES_PATH, problemResponse(422, 'SERIES_OVERVIEW_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.clear(await screen.findByLabelText('Giới thiệu (overview)'))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(
      await screen.findByText('Lỗi: Cần có phần giới thiệu (overview) cho nội dung đang hoặc sắp xuất bản.'),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Giới thiệu (overview)')).toHaveValue('')
  })

  it('save_ShouldOfferReload_WhenRevisionIsStale', async () => {
    const newer = draftSeries({ revision: 2, title: 'Bản mới hơn' })
    givenLoaded(draftSeries())
      .on('PUT', SERIES_PATH, problemResponse(412, 'REVISION_CONFLICT'))
      .on('GET', SERIES_PATH, jsonResponse(200, newer, seriesHeaders(newer)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.type(await screen.findByLabelText('Tiêu đề'), ' sửa')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await user.click(await screen.findByRole('button', { name: 'Tải bản mới nhất (bỏ thay đổi chưa lưu)' }))

    expect(await screen.findByText('Đã tải bản mới nhất từ máy chủ.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tiêu đề')).toHaveValue('Bản mới hơn')
  })

  it('publish_ShouldPointAtModuleList_WhenSeriesHasNoPublicModule', async () => {
    const api = givenLoaded(draftSeries({ modules: [moduleChild()] }))
      .on('POST', seriesPublishPath('publish'), problemResponse(422, 'SERIES_HAS_NO_PUBLIC_MODULE'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    const modules = await screen.findByRole('region', { name: 'Danh sách Module' })
    expect(await within(modules).findByRole('alert')).toHaveTextContent('Series cần ít nhất một Module đã xuất bản')
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
    expect(api.requestsTo('POST', seriesPublishPath('publish'))[0].headers.get('If-Match')).toBe('"1"')
  })

  it('publish_ShouldUseNewEtagForNextSave_WhenSeriesIsPublished', async () => {
    const draft = draftSeries({ revision: 2 })
    const saved = draftSeries({ revision: 4, status: 'PUBLISHED', publishedAt: PUBLISHED_AT, title: 'Sau khi xuất bản' })
    const api = givenLoaded(draft)
      .on(
        'POST',
        seriesPublishPath('publish'),
        jsonResponse(200, publicationOfSeries(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT }), {
          ETag: '"3"',
        }),
      )
      .on('PUT', SERIES_PATH, jsonResponse(200, saved, seriesHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))
    await screen.findByText('Trạng thái: Đã xuất bản')

    await user.type(screen.getByLabelText('Tiêu đề'), ' mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await screen.findByText('Đã lưu.')
    expect(api.requestsTo('PUT', SERIES_PATH)[0].headers.get('If-Match')).toBe('"3"')
    expect(screen.getByLabelText('Slug')).toHaveAttribute('readonly')
  })

  it('unpublish_ShouldNotSendRequest_WhenWarningIsCancelled', async () => {
    const api = givenLoaded(publishedSeries()).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Gỡ xuất bản' }))
    expect(await screen.findByRole('alertdialog')).toHaveTextContent('giữ nguyên trạng thái xuất bản')
    await user.click(screen.getByRole('button', { name: 'Huỷ' }))

    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(api.requestsTo('POST', seriesPublishPath('unpublish'))).toHaveLength(0)
    expect(screen.getByText('Trạng thái: Đã xuất bản')).toBeInTheDocument()
  })

  it('unpublish_ShouldSendRequestAndKeepModules_WhenWarningIsConfirmed', async () => {
    const published = draftSeries({
      status: 'PUBLISHED',
      revision: 3,
      publishedAt: PUBLISHED_AT,
      modules: [moduleChild({ status: 'PUBLISHED' })],
    })
    const api = givenLoaded(published)
      .on(
        'POST',
        seriesPublishPath('unpublish'),
        jsonResponse(200, publicationOfSeries(published, { status: 'UNPUBLISHED', revision: 4, unpublishedAt: PUBLISHED_AT }), {
          ETag: '"4"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Gỡ xuất bản' }))
    await user.click(await screen.findByRole('button', { name: 'Vẫn gỡ xuất bản' }))

    expect(await screen.findByText('Trạng thái: Đã gỡ xuất bản')).toBeInTheDocument()
    expect(api.requestsTo('POST', seriesPublishPath('unpublish'))[0].headers.get('If-Match')).toBe('"3"')
    const modules = screen.getByRole('list', { name: 'Module của Series' })
    expect(within(modules).getByText('Đã xuất bản')).toBeInTheDocument()
  })

  it('render_ShouldLockWriteButtons_WhenUnpublishIsRunning', async () => {
    const published = publishedSeries()
    givenLoaded(published)
      .on(
        'POST',
        seriesPublishPath('unpublish'),
        jsonResponse(200, publicationOfSeries(published, { status: 'UNPUBLISHED', revision: 4 }), { ETag: '"4"' }),
      )
      .install()
    const release = holdRequests('POST')
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Gỡ xuất bản' }))

    await user.click(await screen.findByRole('button', { name: 'Vẫn gỡ xuất bản' }))

    expect(await screen.findByRole('button', { name: 'Đang gỡ xuất bản…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Lưu' })).toBeDisabled()
    release()
    expect(await screen.findByText('Trạng thái: Đã gỡ xuất bản')).toBeInTheDocument()
  })

  it('render_ShouldShowNotFound_WhenSeriesDoesNotExist', async () => {
    givenSeriesEditorSession(new StubApiServer()).on('GET', SERIES_PATH, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang quản trị' })).toBeInTheDocument()
  })

  it('render_ShouldHidePublicationActions_WhenSeriesIsNew', async () => {
    givenSeriesEditorSession(new StubApiServer()).install()

    renderAppRoute('/admin/series/new')

    expect(await screen.findByLabelText('Tiêu đề')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xuất bản' })).toBeNull()
    expect(screen.queryByRole('region', { name: 'Danh sách Module' })).toBeNull()
  })

  it('render_ShouldLinkToNewModuleAndEachModule_WhenSeriesIsSaved', async () => {
    givenLoaded(draftSeries({ modules: [moduleChild()] })).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByRole('link', { name: 'Thêm Module' })).toHaveAttribute(
      'href',
      `/admin/modules/new?seriesId=${SERIES_ID}`,
    )
    expect(screen.getByRole('link', { name: 'Nền tảng' })).toHaveAttribute('href', `/admin/modules/${MODULE_ID}`)
  })

  it('reorder_ShouldSendModuleIdsAndUseNewEtagForNextSave_WhenModuleIsMovedDown', async () => {
    const series = draftSeries({
      modules: [moduleChild(), moduleChild({ id: OTHER_MODULE_ID, title: 'Nâng cao', slug: 'nang-cao', position: 2 })],
    })
    const saved = draftSeries({ revision: 3, title: 'Tiêu đề sau khi sắp' })
    const api = givenLoaded(series)
      .on(
        'PUT',
        SERIES_ORDER_PATH,
        jsonResponse(
          200,
          seriesOrderResponse(2, [
            { id: OTHER_MODULE_ID, position: 1, revision: 2 },
            { id: MODULE_ID, position: 2, revision: 2 },
          ]),
          { ETag: '"2"' },
        ),
      )
      .on('PUT', SERIES_PATH, jsonResponse(200, saved, seriesHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Đưa xuống: Nền tảng' }))
    await user.click(screen.getByRole('button', { name: 'Lưu thứ tự' }))

    expect(await screen.findByText('Đã lưu thứ tự.')).toBeInTheDocument()
    const [order] = api.requestsTo('PUT', SERIES_ORDER_PATH)
    expect(order.body).toEqual({ moduleIds: [OTHER_MODULE_ID, MODULE_ID] })
    expect(order.headers.get('If-Match')).toBe('"1"')
    const modules = within(screen.getByRole('list', { name: 'Module của Series' })).getAllByRole('listitem')
    expect(modules[0]).toHaveTextContent('1.Nâng cao')
    await user.type(screen.getByLabelText('Tiêu đề'), ' mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await screen.findByText('Đã lưu.')
    expect(api.requestsTo('PUT', SERIES_PATH)[0].headers.get('If-Match')).toBe('"2"')
  })

  it('render_ShouldLockWriteButtons_WhenOrderIsBeingSaved', async () => {
    const series = draftSeries({
      modules: [moduleChild(), moduleChild({ id: OTHER_MODULE_ID, title: 'Nâng cao', slug: 'nang-cao', position: 2 })],
    })
    givenLoaded(series)
      .on(
        'PUT',
        SERIES_ORDER_PATH,
        jsonResponse(
          200,
          seriesOrderResponse(2, [
            { id: OTHER_MODULE_ID, position: 1, revision: 2 },
            { id: MODULE_ID, position: 2, revision: 2 },
          ]),
          { ETag: '"2"' },
        ),
      )
      .install()
    const release = holdRequests('PUT')
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Đưa xuống: Nền tảng' }))

    await user.click(screen.getByRole('button', { name: 'Lưu thứ tự' }))

    expect(await screen.findByRole('button', { name: 'Đang lưu thứ tự…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Lưu' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Đưa lên: Nâng cao' })).toBeDisabled()
    release()
    expect(await screen.findByText('Đã lưu thứ tự.')).toBeInTheDocument()
  })

  it('delete_ShouldSendEtagAndOpenIndexWithNotice_WhenDraftSeriesIsDeleted', async () => {
    const series = draftSeries()
    const api = givenContentIndex(givenLoaded(series).on('DELETE', SERIES_PATH, { status: 204 }), []).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${series.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByText(`Đã xóa “${series.title}”.`)).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
    expect(api.requestsTo('DELETE', SERIES_PATH)[0].headers.get('If-Match')).toBe(`"${series.revision}"`)
  })

  it('delete_ShouldExplainChildrenAndStayInEditor_WhenSeriesStillHasModules', async () => {
    const series = draftSeries({ modules: [moduleChild()] })
    givenLoaded(series).on('DELETE', SERIES_PATH, problemResponse(409, 'CONTENT_IN_USE')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${series.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('còn Module')
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.getByRole('button', { name: `Xóa ${series.title}` })).toBeEnabled()
  })
})
