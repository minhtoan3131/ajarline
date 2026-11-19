import { useId } from 'react'

import type { TopicAdminItem } from '../../admin-topics/model/topic'
import type { TopicListLoad } from '../../admin-topics/useTopicList'

export interface TopicPickerProps {
  load: TopicListLoad
  selectedIds: string[]
  /** Topics already saved on this Article. They can be kept or removed even when turned off. */
  attachedIds: string[]
  error: string | null
  disabled: boolean
  onChange: (topicIds: string[]) => void
  onRetry: () => void
}

/** Check boxes for the Topics of one Article. */
export function TopicPicker({ load, selectedIds, attachedIds, error, disabled, onChange, onRetry }: TopicPickerProps) {
  const idPrefix = useId()
  const errorId = `${idPrefix}-error`

  function toggle(topicId: string, checked: boolean) {
    onChange(checked ? [...selectedIds, topicId] : selectedIds.filter((id) => id !== topicId))
  }

  return (
    <fieldset className="topic-picker" aria-describedby={error === null ? undefined : errorId}>
      <legend>Chủ đề (Topic)</legend>
      {load.status === 'loading' && (
        <p className="admin-status" role="status">
          Đang tải danh sách Topic…
        </p>
      )}
      {load.status === 'failed' && (
        <div className="admin-notice admin-notice-warning" role="status">
          <p>Không tải được danh sách Topic. Các Topic đã chọn vẫn được giữ khi bạn lưu bài.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )}
      {load.status === 'ready' && load.topics.length === 0 && (
        <p className="admin-status">Chưa có Topic nào. Hãy tạo Topic ở mục Tổ chức › Topic.</p>
      )}
      {load.status === 'ready' && load.topics.length > 0 && (
        <ul className="topic-picker-list">
          {load.topics.map((topic) => (
            <TopicChoice
              key={topic.id}
              id={`${idPrefix}-${topic.id}`}
              topic={topic}
              checked={selectedIds.includes(topic.id)}
              attached={attachedIds.includes(topic.id)}
              disabled={disabled}
              onToggle={(checked) => toggle(topic.id, checked)}
            />
          ))}
        </ul>
      )}
      <p className="admin-field-hint">Topic đã tắt không gắn thêm được; bài đã gắn sẵn thì vẫn giữ được.</p>
      {error !== null && (
        <p id={errorId} className="admin-field-error">
          Lỗi: {error}
        </p>
      )}
    </fieldset>
  )
}

interface TopicChoiceProps {
  id: string
  topic: TopicAdminItem
  checked: boolean
  attached: boolean
  disabled: boolean
  onToggle: (checked: boolean) => void
}

function TopicChoice({ id, topic, checked, attached, disabled, onToggle }: TopicChoiceProps) {
  const badgeId = `${id}-badge`
  // The server refuses to add a turned-off Topic, so the box stays locked
  // unless the Article already carries that Topic.
  const locked = !topic.active && !attached
  return (
    <li className="topic-picker-item">
      <input
        id={id}
        type="checkbox"
        checked={checked}
        disabled={disabled || locked}
        aria-describedby={topic.active ? undefined : badgeId}
        onChange={(event) => onToggle(event.target.checked)}
      />
      <label htmlFor={id}>{topic.name}</label>
      {!topic.active && (
        <span id={badgeId} className="admin-badge admin-badge-inactive">
          Đã tắt
        </span>
      )}
    </li>
  )
}
