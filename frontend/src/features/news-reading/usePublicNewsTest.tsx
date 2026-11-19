import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

import { HttpClient } from '../../shared/api/httpClient'
import { HttpClientProvider } from '../../shared/api/HttpClientContext'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'
import { newsApiPath, publicNews } from './fixture/publicNewsFixture'
import { usePublicNews } from './usePublicNews'

// One client for every render. A new client on each render would restart the request.
const CLIENT = new HttpClient()

function wrapper({ children }: { children: ReactNode }) {
  return <HttpClientProvider client={CLIENT}>{children}</HttpClientProvider>
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('usePublicNews', () => {
  it('usePublicNews_ShouldReturnFound_WhenNewsIsPublic', async () => {
    new StubApiServer().on('GET', newsApiPath('spring-boot-4-1'), jsonResponse(200, publicNews())).install()

    const { result } = renderHook(() => usePublicNews('spring-boot-4-1'), { wrapper })

    expect(result.current.state).toEqual({ status: 'loading' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
  })

  it('usePublicNews_ShouldReturnNotFound_WhenServerAnswers404', async () => {
    new StubApiServer().on('GET', newsApiPath('x'), problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    const { result } = renderHook(() => usePublicNews('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'not-found' }))
  })

  it('usePublicNews_ShouldReturnFailed_WhenServerAnswers500', async () => {
    new StubApiServer().on('GET', newsApiPath('x'), problemResponse(500, 'INTERNAL_ERROR')).install()

    const { result } = renderHook(() => usePublicNews('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'failed' }))
  })

  it('usePublicNews_ShouldIgnoreOlderAnswer_WhenSlugChangesBeforeItArrives', async () => {
    let finishFirst: (response: Response) => void = () => {}
    const slowFirst = new Promise<Response>((resolve) => {
      finishFirst = resolve
    })
    const signals = new Map<string, AbortSignal | null | undefined>()
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => {
      const path = String(input)
      signals.set(path, init?.signal)
      if (path === newsApiPath('news-a')) {
        return slowFirst
      }
      if (path === newsApiPath('news-b')) {
        return Promise.resolve(ok(publicNews({ slug: 'news-b', title: 'B' })))
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const { result, rerender } = renderHook(({ slug }) => usePublicNews(slug), {
      wrapper,
      initialProps: { slug: 'news-a' },
    })

    rerender({ slug: 'news-b' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
    await act(async () => {
      finishFirst(ok(publicNews({ slug: 'news-a', title: 'A' })))
    })

    expect(signals.get(newsApiPath('news-a'))?.aborted).toBe(true)
    expect(result.current.state).toMatchObject({ status: 'found', news: { slug: 'news-b' } })
  })
})
