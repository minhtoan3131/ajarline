import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenContentIndex } from '../../features/admin-content/fixture/contentIndexFixture'
import {
  ARTICLE_ONE_ID,
  ARTICLE_TWO_ID,
  articleChild,
  draftModule,
  draftSeries,
  givenModuleEditorSession,
  MODULE_ID,
  MODULE_ORDER_PATH,
  MODULE_PATH,
  moduleChild,
  moduleHeaders,
  moduleOrderResponse,
  modulePublishPath,
  MODULES_API_PATH,
  OTHER_MODULE_ID,
  SERIES_API_PATH,
  SERIES_ID,
  SERIES_PATH,
  seriesHeaders,
  seriesItem,
} from '../../features/admin-learning/fixture/learningAdminFixture'
import type { ModuleAdminDetail } from '../../features/admin-learning/model/module'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/modules/${MODULE_ID}`
const OTHER_SERIES_ID = '9d3e5f70-1a2b-4c6d-8e90-a1b2c3d4e5f6'

function givenLoaded(module: ModuleAdminDetail): StubApiServer {
  return givenModuleEditorSession(new StubApiServer()).on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
}

function twoArticles(): ModuleAdminDetail {
  return draftModule({
    articles: [
      articleChild({ status: 'PUBLISHED' }),
      articleChild({ id: ARTICLE_TWO_ID, title: 'Bài thứ hai', slug: 'bai-thu-hai', position: 2 }),
    ],
  })
}

describe('ModuleEditorPage', () => {
  it('render_ShouldShowParentAndArticlesInOrder_WhenModuleIsOpenedDirectly', async () => {
    givenLoaded(twoArticles()).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByLabelText('Tiêu đề')).toHaveValue('Nền tảng')
    expect(screen.getByLabelText('Series cha')).toHaveValue(SERIES_ID)
    expect(await screen.findByRole('option', { name: 'Học Spring Boot' })).toBeInTheDocument()
    expect(screen.getByLabelText('Vị trí')).toHaveValue('1')
    const articles = within(screen.getByRole('list', { name: 'Bài viết của Module' })).getAllByRole('listitem')
    expect(articles).toHaveLength(2)
    expect(articles[0]).toHaveTextContent('1.Bài mở đầubai-mo-dauĐã xuất bản')
    expect(articles[1]).toHaveTextContent('2.Bài thứ haibai-thu-haiBản nháp')
  })

  it('render_ShouldChooseSeriesAndEndPosition_WhenSeriesIdIsInUrl', async () => {
    const series = draftSeries({ modules: [moduleChild(), moduleChild({ id: OTHER_MODULE_ID, position: 2 })] })
    givenModuleEditorSession(new StubApiServer())
      .on('GET', SERIES_PATH, jsonResponse(200, series, seriesHeaders(series)))
      .install()

    renderAppRoute(`/admin/modules/new?seriesId=${SERIES_ID}`)

    expect(await screen.findByLabelText('Series cha')).toHaveValue(SERIES_ID)
    expect(screen.getByLabelText('Vị trí')).toHaveValue('3')
    expect(screen.getByRole('heading', { level: 1, name: 'Module mới' })).toBeInTheDocument()
  })

  it('render_ShouldChooseNothing_WhenSeriesIdInUrlDoesNotExist', async () => {
    givenModuleEditorSession(new StubApiServer())
      .on('GET', `${SERIES_API_PATH}/khong-co`, problemResponse(404, 'CONTENT_NOT_FOUND'))
      .install()

    renderAppRoute('/admin/modules/new?seriesId=khong-co')

    expect(await screen.findByLabelText('Series cha')).toHaveValue('')
    expect(screen.getByLabelText('Vị trí')).toHaveValue('')
  })

  it('save_ShouldCreateModuleAndReplaceUrl_WhenNewModuleIsSaved', async () => {
    const series = draftSeries()
    const created = draftModule()
    const api = givenModuleEditorSession(new StubApiServer())
      .on('GET', SERIES_PATH, jsonResponse(200, series, seriesHeaders(series)))
      .on('POST', MODULES_API_PATH, jsonResponse(201, created, { ...moduleHeaders(created), Location: MODULE_PATH }))
      .on('GET', MODULE_PATH, jsonResponse(200, created, moduleHeaders(created)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(`/admin/modules/new?seriesId=${SERIES_ID}`)
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Nền tảng')
    await user.type(screen.getByLabelText('Slug'), 'nen-tang')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE))
    expect(await screen.findByText('Đã tạo bản nháp.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', MODULES_API_PATH)
    expect(create.body).toEqual({
      title: 'Nền tảng',
      slug: 'nen-tang',
      summary: null,
      overviewMarkdown: null,
      topicIds: [],
      seriesId: SERIES_ID,
      position: 1,
    })
  })

  it('save_ShouldAskForSeriesAndSendNothing_WhenNoSeriesIsChosen', async () => {
    const api = givenModuleEditorSession(new StubApiServer()).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/modules/new')
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Nền tảng')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(
      await screen.findByText('Lỗi: Hãy chọn Series cha và nhập vị trí là số nguyên từ 1 trở lên.'),
    ).toBeInTheDocument()
    expect(api.requestsTo('POST', MODULES_API_PATH)).toHaveLength(0)
  })

  it('save_ShouldShowErrorAtParent_WhenPositionIsInvalid', async () => {
    givenLoaded(draftModule()).on('PUT', MODULE_PATH, problemResponse(422, 'INVALID_POSITION')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Lưu' }))

    expect(
      await screen.findByText(
        'Lỗi: Vị trí không hợp lệ. Nhập số từ 1 đến số Module hiện có của Series đã chọn cộng 1.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByLabelText('Vị trí')).toHaveValue('1')
  })

  it('change_ShouldEmptyPosition_WhenAnotherSeriesIsChosen', async () => {
    givenModuleEditorSession(new StubApiServer(), [seriesItem(), seriesItem({ id: OTHER_SERIES_ID, title: 'Học React' })])
      .on('GET', MODULE_PATH, jsonResponse(200, draftModule({ position: 2 }), moduleHeaders(draftModule({ position: 2 }))))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByRole('option', { name: 'Học React' })

    await user.selectOptions(screen.getByLabelText('Series cha'), OTHER_SERIES_ID)

    expect(screen.getByLabelText('Series cha')).toHaveValue(OTHER_SERIES_ID)
    expect(screen.getByLabelText('Vị trí')).toHaveValue('')
  })

  it('save_ShouldUseEtagOfSavedOrder_WhenFormIsSavedAfterArticlesAreReordered', async () => {
    const saved = draftModule({ revision: 3, title: 'Nền tảng mới' })
    const api = givenLoaded(twoArticles())
      .on(
        'PUT',
        MODULE_ORDER_PATH,
        jsonResponse(
          200,
          moduleOrderResponse(2, [
            { id: ARTICLE_TWO_ID, position: 1, revision: 2 },
            { id: ARTICLE_ONE_ID, position: 2, revision: 2 },
          ]),
          { ETag: '"2"' },
        ),
      )
      .on('PUT', MODULE_PATH, jsonResponse(200, saved, moduleHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Đưa xuống: Bài mở đầu' }))
    await user.click(screen.getByRole('button', { name: 'Lưu thứ tự' }))
    await screen.findByText('Đã lưu thứ tự.')
    await user.type(screen.getByLabelText('Tiêu đề'), ' mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await screen.findByText('Đã lưu.')
    const [order] = api.requestsTo('PUT', MODULE_ORDER_PATH)
    expect(order.body).toEqual({ articleIds: [ARTICLE_TWO_ID, ARTICLE_ONE_ID] })
    expect(order.headers.get('If-Match')).toBe('"1"')
    expect(api.requestsTo('PUT', MODULE_PATH)[0].headers.get('If-Match')).toBe('"2"')
  })

  it('reorder_ShouldOfferReload_WhenArticleSetChanged', async () => {
    const newer = draftModule({ revision: 2, title: 'Bản mới hơn', articles: twoArticles().articles })
    givenLoaded(twoArticles())
      .on('PUT', MODULE_ORDER_PATH, problemResponse(409, 'ORDER_SET_CONFLICT'))
      .on('GET', MODULE_PATH, jsonResponse(200, newer, moduleHeaders(newer)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Đưa xuống: Bài mở đầu' }))
    await user.click(screen.getByRole('button', { name: 'Lưu thứ tự' }))

    const list = await screen.findByRole('region', { name: 'Danh sách bài viết' })
    expect(await within(list).findByRole('alert')).toHaveTextContent('Danh sách đã thay đổi ở nơi khác')
    await user.click(within(list).getByRole('button', { name: 'Tải lại danh sách (bỏ thay đổi chưa lưu)' }))

    expect(await screen.findByText('Đã tải bản mới nhất từ máy chủ.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tiêu đề')).toHaveValue('Bản mới hơn')
    expect(screen.getByRole('button', { name: 'Lưu thứ tự' })).toBeDisabled()
  })

  it('publish_ShouldPointAtArticleList_WhenModuleHasNoPublicArticle', async () => {
    givenLoaded(twoArticles())
      .on('POST', modulePublishPath('publish'), problemResponse(422, 'MODULE_HAS_NO_PUBLIC_ARTICLE'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    const list = await screen.findByRole('region', { name: 'Danh sách bài viết' })
    expect(await within(list).findByRole('alert')).toHaveTextContent('Module cần ít nhất một bài viết đã xuất bản')
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('render_ShouldShowNotFound_WhenModuleDoesNotExist', async () => {
    givenModuleEditorSession(new StubApiServer()).on('GET', MODULE_PATH, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang quản trị' })).toBeInTheDocument()
  })

  it('delete_ShouldSendEtagAndOpenIndexWithNotice_WhenDraftModuleIsDeleted', async () => {
    const module = draftModule()
    const api = givenContentIndex(givenLoaded(module).on('DELETE', MODULE_PATH, { status: 204 }), []).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${module.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByText(`Đã xóa “${module.title}”.`)).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
    expect(api.requestsTo('DELETE', MODULE_PATH)[0].headers.get('If-Match')).toBe(`"${module.revision}"`)
  })

  it('delete_ShouldExplainChildrenAndStayInEditor_WhenModuleStillHasArticles', async () => {
    const module = draftModule({ articles: [articleChild()] })
    givenLoaded(module).on('DELETE', MODULE_PATH, problemResponse(409, 'CONTENT_IN_USE')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${module.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('còn bài viết')
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
  })
})
