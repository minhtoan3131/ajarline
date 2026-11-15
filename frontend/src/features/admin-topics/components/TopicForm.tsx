import { useId, useState, type ChangeEvent } from 'react'

import type { TopicAdminItem, TopicFields } from '../model/topic'
import type { TopicFailure } from '../model/topicFailure'
import type { PendingTopicAction } from '../model/topicManagerState'

export interface TopicFormProps {
  /** The Topic being edited, or null when the form creates a new one. */
  topic: TopicAdminItem | null
  fields: TopicFields
  failure: TopicFailure | null
  pending: PendingTopicAction | null
  dirty: boolean
  onChange: (changes: Partial<TopicFields>) => void
  onSubmit: () => void
  onCancel: () => void
  onDelete: () => void
}

/** Form for one Topic. It keeps the typed text on every failure. */
export function TopicForm({ topic, fields, failure, pending, dirty, onChange, onSubmit, onCancel, onDelete }: TopicFormProps) {
  const idPrefix = useId()
  const editing = topic !== null
  const saving = pending === 'saving'
  const busy = pending !== null
  const slugError = failure?.kind === 'field' && failure.field === 'slug' ? failure.message : null

  return (
    <form
      className="topic-form"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <h2>{editing ? 'Sửa Topic' : 'Topic mới'}</h2>
      <TopicField
        id={`${idPrefix}-slug`}
        label="Slug"
        value={fields.slug}
        error={slugError}
        readOnly={editing}
        hint={
          editing
            ? 'Slug đã cố định từ lúc tạo.'
            : 'Chữ thường, chữ số và dấu gạch ngang. Dùng làm đường dẫn công khai và không đổi được sau khi tạo.'
        }
        onChange={(slug) => onChange({ slug })}
      />
      <TopicField
        id={`${idPrefix}-name`}
        label="Tên"
        value={fields.name}
        error={null}
        hint="Tên hiển thị cho người đọc, tối đa 200 ký tự."
        onChange={(name) => onChange({ name })}
      />
      <TopicField
        id={`${idPrefix}-description`}
        label="Mô tả"
        value={fields.description}
        error={null}
        rows={4}
        onChange={(description) => onChange({ description })}
      />
      <div className="admin-checkbox-field">
        <input
          id={`${idPrefix}-active`}
          type="checkbox"
          checked={fields.active}
          onChange={(event) => onChange({ active: event.target.checked })}
        />
        <label htmlFor={`${idPrefix}-active`}>Cho phép gán Topic này cho bài viết mới</label>
      </div>
      <p className="admin-field-hint">Tắt không xóa Topic: các bài đã gắn vẫn được giữ.</p>
      <div className="topic-form-actions">
        <button type="submit" className="admin-button admin-button-primary" disabled={busy}>
          {submitLabel(editing, saving)}
        </button>
        <button type="button" className="admin-button" disabled={busy} onClick={onCancel}>
          Hủy
        </button>
        <p className="topic-form-status" aria-live="polite">
          {dirty ? 'Có thay đổi chưa lưu' : ''}
        </p>
      </div>
      {topic !== null && (
        <div className="topic-form-danger">
          <DeleteControl topicName={topic.name} deleting={pending === 'deleting'} disabled={busy} onDelete={onDelete} />
        </div>
      )}
    </form>
  )
}

interface DeleteControlProps {
  topicName: string
  deleting: boolean
  disabled: boolean
  onDelete: () => void
}

/** Two-step delete: the first click only asks, the second one deletes. */
function DeleteControl({ topicName, deleting, disabled, onDelete }: DeleteControlProps) {
  const [confirming, setConfirming] = useState(false)

  if (deleting) {
    return (
      <button type="button" className="admin-button" disabled>
        Đang xóa…
      </button>
    )
  }
  if (!confirming) {
    return (
      <button type="button" className="admin-button admin-button-danger" disabled={disabled} onClick={() => setConfirming(true)}>
        Xóa Topic
      </button>
    )
  }
  return (
    <div className="topic-delete-confirm" role="group" aria-label="Xác nhận xóa Topic">
      <p>Xóa Topic “{topicName}”? Việc này không hoàn tác được.</p>
      <button
        type="button"
        className="admin-button admin-button-danger"
        onClick={() => {
          // Collapse the question first, so a failed delete leaves the plain button.
          setConfirming(false)
          onDelete()
        }}
      >
        Xác nhận xóa
      </button>
      <button type="button" className="admin-button" onClick={() => setConfirming(false)}>
        Giữ lại
      </button>
    </div>
  )
}

function submitLabel(editing: boolean, saving: boolean): string {
  if (saving) {
    return editing ? 'Đang lưu…' : 'Đang tạo…'
  }
  return editing ? 'Lưu' : 'Tạo'
}

interface TopicFieldProps {
  id: string
  label: string
  value: string
  error: string | null
  onChange: (value: string) => void
  hint?: string
  readOnly?: boolean
  rows?: number
}

function TopicField({ id, label, value, error, onChange, hint, readOnly = false, rows }: TopicFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint === undefined ? null : hintId, error === null ? null : errorId]
    .filter((part) => part !== null)
    .join(' ')
  const control = {
    id,
    value,
    readOnly,
    className: 'admin-input',
    'aria-invalid': error !== null,
    'aria-describedby': describedBy === '' ? undefined : describedBy,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(event.target.value),
  }

  return (
    <div className="admin-field">
      <label htmlFor={id}>{label}</label>
      {rows === undefined ? <input type="text" {...control} /> : <textarea rows={rows} {...control} />}
      {hint !== undefined && (
        <p id={hintId} className="admin-field-hint">
          {hint}
        </p>
      )}
      {error !== null && (
        <p id={errorId} className="admin-field-error">
          Lỗi: {error}
        </p>
      )}
    </div>
  )
}
