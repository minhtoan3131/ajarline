import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN } from '../../features/admin-auth/fixture/adminSessionFixture'
import { CONTENT_INDEX_PATH } from '../../features/admin-content/contentRoutes'
import { givenContentIndex } from '../../features/admin-content/fixture/contentIndexFixture'
import {
  draftNews,
  givenNewsEditorSession,
  holdRequests,
  newsHeaders,
  NEWS_API_PATH,
  NEWS_ID,
  NEWS_PATH,
} from '../../features/admin-news/fixture/newsAdminFixture'
import { isoToLocalInput, localInputToIso } from '../../features/admin-news/model/news'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/news/${NEWS_ID}`

function givenLoadedDraft(news = draftNews()): StubApiServer {
  return givenNewsEditorSession(new StubApiServer()).on('GET', NEWS_PATH, jsonResponse(200, news, newsHeaders(news)))
}

describe('NewsEditorPage', () => {
  it('save_ShouldCreateDraftAndReplaceUrl_WhenNewNewsIsSaved', async () => {
    const created = draftNews()
    const api = givenNewsEditorSession(new StubApiServer())
      .on('POST', NEWS_API_PATH, jsonResponse(201, created, { ...newsHeaders(created), Location: NEWS_PATH }))
      .on('GET', NEWS_PATH, jsonResponse(200, created, newsHeaders(created)))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/news/new', { previous: CONTENT_INDEX_PATH })
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Spring Boot 4.1 ra mắt')
    await user.type(screen.getByLabelText('Slug'), 'spring-boot-4-1-ra-mat')
    await user.type(screen.getByLabelText('Tóm tắt'), 'Bản phát hành mới của Spring Boot.')
    await user.type(screen.getByLabelText('Tên nguồn'), 'Spring Blog')
    await user.type(screen.getByLabelText('URL nguồn'), 'https://spring.io/blog/spring-boot-4-1')
    await user.click(screen.getByLabelText('Nội dung Markdown'))
    await user.paste('## Điểm mới\n\nNội dung tin.')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Saving moves to the new route, which mounts a fresh editor. Wait for it first,
    // or the notice is found in the old editor just before it is removed.
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE))
    expect(await screen.findByText('Đã tạo tin nháp.')).toBeInTheDocument()
    const [create] = api.requestsTo('POST', NEWS_API_PATH)
    expect(create.body).toEqual({
      title: 'Spring Boot 4.1 ra mắt',
      slug: 'spring-boot-4-1-ra-mat',
      summary: 'Bản phát hành mới của Spring Boot.',
      bodyMarkdown: '## Điểm mới\n\nNội dung tin.',
      source: { name: 'Spring Blog', url: 'https://spring.io/blog/spring-boot-4-1', publishedAt: null },
      topicIds: [],
    })
    expect(create.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('save_ShouldLeaveNoCreateUrlInHistory_WhenNewNewsIsSaved', async () => {
    const created = draftNews()
    givenContentIndex(
      givenNewsEditorSession(new StubApiServer())
        .on('POST', NEWS_API_PATH, jsonResponse(201, created, newsHeaders(created)))
        .on('GET', NEWS_PATH, jsonResponse(200, created, newsHeaders(created))),
      [],
    ).install()
    const user = userEvent.setup()
    renderAppRoute('/admin/news/new', { previous: CONTENT_INDEX_PATH })
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Tin mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE))

    await user.click(screen.getByRole('button', { name: 'Back (test)' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Nội dung' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
  })

  it('render_ShouldLoadNewsById_WhenEditorIsOpenedDirectly', async () => {
    const news = draftNews()
    const api = givenLoadedDraft(news).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByLabelText('Tiêu đề')).toHaveValue('Spring Boot 4.1 ra mắt')
    expect(screen.getByLabelText('Tên nguồn')).toHaveValue('Spring Blog')
    expect(screen.getByLabelText('URL nguồn')).toHaveValue('https://spring.io/blog/spring-boot-4-1')
    expect(screen.getByLabelText('Ngày nguồn đăng (không bắt buộc)')).toHaveValue(isoToLocalInput(news.source.publishedAt))
    expect(screen.getByLabelText('Nội dung Markdown')).toHaveValue('## Điểm mới\n\nNội dung tin.')
    expect(screen.getByText('Đã lưu mọi thay đổi')).toBeInTheDocument()
    expect(api.requestsTo('POST', NEWS_API_PATH)).toHaveLength(0)
  })

  it('save_ShouldSendIfMatchAndUseNewEtag_WhenNewsIsSavedTwice', async () => {
    const second = draftNews({ revision: 2, title: 'Tiêu đề mới' })
    const third = draftNews({ revision: 3, title: 'Tiêu đề mới hơn' })
    const api = givenLoadedDraft()
      .on('PUT', NEWS_PATH, jsonResponse(200, second, newsHeaders(second)), jsonResponse(200, third, newsHeaders(third)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')

    await user.clear(title)
    await user.type(title, 'Tiêu đề mới')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Lưu' }))
    await screen.findByText('Đã lưu.')
    await user.type(title, ' hơn')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.requestsTo('PUT', NEWS_PATH)).toHaveLength(2))
    const [first, next] = api.requestsTo('PUT', NEWS_PATH)
    expect(first.headers.get('If-Match')).toBe('"1"')
    expect(next.headers.get('If-Match')).toBe('"2"')
  })

  it('save_ShouldSendUnchangedValues_WhenFormIsNotEdited', async () => {
    const news = draftNews()
    const api = givenLoadedDraft(news).on('PUT', NEWS_PATH, jsonResponse(200, news, newsHeaders(news))).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await screen.findByText('Đã lưu.')
    expect(api.requestsTo('PUT', NEWS_PATH)[0].body).toEqual({
      title: news.title,
      slug: news.slug,
      summary: news.summary,
      bodyMarkdown: news.bodyMarkdown,
      source: news.source,
      topicIds: news.topicIds,
    })
  })

  it('save_ShouldSendDateAsIso_WhenSourceDateIsChosen', async () => {
    const api = givenLoadedDraft().on('PUT', NEWS_PATH, jsonResponse(200, draftNews(), newsHeaders(draftNews()))).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const date = await screen.findByLabelText('Ngày nguồn đăng (không bắt buộc)')

    fireEvent.change(date, { target: { value: '2026-11-18T09:30' } })
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.requestsTo('PUT', NEWS_PATH)).toHaveLength(1))
    expect(api.requestsTo('PUT', NEWS_PATH)[0].body).toMatchObject({
      source: { publishedAt: localInputToIso('2026-11-18T09:30') },
    })
  })

  it('save_ShouldSendUrlAsTypedAndShowServerError_WhenUrlIsNotValid', async () => {
    const api = givenLoadedDraft()
      .on(
        'PUT',
        NEWS_PATH,
        problemResponse(422, 'VALIDATION_FAILED', { detail: 'source.url must be an absolute http or https URL.' }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const url = await screen.findByLabelText('URL nguồn')
    await user.clear(url)
    await user.type(url, 'abc')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('source.url must be an absolute http or https URL.')
    expect(api.requestsTo('PUT', NEWS_PATH)[0].body).toMatchObject({ source: { url: 'abc' } })
    expect(url).toHaveValue('abc')
  })

  it('save_ShouldShowErrorInSourceBlock_WhenSourceIsRequired', async () => {
    givenLoadedDraft(draftNews({ status: 'PUBLISHED', revision: 3, publishedAt: '2026-11-19T03:00:00Z' }))
      .on('PUT', NEWS_PATH, problemResponse(422, 'NEWS_SOURCE_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const url = await screen.findByLabelText('URL nguồn')
    await user.clear(url)

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    const block = await screen.findByRole('group', { name: 'Nguồn tin' })
    expect(within(block).getByText('Lỗi: Chưa đủ nguồn để xuất bản.')).toBeInTheDocument()

    await user.type(url, 'https://spring.io')

    expect(within(block).queryByText('Lỗi: Chưa đủ nguồn để xuất bản.')).toBeNull()
  })

  it('save_ShouldShowErrorUnderSlug_WhenSlugIsTaken', async () => {
    givenLoadedDraft().on('PUT', NEWS_PATH, problemResponse(409, 'SLUG_CONFLICT')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Slug')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Lỗi: Slug này đã được một tin khác sử dụng.')).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveAttribute('aria-invalid', 'true')
  })

  it('save_ShouldKeepInputAndOfferReload_WhenRevisionIsStale', async () => {
    const newer = draftNews({ revision: 2, title: 'Tiêu đề từ nơi khác' })
    const api = givenNewsEditorSession(new StubApiServer())
      .on('GET', NEWS_PATH, jsonResponse(200, draftNews(), newsHeaders(draftNews())), jsonResponse(200, newer, newsHeaders(newer)))
      .on('PUT', NEWS_PATH, problemResponse(412, 'REVISION_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')
    await user.clear(title)
    await user.type(title, 'Tiêu đề của tôi')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('đã được thay đổi ở nơi khác')
    expect(title).toHaveValue('Tiêu đề của tôi')

    await user.click(screen.getByRole('button', { name: 'Tải bản mới nhất (bỏ thay đổi chưa lưu)' }))

    await waitFor(() => expect(screen.getByLabelText('Tiêu đề')).toHaveValue('Tiêu đề từ nơi khác'))
    expect(api.requestsTo('PUT', NEWS_PATH)).toHaveLength(1)
  })

  it('render_ShouldLockSaveButton_WhenSaveIsRunning', async () => {
    givenLoadedDraft().on('PUT', NEWS_PATH, jsonResponse(200, draftNews(), newsHeaders(draftNews()))).install()
    const release = holdRequests('PUT')
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByRole('button', { name: 'Đang lưu…' })).toBeDisabled()
    release()
    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
  })

  it('render_ShouldLockSlug_WhenNewsWasPublishedBefore', async () => {
    givenLoadedDraft(draftNews({ status: 'UNPUBLISHED', revision: 4, publishedAt: '2026-11-19T03:00:00Z' })).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByLabelText('Slug')).toHaveAttribute('readonly')
  })

  it('render_ShouldShowNotFound_WhenNewsDoesNotExist', async () => {
    givenNewsEditorSession(new StubApiServer()).on('GET', NEWS_PATH, problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    renderAppRoute(EDIT_ROUTE)

    expect(await screen.findByRole('heading', { name: 'Không tìm thấy trang quản trị' })).toBeInTheDocument()
  })

  it('retry_ShouldLoadAgain_WhenFirstLoadFails', async () => {
    const news = draftNews()
    const api = givenNewsEditorSession(new StubApiServer())
      .on('GET', NEWS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, news, newsHeaders(news)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByLabelText('Tiêu đề')).toHaveValue('Spring Boot 4.1 ra mắt')
    expect(api.requestsTo('GET', NEWS_PATH)).toHaveLength(2)
  })

  it('delete_ShouldSendEtagAndOpenIndexWithNotice_WhenDraftNewsIsDeleted', async () => {
    const news = draftNews()
    const api = givenContentIndex(givenLoadedDraft(news).on('DELETE', NEWS_PATH, { status: 204 }), []).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${news.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByText(`Đã xóa “${news.title}”.`)).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent(/^\/admin\/content$/)
    expect(api.requestsTo('DELETE', NEWS_PATH)[0].headers.get('If-Match')).toBe(`"${news.revision}"`)
  })

  it('delete_ShouldTellToUnpublish_WhenNewsWasPublishedElsewhere', async () => {
    const news = draftNews()
    givenLoadedDraft(news).on('DELETE', NEWS_PATH, problemResponse(409, 'CONTENT_ALREADY_EVER_PUBLISHED')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: `Xóa ${news.title}` }))

    await user.click(screen.getByRole('button', { name: 'Xác nhận xóa' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('gỡ xuất bản')
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.queryByRole('button', { name: `Xóa ${news.title}` })).toBeNull()
  })
})
