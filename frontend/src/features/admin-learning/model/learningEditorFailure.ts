import { ApiError } from '../../../shared/api/httpClient'
import {
  generalFailure,
  toCommonEditorFailure,
  type CommonEditorFailure,
} from '../../admin-content/model/editorFailure'
import type { LearningFields } from './learning'
import { ParentNotReadyError } from './module'

export type LearningEditorFailure =
  | CommonEditorFailure
  | { kind: 'field'; field: keyof LearningFields; message: string }
  /** Publish was refused because of the children. It shows at the child list, where the fix is. */
  | { kind: 'children'; message: string }
  /** The children changed elsewhere, so the new order cannot be saved. It shows at the child list. */
  | { kind: 'order'; message: string }

/** Maps a failed Series or Module editor request to what the editor shows. The input stays. */
export function toLearningEditorFailure(error: unknown): LearningEditorFailure {
  if (error instanceof ParentNotReadyError) {
    return {
      kind: 'field',
      field: 'parent',
      message: 'Hãy chọn Series cha và nhập vị trí là số nguyên từ 1 trở lên.',
    }
  }
  if (error instanceof ApiError) {
    switch (error.status) {
      case 404:
        return generalFailure('Nội dung này không còn tồn tại.', error)
      case 409:
        return conflictFailure(error)
      case 422:
        return validationFailure(error)
    }
  }
  return toCommonEditorFailure(error)
}

function conflictFailure(error: ApiError): LearningEditorFailure {
  switch (error.code) {
    case 'SLUG_CONFLICT':
      return { kind: 'field', field: 'slug', message: 'Slug này đã được một nội dung khác cùng loại sử dụng.' }
    case 'SLUG_IMMUTABLE_AFTER_PUBLICATION':
      return { kind: 'field', field: 'slug', message: 'Slug không thể đổi sau lần xuất bản đầu tiên.' }
    case 'ORDER_SET_CONFLICT':
      return {
        kind: 'order',
        message: 'Danh sách đã thay đổi ở nơi khác nên chưa lưu được thứ tự. Hãy tải lại để sắp xếp trên danh sách mới nhất.',
      }
    case 'INVALID_PUBLICATION_STATE':
      return generalFailure(
        'Trạng thái hiện tại không cho phép thao tác này. Hãy tải lại trang để xem trạng thái mới nhất.',
        error,
      )
    default:
      return generalFailure('Thao tác bị từ chối do xung đột dữ liệu. Hãy tải lại trang để xem dữ liệu mới nhất.', error)
  }
}

function validationFailure(error: ApiError): LearningEditorFailure {
  switch (error.code) {
    case 'CONTENT_SUMMARY_REQUIRED':
      return { kind: 'field', field: 'summary', message: 'Cần có tóm tắt cho nội dung đang hoặc sắp xuất bản.' }
    case 'SERIES_OVERVIEW_REQUIRED':
    case 'MODULE_OVERVIEW_REQUIRED':
      return {
        kind: 'field',
        field: 'overviewMarkdown',
        message: 'Cần có phần giới thiệu (overview) cho nội dung đang hoặc sắp xuất bản.',
      }
    case 'SERIES_HAS_NO_PUBLIC_MODULE':
      return {
        kind: 'children',
        message:
          'Series cần ít nhất một Module đã xuất bản và có bài viết công khai. Xem trạng thái từng Module bên dưới để biết Module nào cần xuất bản hoặc cần thêm bài.',
      }
    case 'MODULE_HAS_NO_PUBLIC_ARTICLE':
      return {
        kind: 'children',
        message:
          'Module cần ít nhất một bài viết đã xuất bản. Xem trạng thái từng bài bên dưới để biết bài nào cần xuất bản, hoặc thêm bài vào Module từ màn sửa bài.',
      }
    case 'SERIES_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'parent', message: 'Series đã chọn không còn tồn tại. Hãy chọn lại Series.' }
    case 'INVALID_POSITION':
      return {
        kind: 'field',
        field: 'parent',
        message: 'Vị trí không hợp lệ. Nhập số từ 1 đến số Module hiện có của Series đã chọn cộng 1.',
      }
    case 'TOPIC_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'topicIds', message: 'Có Topic đã chọn không còn tồn tại. Hãy tải lại trang để xem danh sách mới.' }
    case 'INACTIVE_TOPIC_ASSIGNMENT':
      return { kind: 'field', field: 'topicIds', message: 'Topic đã tắt không gắn thêm được. Hãy bỏ chọn Topic đó rồi lưu lại.' }
    default:
      return generalFailure(`Dữ liệu chưa hợp lệ: ${error.detail ?? 'hãy kiểm tra lại các trường.'}`, error)
  }
}

/** The failures that show at the child list instead of at the top of the editor. */
export type ListFailure = Extract<LearningEditorFailure, { kind: 'children' | 'order' }>

export function listFailureOf(failure: LearningEditorFailure | null): ListFailure | null {
  return failure !== null && (failure.kind === 'children' || failure.kind === 'order') ? failure : null
}
