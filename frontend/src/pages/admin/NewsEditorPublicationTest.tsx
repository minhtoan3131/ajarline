import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  draftNews,
  givenNewsEditorSession,
  holdRequests,
  newsHeaders,
  newsPublishPath,
  NEWS_ID,
  NEWS_PATH,
  publicationOfNews,
} from '../../features/admin-news/fixture/newsAdminFixture'
import type { NewsAdminDetail } from '../../features/admin-news/model/news'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/news/${NEWS_ID}`
const PUBLISHED_AT = '2026-11-19T03:00:00Z'

function givenLoaded(news: NewsAdminDetail): StubApiServer {
  return givenNewsEditorSession(new StubApiServer()).on('GET', NEWS_PATH, jsonResponse(200, news, newsHeaders(news)))
}

describe('News editor publication', () => {
  it('publish_ShouldSendSavedEtagWithoutSaving_WhenDraftIsClean', async () => {
    const draft = draftNews({ revision: 2 })
    const api = givenLoaded(draft)
      .on(
        'POST',
        newsPublishPath('publish'),
        jsonResponse(200, publicationOfNews(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT }), {
          ETag: '"3"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    expect(await screen.findByText('Trạng thái: Đã xuất bản')).toBeInTheDocument()
    const [publish] = api.requestsTo('POST', newsPublishPath('publish'))
    expect(publish.headers.get('If-Match')).toBe('"2"')
    expect(publish.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(publish.body).toBeUndefined()
    expect(api.requestsTo('PUT', NEWS_PATH)).toHaveLength(0)
    expect(screen.getByLabelText('Slug')).toHaveAttribute('readonly')
    expect(screen.queryByRole('button', { name: 'Xuất bản' })).toBeNull()
    expect(screen.getByRole('button', { name: 'Gỡ xuất bản' })).toBeInTheDocument()
  })

  it('unpublish_ShouldUseEtagFromPublish_WhenNewsWasJustPublished', async () => {
    const draft = draftNews({ revision: 2 })
    const published = publicationOfNews(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT })
    const api = givenLoaded(draft)
      .on('POST', newsPublishPath('publish'), jsonResponse(200, published, { ETag: '"3"' }))
      .on(
        'POST',
        newsPublishPath('unpublish'),
        jsonResponse(200, { ...published, status: 'UNPUBLISHED', revision: 4, unpublishedAt: '2026-11-19T04:00:00Z' }, {
          ETag: '"4"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    await user.click(await screen.findByRole('button', { name: 'Gỡ xuất bản' }))

    expect(await screen.findByText('Trạng thái: Đã gỡ xuất bản')).toBeInTheDocument()
    expect(api.requestsTo('POST', newsPublishPath('unpublish'))[0].headers.get('If-Match')).toBe('"3"')
    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeInTheDocument()
  })

  it('save_ShouldUseEtagFromPublish_WhenNewsIsSavedAfterPublish', async () => {
    const draft = draftNews({ revision: 2 })
    const saved = draftNews({ revision: 4, status: 'PUBLISHED', publishedAt: PUBLISHED_AT, title: 'Tiêu đề mới' })
    const api = givenLoaded(draft)
      .on(
        'POST',
        newsPublishPath('publish'),
        jsonResponse(200, publicationOfNews(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT }), {
          ETag: '"3"',
        }),
      )
      .on('PUT', NEWS_PATH, jsonResponse(200, saved, newsHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))
    await screen.findByText('Trạng thái: Đã xuất bản')
    const title = screen.getByLabelText('Tiêu đề')

    await user.clear(title)
    await user.type(title, 'Tiêu đề mới')
    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await waitFor(() => expect(api.requestsTo('PUT', NEWS_PATH)).toHaveLength(1))
    expect(api.requestsTo('PUT', NEWS_PATH)[0].headers.get('If-Match')).toBe('"3"')
  })

  it('render_ShouldLockPublishAndExplainWhy_WhenFormHasUnsavedChanges', async () => {
    const saved = draftNews({ revision: 2, title: 'Tiêu đề đã lưu' })
    givenLoaded(draftNews()).on('PUT', NEWS_PATH, jsonResponse(200, saved, newsHeaders(saved))).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')

    await user.type(title, ' thêm')

    const publish = screen.getByRole('button', { name: 'Xuất bản' })
    expect(publish).toBeDisabled()
    expect(screen.getByText('Lưu thay đổi trước khi xuất bản. Xuất bản chỉ áp dụng cho bản đã lưu.')).toBeInTheDocument()
    expect(publish).toHaveAccessibleDescription('Lưu thay đổi trước khi xuất bản. Xuất bản chỉ áp dụng cho bản đã lưu.')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    await screen.findByText('Đã lưu.')
    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeEnabled()
  })

  it('render_ShouldShowNoPublishButton_WhenNewsIsNotSavedYet', async () => {
    givenNewsEditorSession(new StubApiServer()).install()

    renderAppRoute('/admin/news/new')

    await screen.findByLabelText('Tiêu đề')
    expect(screen.queryByRole('button', { name: 'Xuất bản' })).toBeNull()
    expect(screen.getByText('Trạng thái: Chưa lưu lần nào')).toBeInTheDocument()
  })

  it('render_ShouldLockWriteButtons_WhenPublishIsRunning', async () => {
    const draft = draftNews({ revision: 2 })
    givenLoaded(draft)
      .on(
        'POST',
        newsPublishPath('publish'),
        jsonResponse(200, publicationOfNews(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT }), {
          ETag: '"3"',
        }),
      )
      .install()
    const release = holdRequests('POST')
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    expect(await screen.findByRole('button', { name: 'Đang xuất bản…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Lưu' })).toBeDisabled()
    release()
    expect(await screen.findByText('Trạng thái: Đã xuất bản')).toBeInTheDocument()
  })

  it('publish_ShouldShowErrorInSourceBlockAndStayDraft_WhenSourceIsRequired', async () => {
    givenLoaded(draftNews({ source: { name: null, url: null, publishedAt: null } }))
      .on('POST', newsPublishPath('publish'), problemResponse(422, 'NEWS_SOURCE_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    const block = await screen.findByRole('group', { name: 'Nguồn tin' })
    expect(within(block).getByText('Lỗi: Chưa đủ nguồn để xuất bản.')).toBeInTheDocument()
    expect(block).toHaveClass('news-source-invalid')
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeEnabled()
  })

  it.each([
    ['CONTENT_SUMMARY_REQUIRED', 'Tóm tắt', 'Lỗi: Cần có tóm tắt trước khi xuất bản.'],
    ['NEWS_BODY_REQUIRED', 'Nội dung Markdown', 'Lỗi: Cần có nội dung tin trước khi xuất bản.'],
  ])('publish_ShouldShowErrorUnderField_When%s', async (code, label, message) => {
    givenLoaded(draftNews()).on('POST', newsPublishPath('publish'), problemResponse(422, code)).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    expect(await screen.findByText(message)).toBeInTheDocument()
    expect(screen.getByLabelText(label)).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('publish_ShouldOfferReload_WhenRevisionIsStale', async () => {
    givenLoaded(draftNews()).on('POST', newsPublishPath('publish'), problemResponse(412, 'REVISION_CONFLICT')).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('đã được thay đổi ở nơi khác')
    expect(screen.getByRole('button', { name: 'Tải bản mới nhất (bỏ thay đổi chưa lưu)' })).toBeInTheDocument()
  })

  it('publish_ShouldShowPublishedState_WhenNewsWasAlreadyPublishedInAnotherTab', async () => {
    const draft = draftNews()
    givenLoaded(draft)
      .on(
        'POST',
        newsPublishPath('publish'),
        jsonResponse(200, publicationOfNews(draft, { status: 'PUBLISHED', revision: 5, publishedAt: PUBLISHED_AT }), {
          ETag: '"5"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    expect(await screen.findByText('Trạng thái: Đã xuất bản')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Gỡ xuất bản' })).toBeInTheDocument()
  })
})
