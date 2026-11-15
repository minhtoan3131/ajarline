import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN, givenAnonymousVisitor, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  activeTopic,
  inactiveTopic,
  TOPIC_ID,
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
    givenAnonymousVisitor(new StubApiServer()).install()

    renderAppRoute(ROUTE)

    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })

  it('render_ShouldListEveryTopicInServerOrder_WhenTopicsExist', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic(), inactiveTopic())))
      .install()

    renderAppRoute(ROUTE)

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
    givenSignedInAdmin(new StubApiServer()).on('GET', TOPICS_PATH, jsonResponse(200, topicList())).install()

    renderAppRoute(ROUTE)

    expect(await screen.findByText('Chưa có Topic nào.')).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('render_ShouldOfferRetry_WhenListCannotBeLoaded', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, topicList(activeTopic())))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách Topic')

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('table', { name: 'Danh sách Topic' })).toBeInTheDocument()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })
})

describe('TopicManagementPage create form', () => {
  it('create_ShouldSendFullPayloadAndReloadList_WhenFormIsValid', async () => {
    const created = activeTopic()
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()), jsonResponse(200, topicList(created)))
      .on('POST', TOPICS_PATH, jsonResponse(201, created, { Location: topicPath(created.id) }))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java', description: 'Ngôn ngữ và nền tảng Java.' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Đã tạo Topic.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', TOPICS_PATH)
    expect(create.body).toEqual({ slug: 'java', name: 'Java', description: 'Ngôn ngữ và nền tảng Java.', active: true })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(await screen.findByRole('table', { name: 'Danh sách Topic' })).toHaveTextContent('Java')
    expect(screen.queryByLabelText('Slug')).toBeNull()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })

  it('create_ShouldSendNullDescription_WhenDescriptionIsEmpty', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, jsonResponse(201, activeTopic({ description: null })))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    await screen.findByText('Đã tạo Topic.')
    expect(api.requestsTo('POST', TOPICS_PATH)[0].body).toEqual({
      slug: 'java',
      name: 'Java',
      description: null,
      active: true,
    })
  })

  it('create_ShouldSendInactiveTopic_WhenActiveBoxIsCleared', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, jsonResponse(201, activeTopic({ active: false })))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    await user.click(screen.getByLabelText('Cho phép gán Topic này cho bài viết mới'))
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    await screen.findByText('Đã tạo Topic.')
    expect(api.requestsTo('POST', TOPICS_PATH)[0].body).toMatchObject({ active: false })
  })

  it('create_ShouldShowErrorUnderSlugAndKeepInput_WhenSlugIsTaken', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(409, 'SLUG_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Lỗi: Slug này đã được Topic khác sử dụng.')).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
    expect(screen.getByLabelText('Tên')).toHaveValue('Java')
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(1)
  })

  it('create_ShouldExplainLimitsAndKeepInput_WhenValidationFails', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(422, 'VALIDATION_FAILED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'khong hop le', name: 'Java' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Giá trị chưa hợp lệ')
    expect(screen.getByLabelText('Slug')).toHaveValue('khong hop le')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('create_ShouldAskToRepeatAndRenewToken_WhenCsrfProofIsInvalid', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(403, 'CSRF_INVALID'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Phiên làm việc vừa được làm mới')
    await waitFor(() => expect(api.requestsTo('GET', '/api/auth/v1/csrf')).toHaveLength(2))
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
  })

  it('create_ShouldKeepFormAndWarn_WhenSessionHasExpired', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .on('POST', TOPICS_PATH, problemResponse(401, 'AUTHENTICATION_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { slug: 'java', name: 'Java' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.')).toBeInTheDocument()
    expect(screen.getByText(/Phiên đăng nhập đã hết hạn/)).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
    expect(screen.getByTestId('location')).toHaveTextContent(ROUTE)
  })

  it('cancel_ShouldCloseFormAndUnlockOpeners_WhenChangesAreDiscarded', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await user.type(screen.getByLabelText('Tên'), 'Tạm')
    expect(screen.getByRole('button', { name: 'Tạo Topic' })).toBeDisabled()
    expect(screen.getByText('Lưu hoặc hủy thay đổi hiện tại trước khi mở Topic khác.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Hủy' }))

    expect(screen.queryByLabelText('Tên')).toBeNull()
    expect(screen.getByRole('button', { name: 'Tạo Topic' })).toBeEnabled()
    expect(api.requestsTo('POST', TOPICS_PATH)).toHaveLength(0)
  })
})

