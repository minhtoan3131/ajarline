import { useId, type ChangeEvent } from 'react'

import type { ConceptFields, ConceptFieldChanges, ExpressionChanges } from '../model/concept'
import type { ConceptFailure } from '../model/conceptFailure'
import type { PendingConceptAction } from '../model/conceptManagerState'
import { ExpressionRows } from './ExpressionRows'

export interface ConceptFormProps {
  fields: ConceptFields
  failure: ConceptFailure | null
  pending: PendingConceptAction | null
  dirty: boolean
  onChange: (changes: ConceptFieldChanges) => void
  onAddExpression: () => void
  onChangeExpression: (rowKey: string, changes: ExpressionChanges) => void
  onRemoveExpression: (rowKey: string) => void
  onSubmit: () => void
  onCancel: () => void
}

/** Form for one Concept. It keeps the typed text on every failure. */
export function ConceptForm({
  fields,
  failure,
  pending,
  dirty,
  onChange,
  onAddExpression,
  onChangeExpression,
  onRemoveExpression,
  onSubmit,
  onCancel,
}: ConceptFormProps) {
  const idPrefix = useId()
  const saving = pending === 'saving'
  const busy = pending !== null
  const keyError = failure?.kind === 'field' && failure.field === 'key' ? failure.message : null

  return (
    <form
      className="concept-form"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        onSubmit()
      }}
    >
      <h2>Concept mới</h2>
      <ConceptField
        id={`${idPrefix}-key`}
        label="Key"
        value={fields.key}
        error={keyError}
        maxLength={160}
        hint="Chữ thường, chữ số, ngăn cách bằng dấu gạch ngang, gạch dưới hoặc dấu chấm. Không đổi được sau khi tạo."
        onChange={(key) => onChange({ key })}
      />
      <ConceptField
        id={`${idPrefix}-name`}
        label="Tên"
        value={fields.displayName}
        error={null}
        maxLength={200}
        hint="Tên hiển thị cho Admin, tối đa 200 ký tự."
        onChange={(displayName) => onChange({ displayName })}
      />
      <ConceptField
        id={`${idPrefix}-description`}
        label="Mô tả"
        value={fields.description}
        error={null}
        maxLength={4000}
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
        <label htmlFor={`${idPrefix}-active`}>Concept đang bật</label>
      </div>
      <p className="admin-field-hint">Tắt không xóa Concept: Concept đã tắt vẫn được lưu và vẫn hiện trong danh sách.</p>
      <ExpressionRows
        rows={fields.expressions}
        onAdd={onAddExpression}
        onChange={onChangeExpression}
        onRemove={onRemoveExpression}
      />
      <div className="concept-form-actions">
        <button type="submit" className="admin-button admin-button-primary" disabled={busy}>
          {saving ? 'Đang tạo…' : 'Tạo'}
        </button>
        <button type="button" className="admin-button" disabled={busy} onClick={onCancel}>
          Hủy
        </button>
        <p className="concept-form-status" aria-live="polite">
          {dirty ? 'Có thay đổi chưa lưu' : ''}
        </p>
      </div>
    </form>
  )
}

interface ConceptFieldProps {
  id: string
  label: string
  value: string
  error: string | null
  onChange: (value: string) => void
  maxLength: number
  hint?: string
  rows?: number
}

function ConceptField({ id, label, value, error, onChange, maxLength, hint, rows }: ConceptFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint === undefined ? null : hintId, error === null ? null : errorId]
    .filter((part) => part !== null)
    .join(' ')
  const control = {
    id,
    value,
    maxLength,
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
