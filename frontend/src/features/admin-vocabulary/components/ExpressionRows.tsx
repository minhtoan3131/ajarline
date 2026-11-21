import {
  LANGUAGE_HINT_OPTIONS,
  MAX_EXPRESSIONS,
  type ExpressionChanges,
  type ExpressionRow,
  type LanguageHint,
} from '../model/concept'

export interface ExpressionRowsProps {
  rows: ExpressionRow[]
  onAdd: () => void
  onChange: (rowKey: string, changes: ExpressionChanges) => void
  onRemove: (rowKey: string) => void
}

/** The expression rows of one Concept. The server decides which rows are valid. */
export function ExpressionRows({ rows, onAdd, onChange, onRemove }: ExpressionRowsProps) {
  const full = rows.length >= MAX_EXPRESSIONS

  return (
    <fieldset className="concept-expressions">
      <legend>Expression</legend>
      <p className="admin-field-hint">
        Mỗi expression là một cách nói của cùng một ý. Hai expression trùng nhau sau khi chuẩn hoá (không phân biệt chữ
        hoa, chữ thường và khoảng trắng thừa) sẽ bị từ chối.
      </p>
      {rows.length === 0 && <p className="admin-status">Chưa có expression nào.</p>}
      <ul className="concept-expression-list">
        {rows.map((row, index) => {
          const number = index + 1
          return (
            <li key={row.rowKey} className="concept-expression-row">
              <input
                type="text"
                className="admin-input"
                aria-label={`Expression ${number}`}
                maxLength={300}
                value={row.expression}
                onChange={(event) => onChange(row.rowKey, { expression: event.target.value })}
              />
              <select
                className="admin-input"
                aria-label={`Ngôn ngữ của expression ${number}`}
                value={row.languageHint}
                onChange={(event) => onChange(row.rowKey, { languageHint: event.target.value as LanguageHint | '' })}
              >
                <option value="">Không chỉ định</option>
                {LANGUAGE_HINT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
              <label className="concept-expression-auto">
                <input
                  type="checkbox"
                  aria-label={`Tự mở rộng expression ${number}`}
                  checked={row.autoExpand}
                  onChange={(event) => onChange(row.rowKey, { autoExpand: event.target.checked })}
                />
                <span>Tự mở rộng</span>
              </label>
              <button
                type="button"
                className="admin-button"
                aria-label={`Bỏ expression ${number}`}
                onClick={() => onRemove(row.rowKey)}
              >
                Bỏ
              </button>
            </li>
          )
        })}
      </ul>
      <div className="concept-expressions-actions">
        <button type="button" className="admin-button" disabled={full} onClick={onAdd}>
          Thêm expression
        </button>
        <span className="admin-field-hint">
          {rows.length}/{MAX_EXPRESSIONS}
        </span>
      </div>
    </fieldset>
  )
}
