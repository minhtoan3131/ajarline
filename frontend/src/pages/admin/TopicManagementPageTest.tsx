import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenAnonymousVisitor, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  activeTopic,
  inactiveTopic,
  TOPICS_PATH,
  topicList,
} from '../../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const ROUTE = '/admin/topics'

describe('TopicManagementPage', () => {
  it('render_ShouldRedirectToLogin_WhenSessionIsAnonymous', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer()).install()

    // When
    renderAppRoute(ROUTE)

    // Then
    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })

  it('render_ShouldListEveryTopicInServerOrder_WhenTopicsExist', async () => {
    // Given
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic(), inactiveTopic())))
      .install()

    // When
    renderAppRoute(ROUTE)

    // Then
    const rows = await screen.findAllByRole('row')
    expect(rows).toHaveLength(3)
    expect(rows[1]).toHaveTextContent('Java')
    expect(rows[1]).toHaveTextContent('java')
    expect(rows[1]).toHaveTextContent('Ngôn ngữ và nền tảng Java.')
    expect(rows[1]).toHaveTextContent('Đang bật')
    expect(rows[2]).toHaveTextContent('Spring')
    expect(rows[2]).toHaveTextContent('Không có mô tả.')
    expect(rows[2]).toHaveTextContent('Đã tắt')
  })

  it('render_ShouldShowEmptyMessage_WhenNoTopicExists', async () => {
    // Given
    givenSignedInAdmin(new StubApiServer()).on('GET', TOPICS_PATH, jsonResponse(200, topicList())).install()

    // When
    renderAppRoute(ROUTE)

    // Then
    expect(await screen.findByText('Chưa có Topic nào.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('render_ShouldOfferRetry_WhenListCannotBeLoaded', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, topicList(activeTopic())))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách Topic')

    // When
    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    // Then
    expect(await screen.findByRole('table', { name: 'Danh sách Topic' })).toBeInTheDocument()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })
})
