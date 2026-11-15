import { useTopicList, type TopicListLoad } from '../useTopicList'
import { TopicList } from './TopicList'

/** Topic Management screen (ADM-07): the catalog of Topics. */
export function TopicManager() {
  const { load, reload } = useTopicList()

  return (
    <section className="topic-manager">
      <div className="topic-manager-bar">
        <h1>Topic</h1>
      </div>
      <p className="admin-field-hint">
        Topic là cách tổ chức nội dung công khai. Topic đã tắt không nhận thêm bài mới, nhưng các bài đã gắn vẫn được giữ.
      </p>
      <TopicListSection load={load} onRetry={reload} />
    </section>
  )
}

function TopicListSection({ load, onRetry }: { load: TopicListLoad; onRetry: () => void }) {
  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải danh sách Topic…
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được danh sách Topic do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return <TopicList topics={load.topics} />
  }
}
