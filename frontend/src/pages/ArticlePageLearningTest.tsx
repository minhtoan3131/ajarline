import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'

import { renderAppRoute } from '../app/testsupport/renderAppRoute'
import { publicModule } from '../features/learning-navigation/fixture/publicLearningFixture'
import { jsonResponse, StubApiServer } from '../shared/api/testsupport/StubApiServer'

const ARTICLE_API = '/api/v1/articles/dong-bo-hoa'
const NEXT_API = '/api/v1/articles/khoa-va-lock'
const TRAIL = { name: 'Vị trí trong lộ trình học' }
const SEQUENCE = { name: 'Bài trước và bài sau' }

const SERIES = { id: '1f0b6c2e-3a47-4d58-9e61-7b8c9d0e1f23', slug: 'java-concurrency', title: 'Java Concurrency' }
const MODULE = { id: '5a6b7c8d-9e0f-4a1b-8c2d-3e4f5a6b7c8d', slug: 'thread-co-ban', title: 'Thread cơ bản', position: 3 }
const PREVIOUS = { id: '7e5d3a91-2c48-4b06-8f1d-9a0b4c6e2d35', slug: 'tao-thread', title: 'Tạo Thread', position: 1 }
const NEXT = { id: '9a8b7c6d-5e4f-4a3b-8c2d-1e0f9a8b7c6d', slug: 'khoa-va-lock', title: 'Khoá và Lock', position: 3 }

