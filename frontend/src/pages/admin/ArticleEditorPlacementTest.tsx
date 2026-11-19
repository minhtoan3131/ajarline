import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import {
  ARTICLE_ID,
  ARTICLE_PATH,
  draftArticle,
  revisionHeaders,
} from '../../features/admin-content/fixture/articleAdminFixture'
import {
  ARTICLE_TWO_ID,
  articleChild,
  draftModule,
  draftSeries,
  givenSeriesEditorSession,
  givenSeriesList,
  MODULE_ID,
  MODULE_PATH,
  moduleChild,
  moduleHeaders,
  SERIES_PATH,
  seriesHeaders,
  seriesItem,
  seriesListApiPath,
} from '../../features/admin-learning/fixture/learningAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/articles/${ARTICLE_ID}`

function givenArticle(article = draftArticle()): StubApiServer {
  return givenSeriesEditorSession(new StubApiServer()).on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
}

/** The Series that holds the Module, as the picker reads it. */
function givenSeriesWithModule(api: StubApiServer): StubApiServer {
  const series = draftSeries({ modules: [moduleChild()] })
  return api.on('GET', SERIES_PATH, jsonResponse(200, series, seriesHeaders(series)))
}

describe('ArticleEditorPage placement', () => {
  it('save_ShouldSendChosenPlacement_WhenModuleIsChosen', async () => {
    const module = draftModule({ articles: [articleChild(), articleChild({ id: ARTICLE_TWO_ID, position: 2 })] })
    const saved = draftArticle({ revision: 2, moduleId: MODULE_ID, position: 3 })
    const api = givenSeriesWithModule(givenSeriesList(givenArticle(), [seriesItem()]))
      .on('GET', MODULE_PATH, jsonResponse(200, module, moduleHeaders(module)))
      .on('PUT', ARTICLE_PATH, jsonResponse(200, saved, revisionHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const series = await screen.findByLabelText('Series')
    await screen.findByRole('option', { name: 'Học Spring Boot' })

    await user.selectOptions(series, seriesItem().id)
    await user.selectOptions(await screen.findByLabelText('Module'), MODULE_ID)
    expect(await screen.findByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
    const [save] = api.requestsTo('PUT', ARTICLE_PATH)
    expect(save.body).toMatchObject({ moduleId: MODULE_ID, position: 3 })
    expect(save.headers.get('If-Match')).toBe('"1"')
    expect(screen.getByLabelText('Vị trí trong Module')).toHaveValue('3')
  })

  it('save_ShouldSendCurrentPlacement_WhenSeriesListCannotBeRead', async () => {
    const inModule = draftArticle({ moduleId: MODULE_ID, position: 2 })
    const saved = draftArticle({ moduleId: MODULE_ID, position: 2, revision: 2, title: 'Tiêu đề mới' })
    const api = givenSeriesWithModule(givenArticle(inModule))
      .on('GET', seriesListApiPath(), problemResponse(500, 'INTERNAL_ERROR'))
      .on('GET', MODULE_PATH, jsonResponse(200, draftModule(), moduleHeaders(draftModule())))
      .on('PUT', ARTICLE_PATH, jsonResponse(200, saved, revisionHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.type(await screen.findByLabelText('Tiêu đề'), ' mới')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
    expect(api.requestsTo('PUT', ARTICLE_PATH)[0].body).toMatchObject({ moduleId: MODULE_ID, position: 2 })
    expect(screen.getByText('Không tải được danh sách Series. Bài vẫn được lưu với vị trí hiện tại.')).toBeInTheDocument()
  })

  it('save_ShouldShowErrorAtPlacement_WhenModuleNoLongerExists', async () => {
    const inModule = draftArticle({ moduleId: MODULE_ID, position: 1 })
    givenSeriesWithModule(givenSeriesList(givenArticle(inModule), [seriesItem()]))
      .on('GET', MODULE_PATH, jsonResponse(200, draftModule(), moduleHeaders(draftModule())))
      .on('PUT', ARTICLE_PATH, problemResponse(422, 'MODULE_REFERENCE_NOT_FOUND'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Lỗi: Module đã chọn không còn tồn tại. Hãy chọn lại.')).toBeInTheDocument()
    expect(screen.getByLabelText('Vị trí trong Module')).toHaveValue('1')
  })

  it('render_ShouldOfferStandalone_WhenArticleIsNew', async () => {
    givenSeriesList(givenSeriesEditorSession(new StubApiServer()), []).install()

    renderAppRoute('/admin/articles/new')

    const series = await screen.findByLabelText('Series')
    expect(series).toHaveValue('')
    expect(screen.queryByLabelText('Vị trí trong Module')).toBeNull()
  })
})
