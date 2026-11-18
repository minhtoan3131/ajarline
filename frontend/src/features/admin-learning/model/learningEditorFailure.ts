import { ApiError } from '../../../shared/api/httpClient'
import {
  generalFailure,
  toCommonEditorFailure,
  type CommonEditorFailure,
} from '../../admin-content/model/editorFailure'
import type { LearningFields } from './learning'

export type LearningEditorFailure =
  | CommonEditorFailure
  | { kind: 'field'; field: keyof LearningFields; message: string }
  /** Publish was refused because of the children. It shows at the child list, where the fix is. */
  | { kind: 'children'; message: string }

/** Maps a failed Series editor request to what the editor shows. The input stays. */
export function toLearningEditorFailure(error: unknown): LearningEditorFailure {
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
    case 'TOPIC_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'topicIds', message: 'Có Topic đã chọn không còn tồn tại. Hãy tải lại trang để xem danh sách mới.' }
    case 'INACTIVE_TOPIC_ASSIGNMENT':
      return { kind: 'field', field: 'topicIds', message: 'Topic đã tắt không gắn thêm được. Hãy bỏ chọn Topic đó rồi lưu lại.' }
    default:
      return generalFailure(`Dữ liệu chưa hợp lệ: ${error.detail ?? 'hãy kiểm tra lại các trường.'}`, error)
  }
}
