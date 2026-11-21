import { ApiError } from '../../../shared/api/httpClient'
import type { ConceptFields } from './concept'

export type ConceptFailure =
  | { kind: 'field'; field: keyof ConceptFields; message: string }
  | { kind: 'duplicate-expression' }
  | { kind: 'general'; message: string }
  | { kind: 'session-expired' }

const TECHNICAL = 'Không kết nối được máy chủ hoặc máy chủ đang lỗi. Nội dung vẫn còn trên màn hình; hãy thử lại.'

// The server answers 422 without saying which field is wrong, so the
// message states every limit instead of pointing at one field.
const LIMITS =
  'Giá trị chưa hợp lệ. Key (khi tạo): chữ thường, chữ số, ngăn cách bằng dấu gạch ngang, gạch dưới hoặc dấu chấm, tối đa 160 ký tự. ' +
  'Tên: không để trống, tối đa 200 ký tự. Mô tả: tối đa 4000 ký tự. ' +
  'Expression: tối đa 50 cái, mỗi cái không để trống và tối đa 300 ký tự.'

/** Maps a failed Concept request to what the screen shows. The input stays. */
export function toConceptFailure(error: unknown): ConceptFailure {
  if (!(error instanceof ApiError)) {
    return general(TECHNICAL)
  }
  switch (error.status) {
    case 401:
      return { kind: 'session-expired' }
    case 409:
      return conflictFailure(error)
    case 422:
      return error.code === 'DUPLICATE_CONCEPT_EXPRESSION' ? { kind: 'duplicate-expression' } : general(LIMITS, error)
    default:
      return general(
        error.status === 0 || error.status >= 500
          ? TECHNICAL
          : 'Ứng dụng gửi yêu cầu không hợp lệ. Nội dung vẫn còn trên màn hình.',
        error,
      )
  }
}

function conflictFailure(error: ApiError): ConceptFailure {
  if (error.code === 'CONCEPT_KEY_CONFLICT') {
    return { kind: 'field', field: 'key', message: 'Key này đã được Concept khác sử dụng.' }
  }
  return general('Thao tác bị từ chối do xung đột dữ liệu. Hãy tải lại trang để xem dữ liệu mới nhất.', error)
}

function general(message: string, error?: ApiError): ConceptFailure {
  // requestId lets the owner find the server log line for this failure.
  const reference = error?.requestId ? ` Mã tra cứu: ${error.requestId}.` : ''
  return { kind: 'general', message: message + reference }
}
