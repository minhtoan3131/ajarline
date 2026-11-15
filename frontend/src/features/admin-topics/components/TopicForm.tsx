import { useId, type ChangeEvent } from 'react'

import type { TopicFields } from '../model/topic'
import type { TopicFailure } from '../model/topicFailure'
import type { PendingTopicAction } from '../model/topicManagerState'

export interface TopicFormProps {
  fields: TopicFields
  failure: TopicFailure | null
  pending: PendingTopicAction | null
  dirty: boolean
  onChange: (changes: Partial<TopicFields>) => void
  onSubmit: () => void
  onCancel: () => void
}

/** Form for one Topic. It keeps the typed text on every failure. */
export function TopicForm({ fields, failure, pending, dirty, onChange, onSubmit, onCancel }: TopicFormProps) {
  const idPrefix = useId()
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
      <h2>Topic mới</h2>
      <TopicField
        id={`${idPrefix}-slug`}
        label="Slug"
        value={fields.slug}
        error={slugError}
        hint="Chữ thường, chữ số và dấu gạch ngang. Dùng làm đường dẫn công khai và không đổi được sau khi tạo."
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
          {saving ? 'Đang tạo…' : 'Tạo'}
        </button>
        <button type="button" className="admin-button" disabled={busy} onClick={onCancel}>
          Hủy
        </button>
        <p className="topic-form-status" aria-live="polite">
          {dirty ? 'Có thay đổi chưa lưu' : ''}
        </p>
      </div>
    </form>
  )
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
