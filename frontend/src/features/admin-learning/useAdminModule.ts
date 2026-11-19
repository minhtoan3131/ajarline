import { useEffect, useState } from 'react'

import { ApiError, isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import type { Versioned } from '../admin-content/model/article'
import { getModule } from './api/learningAdminApi'
import type { ModuleAdminDetail } from './model/module'

export type AdminModuleLoad =
  | { status: 'loading' }
  | { status: 'ready'; module: Versioned<ModuleAdminDetail> }
  | { status: 'not-found' }
  | { status: 'failed' }

/** Loads a Module for the editor by id, for direct entry and reload. */
export function useAdminModule(moduleId: string): { load: AdminModuleLoad; retry: () => void } {
  const client = useHttpClient()
  const [attempt, setAttempt] = useState(0)
  const [load, setLoad] = useState<AdminModuleLoad>({ status: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    setLoad({ status: 'loading' })
    getModule(client, moduleId, controller.signal).then(
      (module) => {
        if (!controller.signal.aborted) {
          setLoad({ status: 'ready', module })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        const notFound = error instanceof ApiError && error.status === 404
        setLoad(notFound ? { status: 'not-found' } : { status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, moduleId, attempt])

  return { load, retry: () => setAttempt((value) => value + 1) }
}
