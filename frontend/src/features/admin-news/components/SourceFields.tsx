import { EditorField } from '../../admin-content/components/EditorField'
import type { SourceField } from '../model/news'

export interface SourceFieldsProps {
  idPrefix: string
  name: string
  url: string
  publishedAt: string
  /** Error of the whole block, for example when the source is not enough to publish. */
  error: string | null
  onChange: (field: SourceField, value: string) => void
}

/** The source block of a News item. It sits under the summary and above the body. */
export function SourceFields({ idPrefix, name, url, publishedAt, error, onChange }: SourceFieldsProps) {
  const errorId = `${idPrefix}-source-error`
  const dateId = `${idPrefix}-sourcePublishedAt`
  return (
    <fieldset
      className={error === null ? 'news-source' : 'news-source news-source-invalid'}
      aria-describedby={error === null ? undefined : errorId}
    >
      <legend>Nguồn tin</legend>
      <p className="admin-field-hint">Cần tên nguồn và URL hợp lệ để xuất bản.</p>
      <EditorField
        id={`${idPrefix}-sourceName`}
        label="Tên nguồn"
        value={name}
        error={null}
        onChange={(value) => onChange('sourceName', value)}
      />
      <EditorField
        id={`${idPrefix}-sourceUrl`}
        label="URL nguồn"
        value={url}
        error={null}
        hint="Địa chỉ đầy đủ, bắt đầu bằng http:// hoặc https://, không có khoảng trắng."
        onChange={(value) => onChange('sourceUrl', value)}
      />
      <div className="admin-field">
        <label htmlFor={dateId}>Ngày nguồn đăng (không bắt buộc)</label>
        <input
          id={dateId}
          type="datetime-local"
          className="admin-input"
          value={publishedAt}
          onChange={(event) => onChange('sourcePublishedAt', event.target.value)}
        />
      </div>
      {error !== null && (
        <p id={errorId} className="admin-field-error">
          Lỗi: {error}
        </p>
      )}
    </fieldset>
  )
}
