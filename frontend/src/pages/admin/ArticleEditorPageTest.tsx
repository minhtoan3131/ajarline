import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN, givenAnonymousVisitor, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  ARTICLE_ID,
  ARTICLE_PATH,
  ARTICLES_PATH,
  draftArticle,
  revisionHeaders,
} from '../../features/admin-content/fixture/articleAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/articles/${ARTICLE_ID}`

function givenLoadedDraft(api: StubApiServer, article = draftArticle()): StubApiServer {
  return givenSignedInAdmin(api).on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
}

describe('ArticleEditorPage', () => {
  it('render_ShouldRedirectToLogin_WhenSessionIsAnonymous', async () => {
    // Given
    givenAnonymousVisitor(new StubApiServer()).install()

    // When
    renderAppRoute('/admin/articles/new')

    // Then
    expect(await screen.findByLabelText('Tên đăng nhập')).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/admin/login')
  })

  it('save_ShouldCreateStandaloneDraft_WhenNewArticleIsSaved', async () => {
    // Given
    const created = draftArticle()
    const api = givenSignedInAdmin(new StubApiServer())
      .on('POST', ARTICLES_PATH, jsonResponse(201, created, { ...revisionHeaders(created), Location: ARTICLE_PATH }))
      .on('GET', ARTICLE_PATH, jsonResponse(200, created, revisionHeaders(created)))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/articles/new')
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Hiểu Protobuf với Java')
    await user.type(screen.getByLabelText('Slug'), 'hieu-protobuf')
    await user.type(screen.getByLabelText('Tóm tắt'), 'Tóm tắt bài viết.')
    await user.click(screen.getByLabelText('Nội dung Markdown'))
    await user.paste('## Mở đầu\n\nNội dung bài.')

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    expect(await screen.findByText('Đã tạo bản nháp.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', ARTICLES_PATH)
    expect(create.body).toEqual({
      title: 'Hiểu Protobuf với Java',
      slug: 'hieu-protobuf',
      summary: 'Tóm tắt bài viết.',
      bodyMarkdown: '## Mở đầu\n\nNội dung bài.',
      moduleId: null,
      position: null,
      topicIds: [],
    })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('render_ShouldLoadArticleById_WhenEditorIsOpenedDirectly', async () => {
    // Given
    const api = givenLoadedDraft(new StubApiServer()).install()

    // When
    renderAppRoute(EDIT_ROUTE)

    // Then
    expect(await screen.findByLabelText('Tiêu đề')).toHaveValue('Hiểu Protobuf với Java')
    expect(screen.getByLabelText('Nội dung Markdown')).toHaveValue('## Mở đầu\n\nNội dung bài.')
    expect(screen.getByText('Đã lưu mọi thay đổi')).toBeInTheDocument()
    expect(api.requestsTo('POST', ARTICLES_PATH)).toHaveLength(0)
    expect(screen.queryByLabelText(/module|chủ đề|topic/i)).toBeNull()
  })

  it('save_ShouldSendIfMatchAndUseNewEtag_WhenArticleIsSavedTwice', async () => {
    // Given
    const second = draftArticle({ revision: 2, title: 'Tiêu đề mới' })
    const third = draftArticle({ revision: 3, title: 'Tiêu đề mới hơn' })
    const api = givenLoadedDraft(new StubApiServer())
      .on(
        'PUT',
        ARTICLE_PATH,
        jsonResponse(200, second, revisionHeaders(second)),
        jsonResponse(200, third, revisionHeaders(third)),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')

    // When
    await user.clear(title)
    await user.type(title, 'Tiêu đề mới')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await screen.findByText('Đã lưu.')
    await user.type(title, ' hơn')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    await waitFor(() => expect(api.requestsTo('PUT', ARTICLE_PATH)).toHaveLength(2))
    const [first, next] = api.requestsTo('PUT', ARTICLE_PATH)
    expect(first.headers.get('If-Match')).toBe('"1"')
    expect(next.headers.get('If-Match')).toBe('"2"')
    expect(first.body).toMatchObject({ title: 'Tiêu đề mới', moduleId: null, position: null, topicIds: [] })
  })

  it('save_ShouldKeepCurrentEtag_WhenServerReportsNoChange', async () => {
    // Given
    const unchanged = draftArticle()
    const api = givenLoadedDraft(new StubApiServer())
      .on('PUT', ARTICLE_PATH, jsonResponse(200, unchanged, revisionHeaders(unchanged)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await screen.findByText('Đã lưu.')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    await waitFor(() => expect(api.requestsTo('PUT', ARTICLE_PATH)).toHaveLength(2))
    expect(api.requestsTo('PUT', ARTICLE_PATH)[1].headers.get('If-Match')).toBe('"1"')
  })

  it('save_ShouldKeepInputAndOfferReload_WhenRevisionIsStale', async () => {
    // Given
    const newer = draftArticle({ revision: 2, title: 'Tiêu đề từ nơi khác' })
    const api = givenSignedInAdmin(new StubApiServer())
      .on(
        'GET',
        ARTICLE_PATH,
        jsonResponse(200, draftArticle(), revisionHeaders(draftArticle())),
        jsonResponse(200, newer, revisionHeaders(newer)),
      )
      .on('PUT', ARTICLE_PATH, problemResponse(412, 'REVISION_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')
    await user.clear(title)
    await user.type(title, 'Tiêu đề của tôi')

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('đã được thay đổi ở nơi khác')
    expect(title).toHaveValue('Tiêu đề của tôi')
    expect(api.requestsTo('PUT', ARTICLE_PATH)).toHaveLength(1)
    expect(screen.queryByText('Đã lưu.')).toBeNull()

    // When
    await user.click(screen.getByRole('button', { name: 'Tải bản mới nhất (bỏ thay đổi chưa lưu)' }))

    // Then
    await waitFor(() => expect(screen.getByLabelText('Tiêu đề')).toHaveValue('Tiêu đề từ nơi khác'))
    expect(api.requestsTo('PUT', ARTICLE_PATH)).toHaveLength(1)
  })

  it('save_ShouldShowErrorUnderSlug_WhenSlugIsTaken', async () => {
    // Given
    givenLoadedDraft(new StubApiServer()).on('PUT', ARTICLE_PATH, problemResponse(409, 'SLUG_CONFLICT')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Slug')

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    expect(await screen.findByText('Lỗi: Slug này đã được bài khác sử dụng.')).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveAttribute('aria-invalid', 'true')
  })

  it('save_ShouldShowServerDetail_WhenValidationFails', async () => {
    // Given
    givenLoadedDraft(new StubApiServer())
      .on('PUT', ARTICLE_PATH, problemResponse(422, 'VALIDATION_FAILED', { detail: 'Title must not be blank.' }))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.clear(await screen.findByLabelText('Tiêu đề'))

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('Dữ liệu chưa hợp lệ: Title must not be blank.')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('save_ShouldKeepEditorAndWarn_WhenSessionHasExpired', async () => {
    // Given
    givenLoadedDraft(new StubApiServer())
      .on('PUT', ARTICLE_PATH, problemResponse(401, 'AUTHENTICATION_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const body = await screen.findByLabelText('Nội dung Markdown')
    await user.type(body, ' Thêm chữ.')

    // When
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Then
    expect(await screen.findByText(/Phiên đăng nhập đã hết hạn/)).toBeInTheDocument()
    expect(screen.getByText('Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.')).toBeInTheDocument()
    expect(body).toHaveValue('## Mở đầu\n\nNội dung bài. Thêm chữ.')
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.queryByText('Đã lưu.')).toBeNull()
  })

  it('render_ShouldMakeSlugReadOnly_WhenArticleWasPublishedBefore', async () => {
    // Given
    givenLoadedDraft(
      new StubApiServer(),
      draftArticle({ status: 'UNPUBLISHED', publishedAt: '2026-11-01T02:00:00Z', unpublishedAt: '2026-11-02T02:00:00Z' }),
    ).install()

    // When
    renderAppRoute(EDIT_ROUTE)

    // Then
    expect(await screen.findByLabelText('Slug')).toHaveAttribute('readonly')
    expect(screen.getByText('Slug đã cố định từ lần xuất bản đầu tiên.')).toBeInTheDocument()
    expect(screen.getByText('Trạng thái: Đã gỡ xuất bản')).toBeInTheDocument()
  })
})
