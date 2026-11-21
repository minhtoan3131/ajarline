import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN, givenAnonymousVisitor, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  activeConcept,
  CONCEPTS_PATH,
  conceptList,
  conceptPath,
  holdRequestsTo,
  inactiveConcept,
} from '../../features/admin-vocabulary/fixture/conceptAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const ROUTE = '/admin/concepts'

type User = ReturnType<typeof userEvent.setup>

async function openCreateForm(user: User) {
  await user.click(await screen.findByRole('button', { name: 'Tạo Concept' }))
}

async function fillCreateForm(user: User, values: { key: string; displayName: string; description?: string }) {
  await user.type(screen.getByLabelText('Key'), values.key)
  await user.type(screen.getByLabelText('Tên'), values.displayName)
  if (values.description !== undefined) {
    await user.type(screen.getByLabelText('Mô tả'), values.description)
  }
}

/** Adds one row and types the text into the last row. */
async function addExpression(user: User, text: string) {
  await user.click(screen.getByRole('button', { name: 'Thêm expression' }))
  const inputs = screen.getAllByLabelText(/^Expression \d+$/)
  await user.type(inputs[inputs.length - 1], text)
}

function expressionTexts(): string[] {
  return screen.queryAllByLabelText(/^Expression \d+$/).map((input) => (input as HTMLInputElement).value)
}

describe('ConceptManagementPage', () => {
  it('render_ShouldRedirectToLogin_WhenSessionIsAnonymous', async () => {
    givenAnonymousVisitor(new StubApiServer()).install()

    renderAppRoute(ROUTE)

    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })

  it('render_ShouldListEveryConceptInServerOrder_WhenConceptsExist', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList(activeConcept(), inactiveConcept())))
      .install()

    renderAppRoute(ROUTE)

    const rows = await screen.findAllByRole('row')
    expect(rows).toHaveLength(3)
    const first = within(rows[1]).getAllByRole('cell')
    expect(first[0]).toHaveTextContent('thread')
    expect(first[1]).toHaveTextContent('Thread')
    expect(first[2]).toHaveTextContent('Đang bật')
    expect(first[3]).toHaveTextContent('3')
    const second = within(rows[2]).getAllByRole('cell')
    expect(second[0]).toHaveTextContent('spring-boot')
    expect(second[1]).toHaveTextContent('Spring Boot')
    expect(second[2]).toHaveTextContent('Đã tắt')
    expect(second[3]).toHaveTextContent('0')
  })

  it('render_ShouldShowEmptyMessage_WhenNoConceptExists', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()

    renderAppRoute(ROUTE)

    expect(await screen.findByText(/Chưa có Concept nào/)).toBeInTheDocument()
    expect(screen.queryByRole('table')).toBeNull()
  })

  it('render_ShouldOfferRetryAndNotSayEmpty_WhenListCannotBeLoaded', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, conceptList(activeConcept())))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    expect(await screen.findByRole('alert')).toHaveTextContent('Không tải được danh sách Concept')
    expect(screen.queryByText(/Chưa có Concept nào/)).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('table', { name: 'Danh sách Concept' })).toBeInTheDocument()
    expect(api.requestsTo('GET', CONCEPTS_PATH)).toHaveLength(2)
  })

  it('render_ShouldExplainConceptIsRetrievalVocabulary_WhenPageOpens', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()

    renderAppRoute(ROUTE)

    expect(await screen.findByText(/từ vựng truy hồi, không phải Topic/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Concept' })).toBeInTheDocument()
  })
})

