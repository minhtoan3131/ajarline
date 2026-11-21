import { ApiError } from '../../../shared/api/httpClient'
import type { ArticleFields } from './article'

/** Failures that every Admin editor shows in the same way. */
export type CommonEditorFailure =
  | { kind: 'conflict' }
  | { kind: 'general'; message: string }
  | { kind: 'session-expired' }
  | { kind: 'session-renewed' }

export type EditorFailure = CommonEditorFailure | { kind: 'field'; field: keyof ArticleFields; message: string }

const TECHNICAL = 'Không kết nối được máy chủ hoặc máy chủ đang lỗi. Nội dung vẫn còn trên màn hình; hãy thử lại.'

/** Maps a failed Article editor request to what the editor shows. The input stays. */
export function toEditorFailure(error: unknown): EditorFailure {
  if (error instanceof ApiError) {
    switch (error.status) {
      case 404:
        return generalFailure('Bài viết không còn tồn tại.', error)
      case 409:
        return conflictFailure(error)
      case 422:
        return validationFailure(error)
    }
  }
  return toCommonEditorFailure(error)
}

/**
 * Maps the failures that mean the same in every Admin editor: no session, bad
 * CSRF token, a stale revision, a too large body, no connection or a server
 * error. An editor asks this after its own codes.
 */
export function toCommonEditorFailure(error: unknown): CommonEditorFailure {
  if (!(error instanceof ApiError)) {
    return generalFailure(TECHNICAL)
  }
  switch (error.status) {
    case 401:
      return { kind: 'session-expired' }
    case 403:
      return error.code === 'CSRF_INVALID'
        ? { kind: 'session-renewed' }
        : generalFailure('Phiên hiện tại không được phép thực hiện thao tác này.', error)
    case 412:
      return { kind: 'conflict' }
    case 413:
      return generalFailure('Nội dung vượt giới hạn 2 MiB nên chưa được lưu. Hãy rút gọn rồi lưu lại.', error)
    default:
      return generalFailure(
        error.status === 0 || error.status >= 500
          ? TECHNICAL
          : 'Ứng dụng gửi yêu cầu không hợp lệ. Nội dung vẫn còn trên màn hình.',
        error,
      )
  }
}

function conflictFailure(error: ApiError): EditorFailure {
  switch (error.code) {
    case 'SLUG_CONFLICT':
      return { kind: 'field', field: 'slug', message: 'Slug này đã được bài khác sử dụng.' }
    case 'SLUG_IMMUTABLE_AFTER_PUBLICATION':
      return { kind: 'field', field: 'slug', message: 'Slug không thể đổi sau lần xuất bản đầu tiên.' }
    case 'INVALID_PUBLICATION_STATE':
      return generalFailure('Trạng thái hiện tại của bài không cho phép thao tác này. Hãy tải lại trang để xem trạng thái mới nhất.', error)
    default:
      return generalFailure('Thao tác bị từ chối do xung đột dữ liệu. Hãy tải lại trang để xem dữ liệu mới nhất.', error)
  }
}

function validationFailure(error: ApiError): EditorFailure {
  switch (error.code) {
    case 'CONTENT_SUMMARY_REQUIRED':
      return { kind: 'field', field: 'summary', message: 'Cần có tóm tắt trước khi xuất bản.' }
    case 'ARTICLE_BODY_REQUIRED':
      return { kind: 'field', field: 'bodyMarkdown', message: 'Cần có nội dung bài trước khi xuất bản.' }
    case 'MODULE_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'placement', message: 'Module đã chọn không còn tồn tại. Hãy chọn lại.' }
    case 'INVALID_POSITION':
      return {
        kind: 'field',
        field: 'placement',
        message: 'Vị trí không hợp lệ cho Module đã chọn. Nhập số từ 1 đến số bài hiện có của Module cộng 1.',
      }
    case 'TOPIC_REFERENCE_NOT_FOUND':
      return { kind: 'field', field: 'topicIds', message: 'Có Topic đã chọn không còn tồn tại. Hãy tải lại trang để xem danh sách mới.' }
    case 'INACTIVE_TOPIC_ASSIGNMENT':
      return { kind: 'field', field: 'topicIds', message: 'Topic đã tắt không gắn thêm được. Hãy bỏ chọn Topic đó rồi lưu lại.' }
    default:
      return generalFailure(`Dữ liệu chưa hợp lệ: ${error.detail ?? 'hãy kiểm tra lại các trường.'}`, error)
  }
}

/** A message for the whole editor. The request id lets the owner find the server log line. */
export function generalFailure(message: string, error?: ApiError): Extract<CommonEditorFailure, { kind: 'general' }> {
  const reference = error?.requestId ? ` Mã tra cứu: ${error.requestId}.` : ''
  return { kind: 'general', message: message + reference }
}