/** An Article in the middle of a Module: rank 2, with a previous and a next Article. */
function learningArticle(changes: Record<string, unknown> = {}) {
  return {
    id: '3b9c1d7e-5a24-4e68-b0f3-8c1d2e4a6b79',
    type: 'article',
    slug: 'dong-bo-hoa',
    title: 'Đồng bộ hoá',
    summary: 'Dùng synchronized và lock.',
    bodyMarkdown: 'Nội dung bài.',
    topics: [],
    series: SERIES,
    module: MODULE,
    previousArticle: PREVIOUS,
    nextArticle: NEXT,
    publishedAt: '2026-11-01T02:00:00Z',
    updatedAt: '2026-11-01T02:00:00Z',
    ...changes,
  }
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('ArticlePage learning context', () => {
  it('render_ShouldShowSeriesModuleAndRank_WhenArticleIsInPublicModuleAndSeries', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle())).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const trail = await screen.findByRole('navigation', TRAIL)
    expect(within(trail).getByRole('link', { name: 'Java Concurrency' })).toHaveAttribute(
      'href',
      '/series/java-concurrency',
    )
    expect(within(trail).getByRole('link', { name: 'Thread cơ bản' })).toHaveAttribute('href', '/modules/thread-co-ban')
    expect(trail).toHaveTextContent('Module 3 · Bài 2')
  })

  it('render_ShouldShowContextBeforeTheArticleAndSequenceAfterIt_WhenArticleIsInModule', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle())).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const trail = await screen.findByRole('navigation', TRAIL)
    const article = screen.getByRole('article')
    const sequence = screen.getByRole('navigation', SEQUENCE)
    expect(trail.compareDocumentPosition(article) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(article.compareDocumentPosition(sequence) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })

  it('render_ShouldLinkPreviousAndNextArticle_WhenBothExist', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle())).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const sequence = await screen.findByRole('navigation', SEQUENCE)
    expect(within(sequence).getByRole('link', { name: /Bài trước.*Tạo Thread/ })).toHaveAttribute(
      'href',
      '/articles/tao-thread',
    )
    expect(within(sequence).getByRole('link', { name: /Bài sau.*Khoá và Lock/ })).toHaveAttribute(
      'href',
      '/articles/khoa-va-lock',
    )
  })

  it('render_ShouldShowRankOneAndOnlyNext_WhenArticleIsFirstInModule', async () => {
    new StubApiServer()
      .on('GET', ARTICLE_API, jsonResponse(200, learningArticle({ previousArticle: null })))
      .install()

    renderAppRoute('/articles/dong-bo-hoa')

    expect(await screen.findByRole('navigation', TRAIL)).toHaveTextContent('Module 3 · Bài 1')
    const sequence = screen.getByRole('navigation', SEQUENCE)
    expect(within(sequence).queryByRole('link', { name: /Bài trước/ })).toBeNull()
    expect(within(sequence).getByRole('link', { name: /Bài sau/ })).toBeInTheDocument()
  })

  it('render_ShouldShowOnlyPrevious_WhenArticleIsLastInModule', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle({ nextArticle: null }))).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const sequence = await screen.findByRole('navigation', SEQUENCE)
    expect(within(sequence).getByRole('link', { name: /Bài trước/ })).toBeInTheDocument()
    expect(within(sequence).queryByRole('link', { name: /Bài sau/ })).toBeNull()
  })

  it('render_ShouldDropSeriesAndModulePosition_WhenSeriesIsHidden', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle({ series: null }))).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const trail = await screen.findByRole('navigation', TRAIL)
    expect(within(trail).getByRole('link', { name: 'Thread cơ bản' })).toBeInTheDocument()
    expect(within(trail).queryByRole('link', { name: 'Java Concurrency' })).toBeNull()
    expect(trail).toHaveTextContent('Bài 2')
    expect(trail).not.toHaveTextContent('Module')
  })

  it('render_ShouldShowNoContextAndNoSequence_WhenArticleIsStandalone', async () => {
    new StubApiServer()
      .on(
        'GET',
        ARTICLE_API,
        jsonResponse(200, learningArticle({ series: null, module: null, previousArticle: null, nextArticle: null })),
      )
      .install()

    renderAppRoute('/articles/dong-bo-hoa')

    await screen.findByRole('heading', { level: 1, name: 'Đồng bộ hoá' })
    expect(screen.queryByRole('navigation', TRAIL)).toBeNull()
    expect(screen.queryByRole('navigation', SEQUENCE)).toBeNull()
  })

  it('render_ShouldNotLinkSeries_WhenServerSendsSeriesWithoutModule', async () => {
    new StubApiServer()
      .on(
        'GET',
        ARTICLE_API,
        jsonResponse(200, learningArticle({ module: null, previousArticle: null, nextArticle: null })),
      )
      .install()

    renderAppRoute('/articles/dong-bo-hoa')

    await screen.findByRole('heading', { level: 1, name: 'Đồng bộ hoá' })
    expect(screen.queryByRole('navigation', TRAIL)).toBeNull()
    expect(screen.queryByRole('link', { name: 'Java Concurrency' })).toBeNull()
  })

  it('render_ShouldNotShowProgress_WhenArticleHasContext', async () => {
    new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle())).install()

    renderAppRoute('/articles/dong-bo-hoa')

    const trail = await screen.findByRole('navigation', TRAIL)
    const sequence = screen.getByRole('navigation', SEQUENCE)
    expect(trail.textContent).not.toMatch(/\d+\s*\/\s*\d+/)
    expect(sequence.textContent).not.toMatch(/\d+\s*\/\s*\d+/)
  })

  it('render_ShouldRequestOnlyTheArticle_WhenContextIsShown', async () => {
    const api = new StubApiServer().on('GET', ARTICLE_API, jsonResponse(200, learningArticle())).install()

    renderAppRoute('/articles/dong-bo-hoa')
    await screen.findByRole('navigation', TRAIL)

    expect(api.requests.map((request) => request.path)).toEqual([ARTICLE_API])
  })

  it('navigate_ShouldShowLoadingAndReleaseOlderRequest_WhenNextArticleIsClicked', async () => {
    let finishSecond: (response: Response) => void = () => {}
    const slowSecond = new Promise<Response>((resolve) => {
      finishSecond = resolve
    })
    const signals = new Map<string, AbortSignal | null | undefined>()
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      signals.set(path, init?.signal)
      if (path === ARTICLE_API) {
        return Promise.resolve(ok(learningArticle()))
      }
      if (path === NEXT_API) {
        return slowSecond
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const user = userEvent.setup()
    renderAppRoute('/articles/dong-bo-hoa')

    await user.click(await screen.findByRole('link', { name: /Bài sau/ }))

    expect(await screen.findByRole('status')).toHaveTextContent('Đang tải bài viết…')
    expect(screen.queryByRole('heading', { level: 1, name: 'Đồng bộ hoá' })).toBeNull()
    expect(signals.get(ARTICLE_API)?.aborted).toBe(true)
    await act(async () => {
      finishSecond(
        ok(
          learningArticle({
            slug: 'khoa-va-lock',
            title: 'Khoá và Lock',
            previousArticle: { ...PREVIOUS, slug: 'dong-bo-hoa', title: 'Đồng bộ hoá', position: 2 },
            nextArticle: null,
          }),
        ),
      )
    })

    expect(await screen.findByRole('heading', { level: 1, name: 'Khoá và Lock' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/articles/khoa-va-lock')
    expect(screen.getByRole('navigation', TRAIL)).toHaveTextContent('Bài 3')
  })

  it('navigate_ShouldOpenModule_WhenModuleLinkIsClicked', async () => {
    new StubApiServer()
      .on('GET', ARTICLE_API, jsonResponse(200, learningArticle()))
      .on('GET', '/api/v1/modules/thread-co-ban', jsonResponse(200, publicModule()))
      .install()
    const user = userEvent.setup()
    renderAppRoute('/articles/dong-bo-hoa')

    await user.click(await screen.findByRole('link', { name: 'Thread cơ bản' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Thread cơ bản' })).toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/modules/thread-co-ban')
  })
})
