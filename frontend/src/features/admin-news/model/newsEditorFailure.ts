import { ApiError } from '../../../shared/api/httpClient'
import { generalFailure, toCommonEditorFailure, type CommonEditorFailure } from '../../admin-content/model/editorFailure'
import type { NewsFields } from './news'

/** `'source'` is the whole source block. The server code does not say which part is missing. */
export type NewsEditorFailure =
  | CommonEditorFailure
  | { kind: 'field'; field: keyof NewsFields | 'source'; message: string }

/** Maps a failed News editor request to what the editor shows. The input stays. */
export function toNewsEditorFailure(error: unknown): NewsEditorFailure {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 404:
        return generalFailure('Tin này không còn tồn tại.', error)
      case 409:
        return conflictFailure(error)
      case 422:
        return validationFailure(error)
    }
  }
  return toCommonEditorFailure(error)
}

function conflictFailure(error: ApiError): NewsEditorFailure {
  switch (error.code) {
    case 'SLUG_CONFLICT':
      return { kind: 'field', field: 'slug', message: 'Slug này đã được một tin khác sử dụng.' }
    case 'SLUG_IMMUTABLE_AFTER_PUBLICATION':
      return { kind: 'field', field: 'slug', message: 'Slug không thể đổi sau lần xuất bản đầu tiên.' }
    case 'INVALID_PUBLICATION_STATE':
      return generalFailure('Trạng thái hiện tại của tin không cho phép thao tác này. Hãy tải lại trang để xem trạng thái mới nhất.', error)
    default:
      return generalFailure('Thao tác bị từ chối do xung đột dữ liệu. Hãy tải lại trang để xem dữ liệu mới nhất.', error)
  }
}

function validationFailure(error: ApiError): NewsEditorFailure {
  switch (error.code) {
    case 'CONTENT_SUMMARY_REQUIRED':
      return { kind: 'field', field: 'summary', message: 'Cần có tóm tắt trước khi xuất bản.' }
    case 'NEWS_BODY_REQUIRED':
      return { kind: 'field', field: 'bodyMarkdown', message: 'Cần có nội dung tin trước khi xuất bản.' }
    case 'NEWS_SOURCE_REQUIRED':
      return { kind: 'field', field: 'source', message: 'Chưa đủ nguồn để xuất bản.' }
    case 'TOPIC_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'topicIds', message: 'Có Topic đã chọn không còn tồn tại. Hãy tải lại trang để xem danh sách mới.' }
    case 'INACTIVE_TOPIC_ASSIGNMENT':
      return { kind: 'field', field: 'topicIds', message: 'Topic đã tắt không gắn thêm được. Hãy bỏ chọn Topic đó rồi lưu lại.' }
    default:
      return generalFailure(`Dữ liệu chưa hợp lệ: ${error.detail ?? 'hãy kiểm tra lại các trường.'}`, error)
  }
}