describe('TopicManagementPage edit form', () => {
  it('edit_ShouldOpenFormWithCurrentValuesAndReadOnlySlug_WhenEditIsClicked', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic(), inactiveTopic())))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Sửa Topic Java' }))

    expect(screen.getByRole('heading', { level: 2, name: 'Sửa Topic' })).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveValue('java')
    expect(screen.getByLabelText('Slug')).toHaveAttribute('readonly')
    expect(screen.getByText('Slug đã cố định từ lúc tạo.')).toBeInTheDocument()
    expect(screen.getByLabelText('Tên')).toHaveValue('Java')
    expect(screen.getByLabelText('Mô tả')).toHaveValue('Ngôn ngữ và nền tảng Java.')
    expect(screen.getByLabelText('Cho phép gán Topic này cho bài viết mới')).toBeChecked()
  })

  it('save_ShouldSendOnlyEditableFieldsAndReloadList_WhenTopicIsEdited', async () => {
    const renamed = activeTopic({ name: 'Java Core', active: false })
    const api = givenSignedInAdmin(new StubApiServer())
      .on(
        'GET',
        TOPICS_PATH,
        jsonResponse(200, topicList(activeTopic(), inactiveTopic())),
        jsonResponse(200, topicList(renamed, inactiveTopic())),
      )
      .on('PUT', topicPath(TOPIC_ID), jsonResponse(200, renamed))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Sửa Topic Java' }))
    await user.clear(screen.getByLabelText('Tên'))
    await user.type(screen.getByLabelText('Tên'), 'Java Core')
    await user.click(screen.getByLabelText('Cho phép gán Topic này cho bài viết mới'))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu Topic.')).toBeInTheDocument()
    const [update] = api.requestsTo('PUT', topicPath(TOPIC_ID))
    expect(update.body).toEqual({ name: 'Java Core', description: 'Ngôn ngữ và nền tảng Java.', active: false })
    expect(update.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(await screen.findByRole('button', { name: 'Sửa Topic Java Core' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Slug')).toBeNull()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })

  it('save_ShouldSendNullDescription_WhenDescriptionIsCleared', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic())))
      .on('PUT', topicPath(TOPIC_ID), jsonResponse(200, activeTopic({ description: null })))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Sửa Topic Java' }))
    await user.clear(screen.getByLabelText('Mô tả'))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await screen.findByText('Đã lưu Topic.')
    expect(api.requestsTo('PUT', topicPath(TOPIC_ID))[0].body).toMatchObject({ description: null })
  })

  it('save_ShouldKeepOldNameInList_WhenServerRejectsTheEdit', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic())))
      .on('PUT', topicPath(TOPIC_ID), problemResponse(422, 'VALIDATION_FAILED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Sửa Topic Java' }))
    await user.clear(screen.getByLabelText('Tên'))
    await user.type(screen.getByLabelText('Tên'), 'Tên mới')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Giá trị chưa hợp lệ')
    expect(screen.getByLabelText('Tên')).toHaveValue('Tên mới')
    const table = screen.getByRole('table', { name: 'Danh sách Topic' })
    expect(table).toHaveTextContent('Java')
    expect(table).not.toHaveTextContent('Tên mới')
  })

  it('save_ShouldCloseFormAndReloadList_WhenTopicNoLongerExists', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic())), jsonResponse(200, topicList()))
      .on('PUT', topicPath(TOPIC_ID), problemResponse(404, 'TOPIC_NOT_FOUND'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Sửa Topic Java' }))
    await user.type(screen.getByLabelText('Tên'), ' mới')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('không còn tồn tại')
    expect(screen.queryByLabelText('Slug')).toBeNull()
    expect(await screen.findByText('Chưa có Topic nào.')).toBeInTheDocument()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })
})
