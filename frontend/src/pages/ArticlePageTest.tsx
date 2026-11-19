import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import { EDITORIAL_PROFILE } from '../shared/content/editorialProfile'
import {
  contentItem,
  JAVA_TOPIC_REF,
  publicTopic,
  SPRING_TOPIC_REF,
  topicContentPath,
  topicContentResponse,
  topicPath,
} from '../features/topic-browsing/fixture/publicTopicFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const ARTICLE_PATH = '/api/v1/articles/hieu-protobuf'

// Shape of the real public response, including fields the page ignores.
const PUBLIC_ARTICLE = {
  id: '0f8fad5b-d9cb-469f-a165-70867728950e',
  type: 'article',
  slug: 'hieu-protobuf',
  title: 'Hiểu Protobuf với Java',
  summary: 'Tóm tắt bài viết.',
  bodyMarkdown: '## Mở đầu\n\nNội dung bài.',
  topics: [],
  series: null,
  module: null,
  previousArticle: null,
  nextArticle: null,
  publishedAt: '2026-11-01T02:00:00Z',
  updatedAt: '2026-11-01T02:00:00Z',
}

describe('ArticlePage', () => {
  it('render_ShouldShowLoadingFirst_WhenArticleIsRequested', () => {
    // Given
    new StubApiServer().on('GET', ARTICLE_PATH, jsonResponse(200, PUBLIC_ARTICLE)).install()

    // When
    renderAppRoute('/articles/hieu-protobuf')

    // Then
    expect(screen.getByRole('status')).toHaveTextContent('Đang tải bài viết…')
  })

  it('render_ShouldShowArticleWithAttribution_WhenSlugIsPublic', async () => {
    // Given
    const api = new StubApiServer().on('GET', ARTICLE_PATH, jsonResponse(200, PUBLIC_ARTICLE)).install()

    // When
    renderAppRoute('/articles/hieu-protobuf')

    // Then
    expect(await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
    expect(screen.getByText('Tóm tắt bài viết.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Mở đầu' })).toBeInTheDocument()
    expect(screen.getByText(EDITORIAL_PROFILE.displayName)).toBeInTheDocument()
    expect(document.body).not.toHaveTextContent(EDITORIAL_PROFILE.shortBio)
    expect(api.requests.map((request) => request.path)).toEqual([ARTICLE_PATH])
  })

  it('render_ShouldNotShowHierarchyOrAdminLinks_WhenArticleIsStandalone', async () => {
    // Given
    new StubApiServer().on('GET', ARTICLE_PATH, jsonResponse(200, PUBLIC_ARTICLE)).install()

    // When
    renderAppRoute('/articles/hieu-protobuf')
    await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })

    // Then
    // The header has the main navigation; a standalone Article itself has none.
    expect(within(screen.getByRole('article')).queryByRole('navigation')).toBeNull()
    expect(screen.queryByRole('link', { name: /quản trị|admin|đăng nhập/i })).toBeNull()
  })

  it('render_ShouldShowGenericNotFound_WhenArticleIsNotPublic', async () => {
    // Given
    new StubApiServer().on('GET', ARTICLE_PATH, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    // When
    renderAppRoute('/articles/hieu-protobuf')

    // Then
    expect(await screen.findByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeInTheDocument()
    expect(screen.queryByText(/nháp|draft|unpublish|gỡ/i)).toBeNull()
  })

  it('retry_ShouldLoadArticle_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    // Given
    new StubApiServer()
      .on('GET', ARTICLE_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, PUBLIC_ARTICLE))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles/hieu-protobuf')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được bài viết')
    expect(screen.queryByText('Nội dung này không khả dụng hoặc không tồn tại.')).toBeNull()

    // When
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    // Then
    expect(await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })).toBeInTheDocument()
  })

  it('render_ShouldLinkTopicsToTopicHome_WhenArticleHasTopics', async () => {
    new StubApiServer()
      .on('GET', ARTICLE_PATH, jsonResponse(200, { ...PUBLIC_ARTICLE, topics: [JAVA_TOPIC_REF, SPRING_TOPIC_REF] }))
      .install()

    renderAppRoute('/articles/hieu-protobuf')

    const chips = await screen.findByRole('list', { name: 'Chủ đề của bài viết' })
    const links = within(chips).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Java', 'Spring Boot'])
    expect(links[1]).toHaveAttribute('href', '/topics/spring-boot')
  })

  it('render_ShouldNotShowTopicList_WhenArticleHasNoTopics', async () => {
    new StubApiServer().on('GET', ARTICLE_PATH, jsonResponse(200, PUBLIC_ARTICLE)).install()

    renderAppRoute('/articles/hieu-protobuf')
    await screen.findByRole('heading', { level: 1, name: 'Hiểu Protobuf với Java' })

    expect(screen.queryByRole('list', { name: 'Chủ đề của bài viết' })).toBeNull()
  })

  it('navigate_ShouldOpenTopicHome_WhenTopicChipIsClicked', async () => {
    new StubApiServer()
      .on('GET', ARTICLE_PATH, jsonResponse(200, { ...PUBLIC_ARTICLE, topics: [JAVA_TOPIC_REF] }))
      .on('GET', topicPath('java'), jsonResponse(200, publicTopic()))
      .on('GET', topicContentPath('java', 0), jsonResponse(200, topicContentResponse([contentItem()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles/hieu-protobuf')
    const chips = await screen.findByRole('list', { name: 'Chủ đề của bài viết' })

    await user.click(within(chips).getByRole('link', { name: 'Java' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Java' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/topics/java')
  })
})
