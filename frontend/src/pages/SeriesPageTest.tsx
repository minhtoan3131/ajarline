import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  moduleApiPath,
  moduleArticleItem,
  publicModule,
  publicSeries,
  secondModuleArticleItem,
  seriesApiPath,
  seriesModuleItem,
} from '../features/learning-navigation/fixture/publicLearningFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const SERIES_API = seriesApiPath('java-concurrency')

const TWO_MODULES = publicSeries({
  modules: [
    seriesModuleItem({ articles: [moduleArticleItem(), secondModuleArticleItem()] }),
    seriesModuleItem({
      id: '8d1e2f3a-4b5c-4d6e-9f70-a1b2c3d4e5f6',
      slug: 'executor',
      title: 'Executor',
      summary: 'Thread pool và Executor.',
      position: 2,
      articles: [
        moduleArticleItem({ id: '4c5d6e7f-8091-4a2b-b3c4-d5e6f708192a', slug: 'thread-pool', title: 'Thread pool' }),
      ],
    }),
  ],
})

describe('SeriesPage', () => {
  it('render_ShouldShowLoadingFirst_WhenSeriesIsRequested', () => {
    new StubApiServer().on('GET', SERIES_API, jsonResponse(200, TWO_MODULES)).install()

    renderAppRoute('/series/java-concurrency')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải Series…')
  })

  it('render_ShouldShowOverviewTopicsAndModulesInOrder_WhenSeriesIsPublic', async () => {
    new StubApiServer().on('GET', SERIES_API, jsonResponse(200, TWO_MODULES)).install()

    renderAppRoute('/series/java-concurrency')

    expect(await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })).toBeInTheDocument()
    expect(screen.getByText('Học xử lý đồng thời trong Java.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Mục tiêu' })).toBeInTheDocument()
    const topics = screen.getByRole('list', { name: 'Chủ đề của Series' })
    expect(within(topics).getByRole('link', { name: 'Java' })).toHaveAttribute('href', '/topics/java')
    expect(screen.getByRole('link', { name: 'Tất cả Series' })).toHaveAttribute('href', '/series')
    expect(screen.getAllByRole('heading', { level: 3 }).map((heading) => heading.textContent)).toEqual([
      'Thread cơ bản',
      'Executor',
    ])
    expect(screen.getByText('Module 1')).toBeInTheDocument()
    expect(screen.getByText('Module 2')).toBeInTheDocument()
  })

  it('render_ShouldLinkModulesAndArticles_WhenSeriesIsPublic', async () => {
    new StubApiServer().on('GET', SERIES_API, jsonResponse(200, TWO_MODULES)).install()

    renderAppRoute('/series/java-concurrency')

    expect(await screen.findByRole('link', { name: 'Thread cơ bản' })).toHaveAttribute('href', '/modules/thread-co-ban')
    expect(screen.getByRole('link', { name: 'Executor' })).toHaveAttribute('href', '/modules/executor')
    const firstArticles = screen.getByRole('list', { name: 'Các bài viết trong Thread cơ bản' })
    expect(within(firstArticles).getAllByRole('link').map((link) => link.textContent)).toEqual([
      'Tạo Thread',
      'Đồng bộ hoá',
    ])
    expect(within(firstArticles).getByRole('link', { name: 'Tạo Thread' })).toHaveAttribute(
      'href',
      '/articles/tao-thread',
    )
    const secondArticles = screen.getByRole('list', { name: 'Các bài viết trong Executor' })
    expect(within(secondArticles).getByRole('link', { name: 'Thread pool' })).toHaveAttribute(
      'href',
      '/articles/thread-pool',
    )
  })

  it('render_ShouldNotShowProgress_WhenSeriesHasModules', async () => {
    new StubApiServer().on('GET', SERIES_API, jsonResponse(200, TWO_MODULES)).install()

    renderAppRoute('/series/java-concurrency')
    await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })

    expect(screen.getByRole('main').textContent).not.toMatch(/\d+\s*\/\s*\d+/)
  })

  it('render_ShouldNotRunScript_WhenOverviewHasHtml', async () => {
    const overview = '## Mở đầu\n\n<script>window.hacked = true</script>\n\nĐoạn văn.'
    new StubApiServer()
      .on('GET', SERIES_API, jsonResponse(200, publicSeries({ overviewMarkdown: overview })))
      .install()

    renderAppRoute('/series/java-concurrency')

    expect(await screen.findByRole('heading', { level: 2, name: 'Mở đầu' })).toBeInTheDocument()
    expect(document.querySelector('script')).toBeNull()
    expect(screen.queryByText(/hacked/)).toBeNull()
  })

  it('render_ShouldRequestOnlyThisSeries_WhenOpenedByDirectUrl', async () => {
    const api = new StubApiServer().on('GET', SERIES_API, jsonResponse(200, TWO_MODULES)).install()

    renderAppRoute('/series/java-concurrency')
    await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })

    expect(api.requests.map((request) => request.path)).toEqual([SERIES_API])
  })

  it('render_ShouldShowGenericNotFound_WhenSeriesIsNotPublic', async () => {
    new StubApiServer().on('GET', SERIES_API, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute('/series/java-concurrency')

    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Xem các Series' })).toHaveAttribute('href', '/series')
    expect(screen.queryByText(/nháp|draft|unpublish|gỡ|ẩn/i)).toBeNull()
  })

  it('retry_ShouldLoadSeries_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on('GET', SERIES_API, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, TWO_MODULES))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/series/java-concurrency')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được Series')
    expect(screen.queryByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java Concurrency' })).toBeInTheDocument()
  })

  it.each([
    ['no Module', publicSeries({ modules: [] })],
    ['a Module without Article', publicSeries({ modules: [seriesModuleItem({ articles: [] })] })],
  ])('render_ShouldShowTechnicalErrorWithoutEmptyFrame_WhenSeriesHas (%s)', async (_label, body) => {
    new StubApiServer().on('GET', SERIES_API, jsonResponse(200, body)).install()

    renderAppRoute('/series/java-concurrency')

    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được Series')
    expect(screen.queryByRole('heading', { level: 2, name: 'Các Module' })).toBeNull()
  })

  it('navigate_ShouldOpenModule_WhenModuleIsClicked', async () => {
    new StubApiServer()
      .on('GET', SERIES_API, jsonResponse(200, TWO_MODULES))
      .on('GET', moduleApiPath('thread-co-ban'), jsonResponse(200, publicModule()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/series/java-concurrency')

    await user.click(await screen.findByRole('link', { name: 'Thread cơ bản' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/modules/thread-co-ban')
  })
})
