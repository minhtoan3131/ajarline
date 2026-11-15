import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN, givenAnonymousVisitor, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  activeTopic,
  inactiveTopic,
  TOPICS_PATH,
  topicList,
  topicPath,
} from '../../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const ROUTE = '/admin/topics'

type User = ReturnType<typeof userEvent.setup>

async function openCreateForm(user: User) {
  await user.click(await screen.findByRole('button', { name: 'Tạo Topic' }))
}

async function fillCreateForm(user: User, values: { slug: string; name: string; description?: string }) {
  await user.type(screen.getByLabelText('Slug'), values.slug)
  await user.type(screen.getByLabelText('Tên'), values.name)
  if (values.description !== undefined) {
    await user.type(screen.getByLabelText('Mô tả'), values.description)
  }
}

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

describe('TopicManagementPage create form', () => {
  it('create_ShouldSendFullPayloadAndReloadList_WhenFormIsValid', async () => {
    // Given
    const created = activeTopic()
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()), jsonResponse(200, topicList(created)))
      .on('POST', TOPICS_PATH, jsonResponse(201, created, { Location: topicPath(created.id) }))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java', description: 'Ngôn ngữ và nền tảng Java.' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    expect(await screen.findByText('Đã tạo Topic.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', TOPICS_PATH)
    expect(create.body).toEqual({ slug: 'java', name: 'Java', description: 'Ngôn ngữ và nền tảng Java.', active: true })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(await screen.findByRole('table', { name: 'Danh sách Topic' })).toHaveTextContent('Java')
    expect(screen.queryByLabelText('Slug')).toBeNull()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })

  it('create_ShouldSendNullDescription_WhenDescriptionIsEmpty', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, jsonResponse(201, activeTopic({ description: null })))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    await screen.findByText('Đã tạo Topic.')
    expect(api.requestsTo('POST', TOPICS_PATH)[0].body).toEqual({
      slug: 'java',
      name: 'Java',
      description: null,
      active: true,
    })
  })

  it('create_ShouldSendInactiveTopic_WhenActiveBoxIsCleared', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, jsonResponse(201, activeTopic({ active: false })))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    // When
    await user.click(screen.getByLabelText('Cho phép gán Topic này cho bài viết mới'))
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    await screen.findByText('Đã tạo Topic.')
    expect(api.requestsTo('POST', TOPICS_PATH)[0].body).toMatchObject({ active: false })
  })

  it('create_ShouldShowErrorUnderSlugAndKeepInput_WhenSlugIsTaken', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(409, 'SLUG_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    expect(await screen.findByText('Lỗi: Slug này đã được Topic khác sử dụng.')).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
    expect(screen.getByLabelText('Tên')).toHaveValue('Java')
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(1)
  })

  it('create_ShouldExplainLimitsAndKeepInput_WhenValidationFails', async () => {
    // Given
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(422, 'VALIDATION_FAILED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'khong hop le', name: 'Java' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('Giá trị chưa hợp lệ')
    expect(screen.getByLabelText('Slug')).toHaveValue('khong hop le')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('create_ShouldAskToRepeatAndRenewToken_WhenCsrfProofIsInvalid', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(403, 'CSRF_INVALID'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('Phiên làm việc vừa được làm mới')
    await waitFor(() => expect(api.requestsTo('GET', '/api/auth/v1/csrf')).toHaveLength(2))
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
  })

  it('create_ShouldKeepFormAndWarn_WhenSessionHasExpired', async () => {
    // Given
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(401, 'AUTHENTICATION_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    // When
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    // Then
    expect(await screen.findByText('Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.')).toBeInTheDocument()
    expect(screen.getByText(/Phiên đăng nhập đã hết hạn/)).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
    expect(screen.getByTestId('location')).toHaveTextContent(ROUTE)
  })

  it('cancel_ShouldCloseFormAndUnlockOpeners_WhenChangesAreDiscarded', async () => {
    // Given
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await user.type(screen.getByLabelText('Tên'), 'Tạm')
    expect(screen.getByRole('button', { name: 'Tạo Topic' })).toBeDisabled()
    expect(screen.getByText('Lưu hoặc hủy thay đổi hiện tại trước khi mở Topic khác.')).toBeInTheDocument()

    // When
    await user.click(screen.getByRole('button', { name: 'Hủy' }))

    // Then
    expect(screen.queryByLabelText('Tên')).toBeNull()
    expect(screen.getByRole('button', { name: 'Tạo Topic' })).toBeEnabled()
    expect(api.requestsTo('POST', TOPICS_PATH)).toHaveLength(0)
  })
})
