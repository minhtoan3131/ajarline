import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  JAVA_TOPIC_REF,
  SPRING_TOPIC_REF,
  topicDiscovery,
  TOPICS_PATH,
} from '../features/topic-browsing/fixture/publicTopicFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

describe('TopicIndexPage', () => {
  it('render_ShouldShowLoadingFirst_WhenTopicsAreRequested', () => {
    // Given
    new StubApiServer().on('GET', TOPICS_PATH, jsonResponse(200, topicDiscovery(JAVA_TOPIC_REF))).install()

    // When
    renderAppRoute('/topics')

    // Then
    expect(screen.getByRole('status')).toHaveTextContent('Đang tải danh sách chủ đề…')
  })

  it('render_ShouldLinkTopicsInServerOrder_WhenTopicsArePublic', async () => {
    // Given: not alphabetical on purpose, so a client-side sort would fail
    const api = new StubApiServer()
      .on('GET', TOPICS_PATH, jsonResponse(200, topicDiscovery(SPRING_TOPIC_REF, JAVA_TOPIC_REF)))
      .install()

    // When
    renderAppRoute('/topics')

    // Then
    const list = await screen.findByRole('list', { name: 'Các chủ đề' })
    const links = within(list).getAllByRole('link')
    expect(links.map((link) => link.textContent)).toEqual(['Spring Boot', 'Java'])
    expect(links[0]).toHaveAttribute('href', '/topics/spring-boot')
    expect(screen.getByRole('heading', { level: 1, name: 'Chủ đề' })).toBeInTheDocument()
    expect(api.requests.map((request) => request.path)).toEqual([TOPICS_PATH])
  })

  it('render_ShouldShowEmptyMessage_WhenNoTopicIsPublic', async () => {
    // Given
    new StubApiServer().on('GET', TOPICS_PATH, jsonResponse(200, topicDiscovery())).install()

    // When
    renderAppRoute('/topics')

    // Then
    expect(await screen.findByText('Chưa có chủ đề nào có nội dung công khai.')).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Các chủ đề' })).toBeNull()
  })

  it('retry_ShouldLoadTopics_WhenTechnicalErrorIsFollowedBySuccess', async () => {
    // Given
    new StubApiServer()
      .on('GET', TOPICS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, topicDiscovery(JAVA_TOPIC_REF)))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/topics')
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách chủ đề')

    // When
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    // Then
    expect(await screen.findByRole('link', { name: 'Java' })).toHaveAttribute('href', '/topics/java')
  })
})