describe('ConceptManagementPage create form', () => {
  it('create_ShouldSendFullPayloadAndReloadList_WhenFormIsValid', async () => {
    const created = activeConcept()
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()), jsonResponse(200, conceptList(created)))
      .on('POST', CONCEPTS_PATH, jsonResponse(201, created, { Location: conceptPath(created.id) }))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread', description: 'Luồng thực thi trong chương trình.' })
    await addExpression(user, 'multithreading')
    await addExpression(user, 'đa luồng')

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Đã tạo Concept.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', CONCEPTS_PATH)
    expect(create.body).toEqual({
      key: 'thread',
      displayName: 'Thread',
      description: 'Luồng thực thi trong chương trình.',
      active: true,
      expressions: [
        { expression: 'multithreading', languageHint: null, autoExpand: false },
        { expression: 'đa luồng', languageHint: null, autoExpand: false },
      ],
    })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(await screen.findByRole('table', { name: 'Danh sách Concept' })).toHaveTextContent('Thread')
    expect(screen.queryByLabelText('Key')).toBeNull()
    expect(api.requestsTo('GET', CONCEPTS_PATH)).toHaveLength(2)
  })

  it('create_ShouldSendNullDescription_WhenDescriptionIsEmpty', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, jsonResponse(201, inactiveConcept()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'spring-boot', displayName: 'Spring Boot' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    await screen.findByText('Đã tạo Concept.')
    expect(api.requestsTo('POST', CONCEPTS_PATH)[0].body).toEqual({
      key: 'spring-boot',
      displayName: 'Spring Boot',
      description: null,
      active: true,
      expressions: [],
    })
  })

  it('create_ShouldSendInactiveConcept_WhenActiveBoxIsCleared', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, jsonResponse(201, inactiveConcept()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'spring-boot', displayName: 'Spring Boot' })

    await user.click(screen.getByLabelText('Concept đang bật'))
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    await screen.findByText('Đã tạo Concept.')
    expect(api.requestsTo('POST', CONCEPTS_PATH)[0].body).toMatchObject({ active: false })
  })

  it('create_ShouldSendChosenLanguageAndAutoExpand_WhenRowIsChanged', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, jsonResponse(201, activeConcept()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread' })
    await addExpression(user, 'đa luồng')

    await user.selectOptions(screen.getByLabelText('Ngôn ngữ của expression 1'), 'vi')
    await user.click(screen.getByLabelText('Tự mở rộng expression 1'))
    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    await screen.findByText('Đã tạo Concept.')
    expect(api.requestsTo('POST', CONCEPTS_PATH)[0].body).toMatchObject({
      expressions: [{ expression: 'đa luồng', languageHint: 'vi', autoExpand: true }],
    })
  })

  it('create_ShouldShowNoNoticeAndLockButtons_WhileRequestIsRunning', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, jsonResponse(201, activeConcept()))
      .install()
    const release = holdRequestsTo('POST')
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByRole('button', { name: 'Đang tạo…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Hủy' })).toBeDisabled()
    expect(screen.queryByText('Đã tạo Concept.')).toBeNull()
    release()
    expect(await screen.findByText('Đã tạo Concept.')).toBeInTheDocument()
    expect(api.requestsTo('POST', CONCEPTS_PATH)).toHaveLength(1)
  })

  it('create_ShouldShowErrorUnderKeyAndKeepInput_WhenKeyIsTaken', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, problemResponse(409, 'CONCEPT_KEY_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread', description: 'Mô tả' })
    await addExpression(user, 'multithreading')
    await addExpression(user, 'đa luồng')

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Lỗi: Key này đã được Concept khác sử dụng.')).toBeInTheDocument()
    expect(screen.getByLabelText('Key')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Key')).toHaveValue('thread')
    expect(screen.getByLabelText('Tên')).toHaveValue('Thread')
    expect(screen.getByLabelText('Mô tả')).toHaveValue('Mô tả')
    expect(expressionTexts()).toEqual(['multithreading', 'đa luồng'])
    expect(api.requestsTo('GET', CONCEPTS_PATH)).toHaveLength(1)
  })

  it('create_ShouldExplainDuplicateAndKeepInput_WhenExpressionsRepeatAfterNormalizing', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, problemResponse(422, 'DUPLICATE_CONCEPT_EXPRESSION'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread' })
    await addExpression(user, 'Thread')
    await addExpression(user, ' thread ')

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('trùng nhau sau khi chuẩn hoá')
    expect(alert).toHaveTextContent('giữ mỗi cách nói một lần')
    expect(expressionTexts()).toEqual(['Thread', ' thread '])
    expect(screen.getByLabelText('Key')).toHaveValue('thread')
  })

  it('create_ShouldExplainLimitsAndKeepInput_WhenValidationFails', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, problemResponse(422, 'VALIDATION_FAILED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'khong hop le', displayName: 'Thread' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Giá trị chưa hợp lệ')
    expect(screen.getByLabelText('Key')).toHaveValue('khong hop le')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('create_ShouldKeepInputAndAllowRetry_WhenServerFails', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(201, activeConcept()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread' })
    await addExpression(user, 'đa luồng')

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Không kết nối được máy chủ')
    expect(screen.getByLabelText('Key')).toHaveValue('thread')
    expect(expressionTexts()).toEqual(['đa luồng'])

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Đã tạo Concept.')).toBeInTheDocument()
    expect(api.requestsTo('POST', CONCEPTS_PATH)).toHaveLength(2)
  })

  it('create_ShouldKeepFormAndWarn_WhenSessionHasExpired', async () => {
    givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .on('POST', CONCEPTS_PATH, problemResponse(401, 'AUTHENTICATION_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await fillCreateForm(user, { key: 'thread', displayName: 'Thread' })

    await user.click(screen.getByRole('button', { name: 'Tạo' }))

    expect(await screen.findByText('Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.')).toBeInTheDocument()
    expect(screen.getByText(/Phiên đăng nhập đã hết hạn/)).toBeInTheDocument()
    expect(screen.getByLabelText('Key')).toHaveValue('thread')
    expect(screen.getByTestId('location')).toHaveTextContent(ROUTE)
  })

  it('cancel_ShouldCloseFormAndUnlockOpeners_WhenChangesAreDiscarded', async () => {
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList()))
      .install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await user.type(screen.getByLabelText('Tên'), 'Tạm')
    expect(screen.getByRole('button', { name: 'Tạo Concept' })).toBeDisabled()
    expect(screen.getByText('Lưu hoặc hủy thay đổi hiện tại trước khi mở Concept khác.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Hủy' }))

    expect(screen.queryByLabelText('Tên')).toBeNull()
    expect(screen.getByRole('button', { name: 'Tạo Concept' })).toBeEnabled()
    expect(api.requestsTo('POST', CONCEPTS_PATH)).toHaveLength(0)
  })

  it('open_ShouldNotLockOpeners_WhenFormIsOpenAndUntouched', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)

    await openCreateForm(user)

    expect(screen.getByRole('button', { name: 'Tạo Concept' })).toBeEnabled()
    expect(screen.queryByText('Có thay đổi chưa lưu')).toBeNull()
  })
})

