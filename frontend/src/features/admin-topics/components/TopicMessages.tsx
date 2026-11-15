import type { TopicFailure } from '../model/topicFailure'
import type { TopicNotice } from '../model/topicManagerState'

const NOTICE_TEXT: Record<TopicNotice, string> = {
  created: 'Đã tạo Topic.',
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
    case 'session-expired':
      return 'Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.'
    case 'session-renewed':
      return 'Phiên làm việc vừa được làm mới. Hãy thực hiện lại thao tác.'
  }
}
