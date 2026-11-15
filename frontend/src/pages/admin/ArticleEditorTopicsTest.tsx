import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../../app/testsupport/renderAppRoute'
import { givenSignedInAdmin } from '../../features/admin-auth/fixture/adminSessionFixture'
import {
  ARTICLE_ID,
  ARTICLE_PATH,
  ARTICLES_PATH,
  draftArticle,
  revisionHeaders,
} from '../../features/admin-content/fixture/articleAdminFixture'
import {
  activeTopic,
  inactiveTopic,
  OTHER_TOPIC_ID,
  TOPIC_ID,
  TOPICS_PATH,
  topicList,
} from '../../features/admin-topics/fixture/topicAdminFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'

const EDIT_ROUTE = `/admin/articles/${ARTICLE_ID}`

function givenArticle(article = draftArticle()): StubApiServer {
  return givenSignedInAdmin(new StubApiServer())
    .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic(), inactiveTopic())))
    .on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
}

describe('Article editor Topics', () => {
  it('render_ShouldCheckAttachedTopicsAndLockInactiveOnes_WhenArticleHasTopics', async () => {
    givenArticle(draftArticle({ topicIds: [TOPIC_ID] })).install()

    renderAppRoute(EDIT_ROUTE)

    const java = await screen.findByRole('checkbox', { name: 'Java' })
    expect(java).toBeChecked()
    expect(java).toBeEnabled()
    const spring = screen.getByRole('checkbox', { name: 'Spring' })
    expect(spring).not.toBeChecked()
    expect(spring).toBeDisabled()
    expect(screen.getByText('Đã tắt')).toBeInTheDocument()
  })

  it('render_ShouldKeepInactiveTopicRemovable_WhenArticleAlreadyHasIt', async () => {
    const user = userEvent.setup()
    givenArticle(draftArticle({ topicIds: [OTHER_TOPIC_ID] })).install()
    renderAppRoute(EDIT_ROUTE)
    const spring = await screen.findByRole('checkbox', { name: 'Spring' })
    expect(spring).toBeChecked()
    expect(spring).toBeEnabled()

    await user.click(spring)

    expect(spring).not.toBeChecked()
    expect(spring).toBeEnabled()
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('save_ShouldSendChosenTopicIdsWithIfMatch_WhenTopicIsChecked', async () => {
    const saved = draftArticle({ revision: 2, topicIds: [TOPIC_ID] })
    const api = givenArticle()
      .on('PUT', ARTICLE_PATH, jsonResponse(200, saved, revisionHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('checkbox', { name: 'Java' }))
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
    const [update] = api.requestsTo('PUT', ARTICLE_PATH)
    expect(update.body).toMatchObject({ moduleId: null, position: null, topicIds: [TOPIC_ID] })
    expect(update.headers.get('If-Match')).toBe('"1"')
    expect(screen.getByRole('checkbox', { name: 'Java' })).toBeChecked()
    expect(screen.getByText('Đã lưu mọi thay đổi')).toBeInTheDocument()
  })

  it('save_ShouldSendEmptyTopicIds_WhenAttachedTopicIsUnchecked', async () => {
    const saved = draftArticle({ revision: 2, topicIds: [] })
    const api = givenArticle(draftArticle({ topicIds: [TOPIC_ID] }))
      .on('PUT', ARTICLE_PATH, jsonResponse(200, saved, revisionHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('checkbox', { name: 'Java' }))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
    expect(api.requestsTo('PUT', ARTICLE_PATH)[0].body).toMatchObject({ topicIds: [] })
    expect(screen.getByRole('checkbox', { name: 'Java' })).not.toBeChecked()
  })

  it('save_ShouldShowErrorUnderTopicsAndKeepChoice_WhenTopicIsInactive', async () => {
    givenArticle()
      .on('PUT', ARTICLE_PATH, problemResponse(422, 'INACTIVE_TOPIC_ASSIGNMENT'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('checkbox', { name: 'Java' }))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(
      await screen.findByText('Lỗi: Topic đã tắt không gắn thêm được. Hãy bỏ chọn Topic đó rồi lưu lại.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('checkbox', { name: 'Java' })).toBeChecked()
    expect(screen.getByText('Có thay đổi chưa lưu')).toBeInTheDocument()
  })

  it('save_ShouldShowErrorUnderTopics_WhenTopicNoLongerExists', async () => {
    givenArticle()
      .on('PUT', ARTICLE_PATH, problemResponse(422, 'TOPIC_REFERENCE_NOT_FOUND'))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    await user.click(await screen.findByRole('checkbox', { name: 'Java' }))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText(/Có Topic đã chọn không còn tồn tại/)).toBeInTheDocument()
  })

  it('save_ShouldKeepAttachedTopics_WhenTopicListCannotBeLoaded', async () => {
    const article = draftArticle({ topicIds: [TOPIC_ID] })
    const saved = draftArticle({ revision: 2, title: 'Tiêu đề mới', topicIds: [TOPIC_ID] })
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, problemResponse(500, 'INTERNAL_ERROR'), jsonResponse(200, topicList(activeTopic())))
      .on('GET', ARTICLE_PATH, jsonResponse(200, article, revisionHeaders(article)))
      .on('PUT', ARTICLE_PATH, jsonResponse(200, saved, revisionHeaders(saved)))
      .install()
    const user = userEvent.setup()
    renderAppRoute(EDIT_ROUTE)
    expect(await screen.findByText(/Không tải được danh sách Topic/)).toBeInTheDocument()
    const title = screen.getByLabelText('Tiêu đề')
    await user.clear(title)
    await user.type(title, 'Tiêu đề mới')

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    expect(await screen.findByText('Đã lưu.')).toBeInTheDocument()
    expect(api.requestsTo('PUT', ARTICLE_PATH)[0].body).toMatchObject({ topicIds: [TOPIC_ID] })

    await user.click(screen.getByRole('button', { name: 'Thử lại' }))

    expect(await screen.findByRole('checkbox', { name: 'Java' })).toBeChecked()
    expect(api.requestsTo('GET', TOPICS_PATH)).toHaveLength(2)
  })

  it('save_ShouldCreateDraftWithTopics_WhenNewArticleHasChosenTopic', async () => {
    const created = draftArticle({ topicIds: [TOPIC_ID] })
    const api = givenSignedInAdmin(new StubApiServer())
      .on('GET', TOPICS_PATH, jsonResponse(200, topicList(activeTopic())))
      .on('POST', ARTICLES_PATH, jsonResponse(201, created, { ...revisionHeaders(created), Location: ARTICLE_PATH }))
      .on('GET', ARTICLE_PATH, jsonResponse(200, created, revisionHeaders(created)))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/admin/articles/new')
    await user.type(await screen.findByLabelText('Tiêu đề'), 'Hiểu Protobuf với Java')
    await user.type(screen.getByLabelText('Slug'), 'hieu-protobuf')
    await user.click(await screen.findByRole('checkbox', { name: 'Java' }))

    await user.click(screen.getByRole('button', { name: 'Lưu' }))

    // Saving moves to the new route, which mounts a fresh editor. Wait for it first,
    // or the notice is found in the old editor just before it is removed.
    await waitFor(() => expect(screen.getByTestId('location')).toHaveTextContent(`/admin/articles/${ARTICLE_ID}`))
    expect(await screen.findByText('Đã tạo bản nháp.')).toBeInTheDocument()
    expect(api.requestsTo('POST', ARTICLES_PATH)[0].body).toMatchObject({
      topicIds: [TOPIC_ID],
      moduleId: null,
      position: null,
    })
  })
})
