import { useNavigate } from 'react-router'

import { CONTENT_INDEX_PATH } from './contentRoutes'
import { indexNoticeState, type CarriedNotice } from './model/indexNotice'

/**
 * Returns a function that takes the Admin to the Content Index with a
 * notice. It replaces the editor URL, so Back does not reopen an item that
 * no longer exists.
 */
export function useLeaveToIndex(): (notice: CarriedNotice) => void {
  const navigate = useNavigate()
  return (notice) => {
    void navigate(CONTENT_INDEX_PATH, { replace: true, state: indexNoticeState(notice) })
  }
}
