import type { TopicFailure } from '../model/topicFailure'
import type { TopicNotice } from '../model/topicManagerState'

const NOTICE_TEXT: Record<TopicNotice, string> = {
  created: 'Đã tạo Topic.',
  saved: 'Đã lưu Topic.',
  deleted: 'Đã xóa Topic.',
}

/** Success notice and failure message of the last action. */
export function TopicMessages({ failure, notice }: { failure: TopicFailure | null; notice: TopicNotice | null }) {
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

function failureText(failure: TopicFailure): string {
  switch (failure.kind) {
    case 'field':
      return 'Có trường cần sửa; xem thông báo dưới trường đó.'
    case 'general':
      return failure.message
    case 'not-found':
      return 'Topic này không còn tồn tại. Danh sách đã được tải lại.'
    case 'in-use':
      return (
        'Topic này vẫn đang được nội dung sử dụng nên chưa xóa được. ' +
        'Nếu chỉ muốn ngừng dùng, hãy bỏ chọn "Cho phép gán Topic này cho bài viết mới" rồi lưu: Topic được tắt chứ không bị xóa.'
      )
    case 'session-expired':
      return 'Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.'
    case 'session-renewed':
      return 'Phiên làm việc vừa được làm mới. Hãy thực hiện lại thao tác.'
  }
}
