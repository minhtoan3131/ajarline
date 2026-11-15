import { ApiError } from '../../../shared/api/httpClient'
import type { TopicFields } from './topic'

export type TopicFailure =
  | { kind: 'field'; field: keyof TopicFields; message: string }
  | { kind: 'general'; message: string }
  | { kind: 'not-found' }
  | { kind: 'session-expired' }
  | { kind: 'session-renewed' }

const TECHNICAL = 'Không kết nối được máy chủ hoặc máy chủ đang lỗi. Nội dung vẫn còn trên màn hình; hãy thử lại.'

// The server answers 422 without saying which field is wrong, so the
// message states every limit instead of pointing at one field.
const LIMITS =
  'Giá trị chưa hợp lệ. Slug (khi tạo): chữ thường, chữ số và dấu gạch ngang, tối đa 160 ký tự. ' +
  'Tên: không để trống, tối đa 200 ký tự. Mô tả: tối đa 4000 ký tự.'

/** Maps a failed Topic request to what the screen shows. The input stays. */
export function toTopicFailure(error: unknown): TopicFailure {
  if (!(error instanceof ApiError)) {
    return general(TECHNICAL)
  }
  switch (error.status) {
    case 401:
      return { kind: 'session-expired' }
    case 403:
      return error.code === 'CSRF_INVALID'
        ? { kind: 'session-renewed' }
        : general('Phiên hiện tại không được phép thực hiện thao tác này.', error)
    case 404:
      return { kind: 'not-found' }
    case 409:
      return conflictFailure(error)
    case 422:
      return general(LIMITS, error)
    default:
      return general(
        error.status === 0 || error.status >= 500
          ? TECHNICAL
          : 'Ứng dụng gửi yêu cầu không hợp lệ. Nội dung vẫn còn trên màn hình.',
        error,
      )
  }
}

function conflictFailure(error: ApiError): TopicFailure {
  switch (error.code) {
    case 'SLUG_CONFLICT':
      return { kind: 'field', field: 'slug', message: 'Slug này đã được Topic khác sử dụng.' }
    default:
      return general('Thao tác bị từ chối do xung đột dữ liệu. Hãy tải lại trang để xem dữ liệu mới nhất.', error)
  }
}

function general(message: string, error?: ApiError): TopicFailure {
  // requestId lets the owner find the server log line for this failure.
  const reference = error?.requestId ? ` Mã tra cứu: ${error.requestId}.` : ''
  return { kind: 'general', message: message + reference }
}
