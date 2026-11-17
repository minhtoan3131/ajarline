import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import {
  articleCollectionResponse,
  articleSummary,
  standaloneArticlesPath,
} from '../features/article-browsing/fixture/articleCollectionFixture'
import { jsonResponse, problemResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

/**
 * Waits until the Home request was sent, then one more turn of the event
 * loop so its answer is handled. Needed to check that something is absent:
 * there is no element to wait for.
 */
async function waitForHomeRequest(api: StubApiServer): Promise<void> {
  await waitFor(() => expect(api.requests).toHaveLength(1))
  await new Promise((resolve) => setTimeout(resolve, 0))
}

describe('HomePage', () => {
  it('render_ShouldShowHeadingWithoutEntryOrStatus_WhenAnswerIsStillLoading', () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .install()

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
    expect(screen.queryByRole('status')).toBeNull()
  })

  it('render_ShouldLinkToStandaloneIndex_WhenStandaloneArticlesExist', async () => {
    const api = new StubApiServer()
      .on(
        'GET',
        standaloneArticlesPath(0, 1),
        jsonResponse(200, articleCollectionResponse([articleSummary()], { size: 1, hasMore: true })),
      )
      .install()

    renderAppRoute('/')

    const entry = await screen.findByRole('link', { name: 'Bài viết độc lập' })
    expect(entry).toHaveAttribute('href', '/articles?standalone=true')
    expect(api.requests.map((request) => request.path)).toEqual([standaloneArticlesPath(0, 1)])
    const header = screen.getByRole('navigation', { name: 'Điều hướng chính' })
    expect(within(header).queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
  })

  it('render_ShouldHideEntry_WhenThereAreNoStandaloneArticles', async () => {
    const api = new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), jsonResponse(200, articleCollectionResponse([], { size: 1 })))
      .install()

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    await waitForHomeRequest(api)
    expect(screen.queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
    expect(screen.queryByRole('list', { name: 'Lối vào' })).toBeNull()
  })

  it('render_ShouldKeepHomeUsableWithoutEntry_WhenTheRequestFails', async () => {
    const api = new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), problemResponse(500, 'INTERNAL_ERROR'))
      .install()

    renderAppRoute('/')

    expect(screen.getByRole('heading', { level: 1, name: 'Ajarline' })).toBeInTheDocument()
    await waitForHomeRequest(api)
    expect(screen.queryByRole('link', { name: 'Bài viết độc lập' })).toBeNull()
    expect(screen.queryByRole('alert')).toBeNull()
    expect(screen.queryByRole('button', { name: 'Thử lại' })).toBeNull()
  })

  it('navigate_ShouldOpenStandaloneIndex_WhenEntryIsClicked', async () => {
    new StubApiServer()
      .on('GET', standaloneArticlesPath(0, 1), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .on('GET', standaloneArticlesPath(0), jsonResponse(200, articleCollectionResponse([articleSummary()])))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/')

    await user.click(await screen.findByRole('link', { name: 'Bài viết độc lập' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Bài viết độc lập' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/articles')
    expect(screen.getByTestId('search')).toHaveTextContent('?standalone=true')
  })
})
