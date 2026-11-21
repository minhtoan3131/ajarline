import type { ConceptFailure } from '../model/conceptFailure'
import type { ConceptNotice } from '../model/conceptManagerState'

const NOTICE_TEXT: Record<ConceptNotice, string> = {
  created: 'Đã tạo Concept.',
}

/** Success notice and failure message of the last action. */
export function ConceptMessages({ failure, notice }: { failure: ConceptFailure | null; notice: ConceptNotice | null }) {
  return (
    <>
      {notice !== null && (
        <p className="admin-notice admin-notice-success" role="status">
          {NOTICE_TEXT[notice]}
        </p>
      )}
      {failure !== null && (
        <p className="admin-notice admin-notice-error" role="alert">
          {failureText(failure)}
        </p>
      )}
    </>
  )
}

function failureText(failure: ConceptFailure): string {
  switch (failure.kind) {
    case 'field':
      return 'Có trường cần sửa; xem thông báo dưới trường đó.'
    case 'duplicate-expression':
      return (
        'Có expression trùng nhau sau khi chuẩn hoá (không phân biệt chữ hoa, chữ thường và khoảng trắng thừa). ' +
        'Hãy giữ mỗi cách nói một lần.'
      )
    case 'general':
      return failure.message
    case 'session-expired':
      return 'Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.'
  }
}
