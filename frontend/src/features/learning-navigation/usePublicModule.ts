import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchPublicModule } from './api/publicLearningApi'
import type { PublicModule } from './model/publicModule'

export type PublicModuleState =
  | { status: 'loading' }
  | { status: 'found'; module: PublicModule }
  | { status: 'not-found' }
  | { status: 'failed' }

/**
 * Loads the public Module for a slug. Missing and non-public Modules share
 * one not-found state. A Module with no Article is a failure, because a
 * public Module always has a public Article.
 */
export function usePublicModule(slug: string): { state: PublicModuleState; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<PublicModuleState>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setState({ status: 'loading' })
    fetchPublicModule(client, slug, controller.signal).then(
      (module) => {
        if (!controller.signal.aborted) {
          setState(module.articles.length === 0 ? { status: 'failed' } : { status: 'found', module })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        const notFound = error instanceof ApiError && error.status === 404
        setState(notFound ? { status: 'not-found' } : { status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, slug, attempt])

  return { state, retry: () => setAttempt((value) => value + 1) }
}
