import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  moduleApiPath,
  publicModule,
  publicSeries,
  seriesApiPath,
} from '../features/learning-navigation/fixture/publicLearningFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const MODULE_API = moduleApiPath('thread-co-ban')

describe('ModulePage', () => {
  it('render_ShouldShowLoadingFirst_WhenModuleIsRequested', () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule())).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải Module…')
  })

  it('render_ShouldShowOverviewAndArticlesWithRank_WhenModuleIsPublic', async () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule())).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })).toBeInTheDocument()
    expect(screen.getByText('Làm quen với Thread.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Nội dung' })).toBeInTheDocument()
    expect(screen.getByText('Bài 1')).toBeInTheDocument()
    expect(screen.getByText('Bài 2')).toBeInTheDocument()
    const articles = screen.getByRole('region', { name: 'Các bài viết' })
    expect(within(articles).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Tạo Thread',
      'Đồng bộ hoá',
    ])
    expect(screen.getByRole('link', { name: 'Tạo Thread' })).toHaveAttribute('href', '/articles/tao-thread')
    expect(screen.getByRole('link', { name: 'Đồng bộ hoá' })).toHaveAttribute('href', '/articles/dong-bo-hoa')
  })

  it('render_ShouldLinkBackToSeries_WhenSeriesIsPublic', async () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule())).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByRole('link', { name: 'Java Concurrency' })).toHaveAttribute(
      'href',
      '/series/java-concurrency',
    )
  })

  it('render_ShouldLeaveNoTraceOfSeries_WhenSeriesIsHidden', async () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule({ series: null }))).install()

    renderAppRoute('/modules/thread-co-ban')

    await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })
    const page = within(screen.getByRole('main'))
    expect(page.queryByText(/Series/)).toBeNull()
    expect(page.queryByRole('link', { name: 'Java Concurrency' })).toBeNull()
  })

  it('render_ShouldNotShowProgress_WhenModuleHasArticles', async () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule())).install()

    renderAppRoute('/modules/thread-co-ban')
    await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })

    expect(screen.getByRole('main').textContent).not.toMatch(/\d+\s*\/\s*\d+/)
  })

  it('render_ShouldNotRunScript_WhenOverviewHasHtml', async () => {
    const overview = '## Mở đầu\n\n<script>window.hacked = true</script>\n\nĐoạn văn.'
    new StubApiServer()
      .on('GET', MODULE_API, jsonResponse(200, publicModule({ overviewMarkdown: overview })))
      .install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByRole('heading', { level: 2, name: 'Mở đầu' })).toBeInTheDocument()
    expect(document.querySelector('script')).toBeNull()
    expect(screen.queryByText(/hacked/)).toBeNull()
  })

  it('render_ShouldBeEnoughByItself_WhenOpenedByDirectUrl', async () => {
    const api = new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule())).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Java Concurrency' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Tạo Thread' })).toBeInTheDocument()
    expect(api.requests.map((request) => request.path)).toEqual([MODULE_API])
  })

  it('render_ShouldShowGenericNotFound_WhenModuleIsNotPublic', async () => {
    new StubApiServer().on('GET', MODULE_API, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.queryByText(/nháp|draft|unpublish|gỡ|ẩn/i)).toBeNull()
  })

  it('retry_ShouldLoadModule_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on('GET', MODULE_API, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, publicModule()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/modules/thread-co-ban')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được Module')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })).toBeInTheDocument()
  })

  it('render_ShouldShowTechnicalErrorWithoutEmptyFrame_WhenModuleHasNoArticle', async () => {
    new StubApiServer().on('GET', MODULE_API, jsonResponse(200, publicModule({ articles: [] }))).install()

    renderAppRoute('/modules/thread-co-ban')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được Module')
    expect(screen.queryByRole('heading', { level: 2, name: 'Các bài viết' })).toBeNull()
  })

  it('navigate_ShouldOpenSeries_WhenSeriesLinkIsClicked', async () => {
    new StubApiServer()
      .on('GET', MODULE_API, jsonResponse(200, publicModule()))
      .on('GET', seriesApiPath('java-concurrency'), jsonResponse(200, publicSeries()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/modules/thread-co-ban')

    await user.click(await screen.findByRole('link', { name: 'Java Concurrency' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/series/java-concurrency')
  })
})
