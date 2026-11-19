import type { LearningEditorFailure } from '../model/learningEditorFailure'
import type { LearningNotice } from '../model/learningEditorState'

const NOTICE_TEXT: Record<LearningNotice, string> = {
  created: 'Đã tạo bản nháp.',
  saved: 'Đã lưu.',
  'order-saved': 'Đã lưu thứ tự.',
  reloaded: 'Đã tải bản mới nhất từ máy chủ.',
  published: 'Đã xuất bản.',
  unpublished: 'Đã gỡ xuất bản. Các cấp bên dưới giữ nguyên trạng thái xuất bản của chúng.',
}

export interface LearningEditorMessagesProps {
  failure: LearningEditorFailure | null
  notice: LearningNotice | null
  reloading: boolean
  onReload: () => void
}

/** The notice and the failure at the top of a Series or Module editor. List failures are not shown here. */
export function LearningEditorMessages({ failure, notice, reloading, onReload }: LearningEditorMessagesProps) {
  const atTop = failure !== null && failure.kind !== 'children' && failure.kind !== 'order'
  return (
    <>
      {notice !== null && (
        <p className="admin-notice admin-notice-success" role="status">
          {NOTICE_TEXT[notice]}
        </p>
      )}
      {atTop && (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>{failureText(failure)}</p>
          {failure.kind === 'conflict' && (
            <button type="button" className="admin-button" disabled={reloading} onClick={onReload}>
              Tải bản mới nhất (bỏ thay đổi chưa lưu)
            </button>
          )}
        </div>
      )}
    </>
  )
}

function failureText(failure: LearningEditorFailure): string {
  switch (failure.kind) {
    case 'conflict':
      return 'Nội dung đã được thay đổi ở nơi khác sau khi bạn mở. Thay đổi của bạn chưa được lưu và vẫn còn trên màn hình.'
    case 'field':
      return 'Có trường cần sửa; xem thông báo dưới trường đó.'
    case 'general':
    case 'children':
    case 'order':
      return failure.message
    case 'session-expired':
      return 'Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.'
    case 'session-renewed':
      return 'Phiên làm việc vừa được làm mới. Hãy thực hiện lại thao tác.'
  }
}
