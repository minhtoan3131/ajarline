import { useId, useState } from 'react'

import type { PublicationStatus } from '../model/article'
import {
  ADMIN_CONTENT_TYPES,
  CONTENT_TYPE_LABELS,
  PUBLICATION_STATUSES,
} from '../model/contentIndex'
import { MAX_SEARCH_LENGTH, type ContentIndexQuery } from '../model/contentIndexQuery'
import { STATUS_LABELS } from './PublicationControls'

export interface ContentFiltersProps {
  query: ContentIndexQuery
  /** Called with the new query. A change of any filter goes back to the first page. */
  onChange: (query: ContentIndexQuery) => void
}

/** Type filter, status filter and search box of the Content Index. */
export function ContentFilters({ query, onChange }: ContentFiltersProps) {
  return (
    <div className="content-filters">
      <CheckGroup
        legend="Loại"
        values={ADMIN_CONTENT_TYPES}
        labels={CONTENT_TYPE_LABELS}
        selected={query.types}
        onChange={(types) => onChange({ ...query, types, page: 0 })}
      />
      <CheckGroup<PublicationStatus>
        legend="Trạng thái"
        values={PUBLICATION_STATUSES}
        labels={STATUS_LABELS}
        selected={query.statuses}
        onChange={(statuses) => onChange({ ...query, statuses, page: 0 })}
      />
      <SearchBox key={query.q} initial={query.q} onSearch={(q) => onChange({ ...query, q, page: 0 })} />
      <p className="admin-field-hint">Không chọn gì ở một nhóm nghĩa là lấy tất cả.</p>
    </div>
  )
}

interface CheckGroupProps<T extends string> {
  legend: string
  values: readonly T[]
  labels: Record<T, string>
  selected: readonly T[]
  onChange: (selected: T[]) => void
}

function CheckGroup<T extends string>({ legend, values, labels, selected, onChange }: CheckGroupProps<T>) {
  return (
    <fieldset className="content-filter-group">
      <legend>{legend}</legend>
      {values.map((value) => (
        <label key={value} className="content-filter-option">
          <input
            type="checkbox"
            checked={selected.includes(value)}
            onChange={(event) =>
              onChange(values.filter((other) => (other === value ? event.target.checked : selected.includes(other))))
            }
          />
          {labels[value]}
        </label>
      ))}
    </fieldset>
  )
}

function SearchBox({ initial, onSearch }: { initial: string; onSearch: (q: string) => void }) {
  const inputId = useId()
  const [text, setText] = useState(initial)

  return (
    <form
      className="content-search"
      role="search"
      aria-label="Tìm nội dung"
      onSubmit={(event) => {
        event.preventDefault()
        onSearch(text.trim())
      }}
    >
      <label htmlFor={inputId}>Tìm theo tiêu đề hoặc slug</label>
      <input
        id={inputId}
        type="search"
        className="admin-input"
        value={text}
        maxLength={MAX_SEARCH_LENGTH}
        onChange={(event) => setText(event.target.value)}
      />
      <button type="submit" className="admin-button">
        Tìm
      </button>
      {text !== '' && (
        <button
          type="button"
          className="admin-button"
          onClick={() => {
            setText('')
            if (initial !== '') {
              onSearch('')
            }
          }}
        >
          Bỏ chữ tìm
        </button>
      )}
    </form>
  )
}
