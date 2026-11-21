import { ApiError } from '../../../shared/api/httpClient'
import type { AdminContentType } from './contentIndex'
import { generalFailure, toCommonEditorFailure } from './editorFailure'

export type DeleteFailureKind =
  | 'ever-published'
  | 'in-use'
  | 'conflict'
  | 'gone'
  | 'session-expired'
  | 'session-renewed'
  | 'general'

export interface DeleteFailure {
  kind: DeleteFailureKind
  message: string
}

export const GONE_MESSAGE = 'Mục này không còn nữa. Có thể nó đã bị xóa ở nơi khác.'

/** The kind of child that stops a delete. Article and News have none. */
const CHILD_NAMES: Partial<Record<AdminContentType, string>> = {
  series: 'Module',
  module: 'bài viết',
}

/**
 * Maps a failed delete request to what the screen shows. The server code
 * decides the meaning, never the text of the server message.
 */
export function describeDeleteFailure(error: unknown, type: AdminContentType): DeleteFailure {
  if (error instanceof ApiError) {
    if (error.status === 404) {
      return { kind: 'gone', message: GONE_MESSAGE }
    }
    if (error.status === 409 && error.code === 'CONTENT_ALREADY_EVER_PUBLISHED') {
      return {
        kind: 'ever-published',
        message: 'Mục này đã từng được xuất bản nên không xóa được. Nếu muốn ngừng công khai, hãy gỡ xuất bản.',
      }
    }
    if (error.status === 409 && error.code === 'CONTENT_IN_USE') {
      const child = CHILD_NAMES[type] ?? 'nội dung con'
      return {
        kind: 'in-use',
        message: `Mục này còn ${child} bên trong nên chưa xóa được. Hãy xử lý ${child} trước rồi xóa lại.`,
      }
    }
    if (error.status === 409 || error.status === 422) {
      return { kind: 'general', message: generalFailure('Máy chủ từ chối yêu cầu xóa.', error).message }
    }
  }
  const common = toCommonEditorFailure(error)
  switch (common.kind) {
    case 'conflict':
      return { kind: 'conflict', message: 'Mục này vừa được thay đổi ở nơi khác. Hãy tải lại rồi xem lại trước khi xóa.' }
    case 'session-expired':
      return { kind: 'session-expired', message: 'Phiên đăng nhập đã hết nên chưa xóa được.' }
    case 'session-renewed':
      return { kind: 'session-renewed', message: 'Phiên làm việc vừa được làm mới. Hãy bấm xóa lại.' }
    case 'general':
      return { kind: 'general', message: common.message }
  }
}

/** False when trying again cannot work: the item was published once, or it is gone. */
export function allowsRetry(failure: DeleteFailure): boolean {
  return failure.kind !== 'ever-published' && failure.kind !== 'gone'
}

/** True when the list on screen is out of date after this failure, so the caller loads it again. */
export function needsReload(failure: DeleteFailure): boolean {
  return failure.kind === 'conflict' || failure.kind === 'gone' || failure.kind === 'ever-published'
}
