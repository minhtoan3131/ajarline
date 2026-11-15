import { TopicIndexView } from '../features/topic-browsing/components/TopicIndexView'
import { useTopicIndex } from '../features/topic-browsing/useTopicIndex'

export function TopicIndexPage() {
  const { state, retry } = useTopicIndex()

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải danh sách chủ đề…
        </p>
      )
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được danh sách chủ đề do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'ready':
      return <TopicIndexView topics={state.topics} />
  }
}
