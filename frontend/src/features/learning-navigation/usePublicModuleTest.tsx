import { act, renderHook, waitFor } from '@testing-library/react'
import type { ReactNode } from 'react'
import { describe, expect, it, vi } from 'vitest'

import { HttpClient } from '../../shared/api/httpClient'
import { HttpClientProvider } from '../../shared/api/HttpClientContext'
import { jsonResponse, problemResponse, StubApiServer } from '../../shared/api/testsupport/StubApiServer'
import { moduleApiPath, publicModule } from './fixture/publicLearningFixture'
import { usePublicModule } from './usePublicModule'

// One client for every render. A new client on each render would restart the request.
const CLIENT = new HttpClient()

function wrapper({ children }: { children: ReactNode }) {
  return <HttpClientProvider client={CLIENT}>{children}</HttpClientProvider>
}

function ok(body: unknown): Response {
  return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
}

describe('usePublicModule', () => {
  it('usePublicModule_ShouldReturnFound_WhenModuleHasArticles', async () => {
    new StubApiServer().on('GET', moduleApiPath('thread-co-ban'), jsonResponse(200, publicModule())).install()

    const { result } = renderHook(() => usePublicModule('thread-co-ban'), { wrapper })

    expect(result.current.state).toEqual({ status: 'loading' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
  })

  it('usePublicModule_ShouldReturnNotFound_WhenServerAnswers404', async () => {
    new StubApiServer().on('GET', moduleApiPath('x'), problemResponse(404, 'CONTENT_NOT_FOUND')).install()

    const { result } = renderHook(() => usePublicModule('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'not-found' }))
  })

  it('usePublicModule_ShouldReturnFailed_WhenModuleHasNoArticle', async () => {
    new StubApiServer().on('GET', moduleApiPath('x'), jsonResponse(200, publicModule({ articles: [] }))).install()

    const { result } = renderHook(() => usePublicModule('x'), { wrapper })

    await waitFor(() => expect(result.current.state).toEqual({ status: 'failed' }))
  })

  it('usePublicModule_ShouldIgnoreOlderAnswer_WhenSlugChangesBeforeItArrives', async () => {
    let finishFirst: (response: Response) => void = () => {}
    const slowFirst = new Promise<Response>((resolve) => {
      finishFirst = resolve
    })
    vi.stubGlobal('fetch', (input: RequestInfo | URL) => {
      const path = String(input)
      if (path === moduleApiPath('module-a')) {
        return slowFirst
      }
      if (path === moduleApiPath('module-b')) {
        return Promise.resolve(ok(publicModule({ slug: 'module-b', title: 'B' })))
      }
      throw new Error(`Unexpected request in test: ${path}`)
    })
    const { result, rerender } = renderHook(({ slug }) => usePublicModule(slug), {
      wrapper,
      initialProps: { slug: 'module-a' },
    })

    rerender({ slug: 'module-b' })
    await waitFor(() => expect(result.current.state.status).toBe('found'))
    await act(async () => {
      finishFirst(ok(publicModule({ slug: 'module-a', title: 'A' })))
    })

    expect(result.current.state).toMatchObject({ status: 'found', module: { slug: 'module-b' } })
  })
})
