import { fireEvent, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  ARTICLE_ID,
  ARTICLE_PATH,
  draftArticle,
  revisionHeaders,
} from '../../features/admin-content/fixture/articleAdminFixture'
import { givenTopicCatalog } from '../../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/articles/${ARTICLE_ID}`

function givenLoadedDraft(): StubApiServer {
  const article = draftArticle()
  return givenTopicCatalog(givenSignedInAdmin(new StubApiServer()))
    .on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
    .install()
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('Article editor preview', () => {
  it('render_ShouldUpdateInlinePreview_WhenBodyChangesWithoutSaving', async () => {
    // Given
    const api = givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const body = await screen.findByLabelText('Nội dung Markdown')

    // When
    await user.clear(body)
    await user.paste('## Phần mới')

    // Then
    const preview = screen.getByRole('region', { name: 'Xem trước trực tiếp' })
    expect(await within(preview).findByRole('heading', { level: 2, name: 'Phần mới' })).toBeInTheDocument()
    expect(api.requests.filter((request) => request.method !== 'GET')).toHaveLength(0)
  })

  it('switchPane_ShouldMarkSelectedMode_WhenPreviewModeIsChosen', async () => {
    // Given
    givenLoadedDraft()
    const user = userEvent.setup()
    const { container } = renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')

    // When
    await user.click(screen.getByRole('button', { name: 'Xem trước' }))

    // Then
    expect(screen.getByRole('button', { name: 'Xem trước' })).toHaveAttribute('aria-pressed', 'true')
    expect(container.querySelector('.article-editor-workspace')).toHaveAttribute('data-pane', 'preview')
  })

  it('openPreview_ShouldShowUnsavedValuesWithoutRequests_WhenOverlayOpens', async () => {
    // Given
    const api = givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const title = await screen.findByLabelText('Tiêu đề')
    await user.clear(title)
    await user.type(title, 'Tiêu đề chưa lưu')
    const requestsBefore = api.requests.length

    // When
    await user.click(screen.getByRole('button', { name: 'Xem trước toàn màn hình' }))

    // Then
    const dialog = screen.getByRole('dialog', { name: 'Xem trước bài viết' })
    expect(within(dialog).getByRole('heading', { name: 'Tiêu đề chưa lưu' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Đóng xem trước' })).toHaveFocus()
    expect(document.body).toHaveClass('scroll-locked')
    expect(api.requests).toHaveLength(requestsBefore)
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('closePreview_ShouldRestoreFocusAndScroll_WhenEscapeIsPressed', async () => {
    // Given
    givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')
    const trigger = screen.getByRole('button', { name: 'Xem trước toàn màn hình' })
    await user.click(trigger)

    // When
    await user.keyboard('{Escape}')

    // Then
    expect(screen.queryByRole('dialog')).toBeNull()
    expect(trigger).toHaveFocus()
    expect(document.body).not.toHaveClass('scroll-locked')
  })

  it('closePreview_ShouldClose_WhenCloseButtonIsClicked', async () => {
    // Given
    givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')
    await user.click(screen.getByRole('button', { name: 'Xem trước toàn màn hình' }))

    // When
    await user.click(screen.getByRole('button', { name: 'Đóng xem trước' }))

    // Then
    expect(screen.queryByRole('dialog')).toBeNull()
  })

  it('tab_ShouldKeepFocusInsideOverlay_WhenOverlayIsOpen', async () => {
    // Given
    givenLoadedDraft()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await screen.findByLabelText('Tiêu đề')
    await user.click(screen.getByRole('button', { name: 'Xem trước toàn màn hình' }))
    const dialog = screen.getByRole('dialog', { name: 'Xem trước bài viết' })

    // When
    await user.tab()
    await user.tab()
    await user.tab({ shift: true })

    // Then
    expect(dialog).toContainElement(document.activeElement as HTMLElement)
  })

  it('clickPreviewLink_ShouldKeepEditorAndUnsavedText_WhenLinkPointsToSitePage', async () => {
    const api = givenLoadedDraft()
    const open = vi.spyOn(window, 'open').mockReturnValue(null)
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    const body = await screen.findByLabelText('Nội dung Markdown')
    await user.clear(body)
    await user.paste('[Bài khác](/articles/bai-khac)')
    const preview = screen.getByRole('region', { name: 'Xem trước trực tiếp' })

    const followed = fireEvent.click(await within(preview).findByRole('link', { name: 'Bài khác' }))

    expect(followed).toBe(false)
    expect(open).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('location')).toHaveTextContent(EDIT_ROUTE)
    expect(screen.getByLabelText('Nội dung Markdown')).toHaveValue('[Bài khác](/articles/bai-khac)')
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
    expect(api.requests.filter((request) => request.method !== 'GET')).toHaveLength(0)
  })
})
