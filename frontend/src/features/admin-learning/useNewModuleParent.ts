import { useEffect, useState } from 'react'

import { isAbortError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { getSeries } from './api/learningAdminApi'
import type { ParentFields } from './model/learning'

export type NewModuleParent = { status: 'loading' } | { status: 'ready'; parent: ParentFields | null }

const NOTHING_CHOSEN: NewModuleParent = { status: 'ready', parent: null }

/**
 * Reads the Series named in the URL so a new Module can start at its end.
 * A missing, wrong or unreadable Series is not an error: nothing is chosen.
 */
export function useNewModuleParent(seriesId: string | null): NewModuleParent {
  const client = useHttpClient()
  const [parent, setParent] = useState<NewModuleParent>(seriesId === null ? NOTHING_CHOSEN : { status: 'loading' })

  useEffect(() => {
    if (seriesId === null) {
      setParent(NOTHING_CHOSEN)
      return
    }
    const controller = new AbortController()
    setParent({ status: 'loading' })
    getSeries(client, seriesId, controller.signal).then(
      (series) => {
        if (!controller.signal.aborted) {
          // The new Module goes after the last one.
          setParent({
            status: 'ready',
            parent: { seriesId: series.value.id, position: String(series.value.modules.length + 1) },
          })
        }
      },
      (error: unknown) => {
        if (controller.signal.aborted || isAbortError(error)) {
          return
        }
        setParent(NOTHING_CHOSEN)
      },
    )
    return () => controller.abort()
  }, [client, seriesId])

  return parent
}
