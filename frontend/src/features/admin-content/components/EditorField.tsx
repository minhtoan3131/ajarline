import type { ChangeEvent } from 'react'

export interface EditorFieldProps {
  id: string
  label: string
  value: string
  error: string | null
  onChange: (value: string) => void
  hint?: string
  readOnly?: boolean
  rows?: number
  monospace?: boolean
}

/** A text input or textarea with its label, hint and error message. Without `rows` it is one line. */
export function EditorField({ id, label, value, error, onChange, hint, readOnly = false, rows, monospace = false }: EditorFieldProps) {
  const hintId = `${id}-hint`
  const errorId = `${id}-error`
  const describedBy = [hint === undefined ? null : hintId, error === null ? null : errorId]
    .filter((part) => part !== null)
    .join(' ')
  const control = {
    id,
    value,
    readOnly,
    className: monospace ? 'admin-input admin-input-mono' : 'admin-input',
    'aria-invalid': error !== null,
    'aria-describedby': describedBy === '' ? undefined : describedBy,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => onChange(event.target.value),
  }

  return (
    <div className="admin-field">
      <label htmlFor={id}>{label}</label>
      {rows === undefined ? (
        <input type="text" {...control} />
      ) : (
        <textarea rows={rows} spellCheck={!monospace} {...control} />
      )}
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
