import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

import { HttpClient } from '../../shared/api/httpClient'
import { HttpClientProvider } from '../../shared/api/HttpClientContext'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'
import { publicSeries, seriesApiPath, seriesModuleItem } from './fixture/publicLearningFixture'
import { usePublicSeries } from './usePublicSeries'

// One client for every render. A new client on each render would restart the request.
const CLIENT = new HttpClient()

function wrapper({ children }: { children: ReactNode }) {
  return <HttpClientProvider client={CLIENT}>{children}</HttpClientProvider>
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('usePublicSeries', () => {
  it('usePublicSeries_ShouldReturnFound_WhenSeriesHasModulesWithArticles', async () => {
    new StubApiServer().on('GET', seriesApiPath('java-concurrency'), jsonResponse(200, publicSeries())).install()

    const { result } = renderHook(() => usePublicSeries('java-concurrency'), { wrapper })

    expect(result.current.state).toEqual({ status: 'loading' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
  })

  it('usePublicSeries_ShouldReturnNotFound_WhenServerAnswers404', async () => {
    new StubApiServer().on('GET', seriesApiPath('x'), problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    const { result } = renderHook(() => usePublicSeries('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'not-found' }))
  })

  it('usePublicSeries_ShouldReturnFailed_WhenServerAnswers500', async () => {
    new StubApiServer().on('GET', seriesApiPath('x'), problemResponse(500, 'INTERNAL_ERROR')).install()

    const { result } = renderHook(() => usePublicSeries('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'failed' }))
  })

  it.each([
    ['no Module', publicSeries({ modules: [] })],
    ['a Module without Article', publicSeries({ modules: [seriesModuleItem({ articles: [] })] })],
  ])('usePublicSeries_ShouldReturnFailed_WhenSeriesHas (%s)', async (_label, body) => {
    new StubApiServer().on('GET', seriesApiPath('x'), jsonResponse(200, body)).install()

    const { result } = renderHook(() => usePublicSeries('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'failed' }))
  })

  it('usePublicSeries_ShouldGoBackToLoading_WhenSlugChanges', async () => {
    vi.stubGlobal('fetch', (input: RequestInfo | URL) =>
      String(input) === seriesApiPath('series-a')
        ? Promise.resolve(ok(publicSeries({ slug: 'series-a' })))
        : new Promise<Response>(() => {}),
    )
    const { result, rerender } = renderHook(({ slug }) => usePublicSeries(slug), {
      wrapper,
      initialProps: { slug: 'series-a' },
    })
    await waitFor(() => expect(result.current.state.status).toBe('found'))

    rerender({ slug: 'series-b' })

    expect(result.current.state).toEqual({ status: 'loading' })
  })

  it('usePublicSeries_ShouldIgnoreOlderAnswer_WhenSlugChangesBeforeItArrives', async () => {
    let finishFirst: (response: Response) => void = () => {}
    const slowFirst = new Promise<Response>((resolve) => {
      finishFirst = resolve
    })
    const signals = new Map<string, AbortSignal | null | undefined>()
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      signals.set(path, init?.signal)
      if (path === seriesApiPath('series-a')) {
        return slowFirst
      }
      if (path === seriesApiPath('series-b')) {
        return Promise.resolve(ok(publicSeries({ slug: 'series-b', title: 'B' })))
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const { result, rerender } = renderHook(({ slug }) => usePublicSeries(slug), {
      wrapper,
      initialProps: { slug: 'series-a' },
    })

    rerender({ slug: 'series-b' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
    await act(async () => {
      finishFirst(ok(publicSeries({ slug: 'series-a', title: 'A' })))
    })

    expect(signals.get(seriesApiPath('series-a'))?.aborted).toBe(true)
    expect(result.current.state).toMatchObject({ status: 'found', series: { slug: 'series-b' } })
  })
})
