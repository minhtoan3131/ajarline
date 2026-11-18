import type { Dispatch } from 'react'

import { useHttpClient } from '../../shared/api/HttpClientContext'
import { reorderArticles, reorderModules } from './api/learningAdminApi'
import type { LearningChild } from './model/learning'
import { childrenOf } from './model/learningDetail'
import type { LearningEditorAction, LearningEditorState } from './model/learningEditorState'

export interface ChildOrderController {
  /** The children as the Admin sees them: the arranged order if there is one, else the saved order. */
  readonly children: LearningChild[]
  /** There is an arranged order that is not saved yet. */
  readonly dirty: boolean
  move(index: number, offset: -1 | 1): void
  save(): Promise<void>
}

/**
 * Arranges and saves the order of the children of the open Series or Module.
 * It keeps no state of its own: it writes to the editor's reducer, so the
 * ETag of the parent stays in one place.
 */
export function useChildOrder(
  state: LearningEditorState,
  dispatch: Dispatch<LearningEditorAction>,
  fail: (error: unknown) => void,
): ChildOrderController {
  const client = useHttpClient()
  const children = state.order ?? (state.current === null ? [] : childrenOf(state.current.value))

  function move(index: number, offset: -1 | 1) {
    const target = index + offset
    if (target < 0 || target >= children.length) {
      return
    }
    const arranged = [...children]
    const [moved] = arranged.splice(index, 1)
    arranged.splice(target, 0, moved)
    dispatch({ type: 'order-changed', order: arranged })
  }

  async function save() {
    const current = state.current
    const order = state.order
    if (current === null || order === null || state.pending !== null) {
      return
    }
    dispatch({ type: 'mutation-started', pending: 'ordering', submitted: null })
    try {
      const ids = order.map((child) => child.id)
      const detail = current.value
      const saved =
        detail.type === 'series'
          ? await reorderModules(client, detail.id, ids, current.etag)
          : await reorderArticles(client, detail.id, ids, current.etag)
      dispatch({ type: 'order-saved', order: saved })
    } catch (error) {
      fail(error)
    }
  }

  return { children, dirty: state.order !== null, move, save }
}
