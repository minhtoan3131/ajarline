import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { CSRF_TOKEN, givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  ARTICLE_ID,
  ARTICLE_PATH,
  draftArticle,
  publicationOf,
  publishPath,
  revisionHeaders,
} from '../../features/admin-content/fixture/articleAdminFixture'
import type { ArticleAdminDetail } from '../../features/admin-content/model/article'
import { givenTopicCatalog } from '../../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/articles/${ARTICLE_ID}`
const PUBLISHED_AT = '2026-11-10T03:00:00Z'

function givenLoaded(article: ArticleAdminDetail): StubApiServer {
  return givenTopicCatalog(givenSignedInAdmin(new StubApiServer())).on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
}

describe('Article editor publication', () => {
  it('publish_ShouldSendSavedEtagWithoutSaving_WhenDraftIsClean', async () => {
    // Given
    const draft = draftArticle({ revision: 2 })
    const api = givenLoaded(draft)
      .on(
        'POST',
        publishPath('publish'),
        jsonResponse(200, publicationOf(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT }), {
          ETag: '"3"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    // When
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    // Then
    expect(await screen.findByText('Trạng thái: Đã xuất bản')).toBeInTheDocument()
    const [publish] = api.requestsTo('POST', publishPath('publish'))
    expect(publish.headers.get('If-Match')).toBe('"2"')
    expect(publish.headers.get('X-CSRF-TOKEN')).toBe(CSRF_TOKEN.token)
    expect(publish.body).toBeUndefined()
    expect(api.requestsTo('PUT', ARTICLE_PATH)).toHaveLength(0)
    expect(screen.getByLabelText('Slug')).toHaveAttribute('readonly')
    expect(screen.getByRole('link', { name: 'Mở trang công khai' })).toHaveAttribute('href', '/articles/hieu-protobuf')
    expect(screen.queryByRole('button', { name: 'Xuất bản' })).toBeNull()
  })

  it('unpublish_ShouldUseEtagFromPublish_WhenArticleWasJustPublished', async () => {
    // Given
    const draft = draftArticle({ revision: 2 })
    const published = publicationOf(draft, { status: 'PUBLISHED', revision: 3, publishedAt: PUBLISHED_AT })
    const api = givenLoaded(draft)
      .on('POST', publishPath('publish'), jsonResponse(200, published, { ETag: '"3"' }))
      .on(
        'POST',
        publishPath('unpublish'),
        jsonResponse(200, { ...published, status: 'UNPUBLISHED', revision: 4, unpublishedAt: '2026-11-10T04:00:00Z' }, {
          ETag: '"4"',
        }),
      )
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    // When
    await user.click(await screen.findByRole('button', { name: 'Gỡ xuất bản' }))

    // Then
    expect(await screen.findByText('Trạng thái: Đã gỡ xuất bản')).toBeInTheDocument()
    expect(api.requestsTo('POST', publishPath('unpublish'))[0].headers.get('If-Match')).toBe('"3"')
    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeInTheDocument()
    expect(screen.getByLabelText('Slug')).toHaveAttribute('readonly')
  })

  it('publish_ShouldBeDisabled_WhenFormHasUnsavedChanges', async () => {
    // Given
    givenLoaded(draftArticle()).install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    // When
    await user.type(await screen.findByLabelText('Tiêu đề'), ' sửa')

    // Then
    expect(screen.getByRole('button', { name: 'Xuất bản' })).toBeDisabled()
    expect(screen.getByText('Lưu thay đổi trước khi xuất bản. Xuất bản chỉ áp dụng cho bản đã lưu.')).toBeInTheDocument()
  })

  it('render_ShouldHideUnpublish_WhenArticleIsDraft', async () => {
    // Given
    givenLoaded(draftArticle()).install()

    // When
    renderAppRoute(EDIT_ROUTE)

    // Then
    expect(await screen.findByRole('button', { name: 'Xuất bản' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Gỡ xuất bản' })).toBeNull()
    expect(screen.queryByRole('link', { name: 'Mở trang công khai' })).toBeNull()
  })

  it('render_ShouldHidePublicationActions_WhenArticleIsNew', async () => {
    // Given
    givenTopicCatalog(givenSignedInAdmin(new StubApiServer())).install()

    // When
    renderAppRoute('/admin/articles/new')

    // Then
    expect(await screen.findByLabelText('Tiêu đề')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Xuất bản' })).toBeNull()
  })

  it('publish_ShouldShowErrorUnderSummary_WhenSummaryIsMissing', async () => {
    // Given
    givenLoaded(draftArticle({ summary: null }))
      .on('POST', publishPath('publish'), problemResponse(422, 'CONTENT_SUMMARY_REQUIRED'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    // When
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    // Then
    expect(await screen.findByText('Lỗi: Cần có tóm tắt trước khi xuất bản.')).toBeInTheDocument()
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })

  it('publish_ShouldShowConflictWithoutRetry_WhenRevisionIsStale', async () => {
    // Given
    const api = givenLoaded(draftArticle())
      .on('POST', publishPath('publish'), problemResponse(412, 'REVISION_CONFLICT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)

    // When
    await user.click(await screen.findByRole('button', { name: 'Xuất bản' }))

    // Then
    expect(await screen.findByRole('alert')).toHaveTextContent('đã được thay đổi ở nơi khác')
    await waitFor(() => expect(api.requestsTo('POST', publishPath('publish'))).toHaveLength(1))
    expect(screen.getByText('Trạng thái: Bản nháp')).toBeInTheDocument()
  })
})