describe('ConceptManagementPage expression rows', () => {
  it('addExpression_ShouldDisableAddButton_WhenFiftyRowsExist', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)

    for (let count = 0; count < 50; count += 1) {
      await user.click(screen.getByRole('button', { name: 'Thêm expression' }))
    }

    expect(screen.getAllByLabelText(/^Expression \d+$/)).toHaveLength(50)
    expect(screen.getByRole('button', { name: 'Thêm expression' })).toBeDisabled()
    expect(screen.getByText('50/50')).toBeInTheDocument()
  }, 30_000)

  it('removeExpression_ShouldKeepTextOfOtherRows_WhenMiddleRowIsRemoved', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)
    await addExpression(user, 'alpha')
    await addExpression(user, 'beta')
    await addExpression(user, 'gamma')

    await user.click(screen.getByRole('button', { name: 'Bỏ expression 2' }))

    expect(expressionTexts()).toEqual(['alpha', 'gamma'])
  })

  it('addExpression_ShouldStartRowWithNoLanguageAndAutoExpandOff_WhenRowIsAdded', async () => {
    givenSignedInAdmin(new StubApiServer()).on('GET', CONCEPTS_PATH, jsonResponse(200, conceptList())).install()
    const user = userEvent.setup()
    renderAppRoute(ROUTE)
    await openCreateForm(user)

    await user.click(screen.getByRole('button', { name: 'Thêm expression' }))

    expect(screen.getByLabelText('Ngôn ngữ của expression 1')).toHaveValue('')
    expect(screen.getByLabelText('Tự mở rộng expression 1')).not.toBeChecked()
  })
})
