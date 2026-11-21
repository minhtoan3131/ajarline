import type { TopicAdminItem } from '../model/topic'
import { useTopicList, type TopicListLoad } from '../useTopicList'
import { useTopicManager } from '../useTopicManager'
import { TopicForm } from './TopicForm'
import { TopicList } from './TopicList'
import { TopicMessages } from './TopicMessages'

/** Topic Management screen: the catalog of Topics. */
export function TopicManager() {
  const { load, reload } = useTopicList()
  const manager = useTopicManager(reload)
  const { state, dirty } = manager
  // Typed text is never dropped silently: while a form has unsaved changes or
  // an action is running, no other form can be opened. "Hủy" is the way out.
  const locked = dirty || state.pending !== null

  return (
    <section className="topic-manager">
      <div className="topic-manager-bar">
        <h1>Topic</h1>
        <button type="button" className="admin-button admin-button-primary" disabled={locked} onClick={manager.openCreate}>
          Tạo Topic
        </button>
      </div>
      <p className="admin-field-hint">
        Topic là cách tổ chức nội dung công khai. Topic đã tắt không nhận thêm bài mới, nhưng các bài đã gắn vẫn được giữ.
      </p>
      {dirty && (
        <p className="admin-field-hint">Lưu hoặc hủy thay đổi hiện tại trước khi mở Topic khác.</p>
      )}
      <TopicMessages failure={state.failure} notice={state.notice} />
      {state.panel.mode !== 'closed' && (
        <TopicForm
          key={state.panel.mode === 'edit' ? state.panel.topic.id : 'create'}
          topic={state.panel.mode === 'edit' ? state.panel.topic : null}
          fields={state.fields}
          failure={state.failure}
          pending={state.pending}
          dirty={dirty}
          onChange={manager.changeFields}
          onSubmit={() => void manager.save()}
          onCancel={manager.close}
          onDelete={() => void manager.remove()}
        />
      )}
      <TopicListSection load={load} onRetry={reload} onEdit={manager.openEdit} editDisabled={locked} />
    </section>
  )
}

interface TopicListSectionProps {
  load: TopicListLoad
  onRetry: () => void
  onEdit: (topic: TopicAdminItem) => void
  editDisabled: boolean
}

function TopicListSection({ load, onRetry, onEdit, editDisabled }: TopicListSectionProps) {
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
      return <TopicList topics={load.topics} editDisabled={editDisabled} onEdit={onEdit} />
  }
}
