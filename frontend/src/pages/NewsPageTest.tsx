import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import { EDITORIAL_PROFILE } from '../features/article-reading/editorialProfile'
import { newsApiPath, publicNews } from '../features/news-reading/fixture/publicNewsFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const NEWS_PATH = newsApiPath('spring-boot-4-1')

function stubNews(body: unknown): StubApiServer {
  return new StubApiServer().on('GET', NEWS_PATH, jsonResponse(200, body)).install()
}

describe('NewsPage', () => {
  it('render_ShouldShowLoadingFirst_WhenNewsIsRequested', () => {
    stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    expect(screen.getByRole('status')).toHaveTextContent('Đang tải tin…')
  })

  it('render_ShouldShowNewsWithSourceAndAttribution_WhenSlugIsPublic', async () => {
    const api = stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    expect(await screen.findByRole('heading', { level: 1, name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
    expect(screen.getByText('Bản phát hành mới có nhiều thay đổi.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Điểm mới' })).toBeInTheDocument()
    expect(screen.getByText(/^Xuất bản/)).toHaveTextContent(/Xuất bản\s+4 tháng 11/)
    expect(screen.getByText(EDITORIAL_PROFILE.displayName)).toBeInTheDocument()
    const chips = screen.getByRole('list', { name: 'Chủ đề của tin' })
    expect(within(chips).getByRole('link', { name: 'Java' })).toHaveAttribute('href', '/topics/java')
    const source = screen.getByRole('region', { name: 'Nguồn gốc' })
    expect(within(source).getByRole('link', { name: 'Spring Blog (mở trong tab mới)' })).toHaveAttribute(
      'target',
      '_blank',
    )
    expect(api.requests.map((request) => request.path)).toEqual([NEWS_PATH])
  })

  it('render_ShouldShowUpdatedDate_WhenUpdatedOnAnotherDay', async () => {
    stubNews(publicNews({ updatedAt: '2026-11-06T02:00:00Z' }))

    renderAppRoute('/news/spring-boot-4-1')

    expect(await screen.findByText(/^Xuất bản/)).toHaveTextContent(/Cập nhật\s+6 tháng 11/)
  })

  it('render_ShouldNotShowUpdatedDate_WhenUpdatedOnSameDay', async () => {
    stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    await screen.findByRole('heading', { level: 1, name: 'Spring Boot 4.1 ra mắt' })
    expect(screen.queryByText(/Cập nhật/)).toBeNull()
  })

  it('render_ShouldKeepAttributionOutOfSourceBlockAndSourceNameAsText_WhenNewsIsShown', async () => {
    stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    const source = await screen.findByRole('region', { name: 'Nguồn gốc' })
    expect(within(source).queryByText(EDITORIAL_PROFILE.displayName)).toBeNull()
    expect(source).not.toHaveTextContent(EDITORIAL_PROFILE.responsibilityLabel)
    const headerLine = screen.getByText('Nguồn: Spring Blog')
    expect(headerLine.closest('a')).toBeNull()
    expect(within(headerLine).queryByRole('link')).toBeNull()
  })

  it('render_ShouldPlaceSourceBlockAfterBody_WhenNewsIsShown', async () => {
    stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    const title = await screen.findByRole('heading', { level: 1, name: 'Spring Boot 4.1 ra mắt' })
    const body = screen.getByRole('heading', { level: 2, name: 'Điểm mới' })
    const source = screen.getByRole('region', { name: 'Nguồn gốc' })
    expect(title.compareDocumentPosition(body) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(body.compareDocumentPosition(source) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('render_ShouldShowTypeLabelAndNoLearningArea_WhenNewsIsShown', async () => {
    stubNews(publicNews())

    renderAppRoute('/news/spring-boot-4-1')

    await screen.findByRole('heading', { level: 1, name: 'Spring Boot 4.1 ra mắt' })
    expect(screen.getByText('Tin')).toBeInTheDocument()
    expect(screen.queryByRole('navigation', { name: 'Vị trí trong lộ trình học' })).toBeNull()
    expect(screen.queryByRole('navigation', { name: 'Bài trước và bài sau' })).toBeNull()
  })

  it('render_ShouldShowBodyAndNoSourceLink_WhenSourceUrlIsUnsafe', async () => {
    stubNews(publicNews({ source: { name: 'Spring Blog', url: 'javascript:alert(1)', publishedAt: null } }))

    renderAppRoute('/news/spring-boot-4-1')

    expect(await screen.findByRole('heading', { level: 2, name: 'Điểm mới' })).toBeInTheDocument()
    const source = screen.getByRole('region', { name: 'Nguồn gốc' })
    expect(within(source).queryByRole('link')).toBeNull()
    expect(within(source).getByText('Spring Blog')).toBeInTheDocument()
    expect(source).not.toHaveTextContent('javascript')
    expect(within(source).queryByText(/Nguồn đăng/)).toBeNull()
  })

  it('render_ShouldOpenBodyExternalLinkInNewTab_WhenBodyHasExternalLink', async () => {
    stubNews(publicNews({ bodyMarkdown: 'Xem [tài liệu](https://example.com/docs) thêm.' }))

    renderAppRoute('/news/spring-boot-4-1')

    const link = await screen.findByRole('link', { name: 'tài liệu (mở trong tab mới)' })
    expect(link).toHaveAttribute('target', '_blank')
    expect(link).toHaveAttribute('rel', 'noopener noreferrer external')
  })

  it('render_ShouldShowGenericNotFound_WhenNewsIsNotPublic', async () => {
    new StubApiServer().on('GET', newsApiPath('an-di'), problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute('/news/an-di')

    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('retry_ShouldLoadNews_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    new StubApiServer()
      .on('GET', NEWS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, publicNews()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/news/spring-boot-4-1')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được tin do lỗi kết nối hoặc máy chủ.')
    expect(screen.queryByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Spring Boot 4.1 ra mắt' })).toBeInTheDocument()
  })
})
