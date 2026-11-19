import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { draftNews, givenNewsEditorSession, newsHeaders, NEWS_ID, NEWS_PATH } from '../../features/admin-news/fixture/newsAdminFixture'
import { jsonResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/news/${NEWS_ID}`

function givenLoadedDraft(): StubApiServer {
  const news = draftNews()
  return givenNewsEditorSession(new StubApiServer())
    .on('GET', NEWS_PATH, jsonResponse(200, news, newsHeaders(news)))
    .install()
}

describe('News editor preview', () => {
  it('render_ShouldUpdateInlinePreviewWithoutRequests_WhenBodyChangesWithoutSaving', async () => {
    const api = givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const body = await screen.findByLabelText('Nội dung Markdown')

    await user.clear(body)
    await user.paste('## Phần mới')

    const preview = screen.getByRole('region', { name: 'Xem trước trực tiếp' })
    expect(await within(preview).findByRole('heading', { level: 2, name: 'Phần mới' })).toBeInTheDocument()
    expect(api.requests.filter((request) => request.method !== 'GET')).toHaveLength(0)
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('openPreview_ShouldShowUnsavedValuesWithoutRequests_WhenOverlayOpens', async () => {
    const api = givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')
    await user.clear(title)
    await user.type(title, 'Tiêu đề chưa lưu')
    const requestsBefore = api.requests.length

    await user.click(screen.getByRole('button', { name: 'Xem trước toàn màn hình' }))

    const dialog = screen.getByRole('dialog', { name: 'Xem trước tin' })
    expect(within(dialog).getByRole('heading', { name: 'Tiêu đề chưa lưu' })).toBeInTheDocument()
    expect(within(dialog).getByRole('region', { name: 'Nguồn tin trong xem trước' })).toHaveTextContent('Spring Blog')
    expect(api.requests).toHaveLength(requestsBefore)
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('closePreview_ShouldClose_WhenEscapeIsPressed', async () => {
    givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')
    const trigger = screen.getByRole('button', { name: 'Xem trước toàn màn hình' })
    await user.click(trigger)

    await user.keyboard('{Escape}')

    expect(screen.queryByRole('dialog')).toBeNull()
    expect(trigger).toHaveFocus()
  })

  it('render_ShouldWarnInPreview_WhenSourceUrlIsUnsafe', async () => {
    givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const url = await screen.findByLabelText('URL nguồn')

    await user.clear(url)
    await user.type(url, 'javascript:alert(1)')

    const preview = screen.getByRole('region', { name: 'Xem trước trực tiếp' })
    expect(await within(preview).findByText('Người đọc sẽ không thấy link này.')).toBeInTheDocument()
    expect(within(preview).queryByRole('link')).toBeNull()
  })

  it('render_ShouldNotWarnInPreview_WhenSourceUrlIsSafe', async () => {
    givenLoadedDraft()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('URL nguồn')

    const preview = screen.getByRole('region', { name: 'Xem trước trực tiếp' })
    expect(await within(preview).findByText('https://spring.io/blog/spring-boot-4-1')).toBeInTheDocument()
    expect(within(preview).queryByText('Người đọc sẽ không thấy link này.')).toBeNull()
    expect(within(preview).queryByRole('link')).toBeNull()
  })
})
