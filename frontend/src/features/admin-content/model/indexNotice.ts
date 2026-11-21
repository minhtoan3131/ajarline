import { GONE_MESSAGE } from './contentDeleteFailure'

/** A message at the top of the Content Index. */
export type IndexNotice =
  | { kind: 'deleted'; title: string }
  | { kind: 'gone' }
  | { kind: 'failure'; message: string }

/** The notices that an editor can hand to the Index through the navigation state. */
export type CarriedNotice = Exclude<IndexNotice, { kind: 'failure' }>

/** Navigation state that carries a notice to the Index. */
export function indexNoticeState(notice: CarriedNotice): { indexNotice: CarriedNotice } {
  return { indexNotice: notice }
}

/** Reads a notice from navigation state. Anything else, including a hand-made value, gives `null`. */
export function indexNoticeFrom(state: unknown): CarriedNotice | null {
  if (typeof state !== 'object' || state === null || !('indexNotice' in state)) {
    return null
  }
  const notice = state.indexNotice
  if (typeof notice !== 'object' || notice === null || !('kind' in notice)) {
    return null
  }
  if (notice.kind === 'gone') {
    return { kind: 'gone' }
  }
  if (notice.kind === 'deleted' && 'title' in notice && typeof notice.title === 'string') {
    return { kind: 'deleted', title: notice.title }
  }
  return null
}

export function noticeText(notice: IndexNotice): string {
  switch (notice.kind) {
    case 'deleted':
      return `Đã xóa “${notice.title}”.`
    case 'gone':
      return GONE_MESSAGE
    case 'failure':
      return notice.message
  }
}

/** True for a notice that tells about a problem, so the screen announces it as an alert. */
export function isProblemNotice(notice: IndexNotice): boolean {
  return notice.kind !== 'deleted'
}
