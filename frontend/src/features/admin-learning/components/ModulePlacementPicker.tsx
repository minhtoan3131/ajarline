import { useEffect, useId, useRef, useState } from 'react'

import { ApiError, isAbortError } from '../../../shared/api/httpClient'
import { useHttpClient } from '../../../shared/api/HttpClientContext'
import { EditorField } from '../../admin-content/components/EditorField'
import type { Placement } from '../../admin-content/model/article'
import { getModule, getSeries } from '../api/learningAdminApi'
import type { LearningChild } from '../model/learning'
import { useSeriesChoices } from '../useSeriesChoices'

export interface ModulePlacementPickerProps {
  /** The Article being edited, or null when it is new. Its own place is not counted when a Module is chosen. */
  articleId: string | null
  value: Placement | null
  onChange: (placement: Placement | null) => void
  /** Error of the placement field that the editor holds. */
  error: string | null
  disabled: boolean
}

type ModulesLoad =
  | { status: 'none' }
  | { status: 'loading' }
  | { status: 'ready'; modules: LearningChild[] }
  | { status: 'failed' }

function readProblem(failure: unknown): string {
  return failure instanceof ApiError && failure.status === 404
    ? 'Module hiện tại của bài không còn tồn tại. Hãy chọn Module khác hoặc chọn bài độc lập.'
    : 'Không đọc được thông tin Module. Bài vẫn được lưu với vị trí hiện tại.'
}

/**
 * Chooses where an Article goes: standalone, or a Module of a Series and a
 * position in it. It reads its own data and keeps its own read errors, so a
 * failure here never stops the Admin from saving the Article.
 */
export function ModulePlacementPicker({ articleId, value, onChange, error, disabled }: ModulePlacementPickerProps) {
  const client = useHttpClient()
  const idPrefix = useId()
  const seriesChoices = useSeriesChoices()
  const [seriesId, setSeriesId] = useState('')
  const [modules, setModules] = useState<ModulesLoad>({ status: 'none' })
  const [modulesAttempt, setModulesAttempt] = useState(0)
  const [problem, setProblem] = useState<string | null>(null)
  // The Module the Admin chose here. It tells the first effect not to look it up again.
  const ownChoice = useRef<string | null>(null)
  // Counts the choices. An answer for an older choice is dropped.
  const latestChoice = useRef(0)
  const valueModuleId = value?.moduleId ?? null

  // Find the Series of the Module the Article is already in, for example when the editor opens.
  useEffect(() => {
    if (valueModuleId === null || valueModuleId === ownChoice.current) {
      return
    }
    const controller = new AbortController()
    setProblem(null)
    getModule(client, valueModuleId, controller.signal).then(
      (found) => {
        if (!controller.signal.aborted) {
          setSeriesId(found.value.seriesId)
        }
      },
      (failure: unknown) => {
        if (controller.signal.aborted || isAbortError(failure)) {
          return
        }
        setProblem(readProblem(failure))
      },
    )
    return () => controller.abort()
  }, [client, valueModuleId])

  // Read the Modules of the chosen Series. Changing the Series cancels the older read.
  useEffect(() => {
    if (seriesId === '') {
      setModules({ status: 'none' })
      return
    }
    const controller = new AbortController()
    setModules({ status: 'loading' })
    getSeries(client, seriesId, controller.signal).then(
      (series) => {
        if (!controller.signal.aborted) {
          setModules({ status: 'ready', modules: series.value.modules })
        }
      },
      (failure: unknown) => {
        if (controller.signal.aborted || isAbortError(failure)) {
          return
        }
        setModules({ status: 'failed' })
      },
    )
    return () => controller.abort()
  }, [client, seriesId, modulesAttempt])

  function chooseSeries(next: string) {
    latestChoice.current += 1
    ownChoice.current = null
    setProblem(null)
    setSeriesId(next)
    // The old Module belongs to the old Series. Keeping it would hide what a save does.
    onChange(null)
  }

  async function chooseModule(moduleId: string) {
    const choice = ++latestChoice.current
    ownChoice.current = moduleId === '' ? null : moduleId
    setProblem(null)
    if (moduleId === '') {
      onChange(null)
      return
    }
    if (moduleId === valueModuleId) {
      return
    }
    try {
      const chosen = await getModule(client, moduleId)
      if (choice !== latestChoice.current) {
        return
      }
      // The Article may already be in this Module. Its own place is not counted.
      const others = chosen.value.articles.filter((article) => article.id !== articleId).length
      onChange({ moduleId, position: others + 1 })
    } catch (failure) {
      if (choice === latestChoice.current) {
        setProblem(readProblem(failure))
      }
    }
  }

  function changePosition(text: string) {
    const position = Number.parseInt(text, 10)
    if (value === null || !Number.isInteger(position) || position < 1) {
      return
    }
    onChange({ moduleId: value.moduleId, position })
  }

  const items = seriesChoices.load.status === 'ready' ? seriesChoices.load.items : []
  // The Series may be older than the first page of the list. Keep it as a choice.
  const listed = items.some((item) => item.id === seriesId)
  const moduleChoice =
    value !== null && modules.status === 'ready' && modules.modules.some((module) => module.id === value.moduleId)
      ? value.moduleId
      : ''
  const errorId = `${idPrefix}-error`

  return (
    <fieldset className="placement-picker" aria-describedby={error === null ? undefined : errorId}>
      <legend>Vị trí trong lộ trình học</legend>
      {seriesChoices.load.status === 'failed' && (
        <div className="admin-notice admin-notice-warning" role="status">
          <p>Không tải được danh sách Series. Bài vẫn được lưu với vị trí hiện tại.</p>
          <button type="button" className="admin-button" onClick={seriesChoices.retry}>
            Thử lại
          </button>
        </div>
      )}
      <div className="admin-field">
        <label htmlFor={`${idPrefix}-series`}>Series</label>
        <select
          id={`${idPrefix}-series`}
          className="admin-input"
          value={seriesId}
          disabled={disabled || seriesChoices.load.status !== 'ready'}
          onChange={(event) => chooseSeries(event.target.value)}
        >
          <option value="">Bài độc lập (không thuộc Series)</option>
          {seriesId !== '' && !listed && <option value={seriesId}>Series hiện tại</option>}
          {items.map((item) => (
            <option key={item.id} value={item.id}>
              {item.title}
            </option>
          ))}
        </select>
      </div>
      {modules.status === 'loading' && (
        <p className="admin-status" role="status">
          Đang tải Module…
        </p>
      )}
      {modules.status === 'failed' && (
        <div className="admin-notice admin-notice-warning" role="status">
          <p>Không tải được danh sách Module của Series này.</p>
          <button type="button" className="admin-button" onClick={() => setModulesAttempt((count) => count + 1)}>
            Thử lại
          </button>
        </div>
      )}
      {modules.status === 'ready' &&
        (modules.modules.length === 0 ? (
          <p className="admin-status">Series này chưa có Module. Hãy tạo Module ở màn Series trước.</p>
        ) : (
          <div className="admin-field">
            <label htmlFor={`${idPrefix}-module`}>Module</label>
            <select
              id={`${idPrefix}-module`}
              className="admin-input"
              value={moduleChoice}
              disabled={disabled}
              onChange={(event) => void chooseModule(event.target.value)}
            >
              <option value="">— Chọn Module (để trống: bài độc lập) —</option>
              {modules.modules.map((module) => (
                <option key={module.id} value={module.id}>
                  {`${module.position}. ${module.title}`}
                </option>
              ))}
            </select>
          </div>
        ))}
      {value !== null && (
        <EditorField
          id={`${idPrefix}-position`}
          label="Vị trí trong Module"
          value={String(value.position)}
          error={null}
          hint="Số nguyên từ 1. Mặc định là cuối Module."
          onChange={changePosition}
        />
      )}
      {problem !== null && (
        <p className="admin-field-hint" role="status">
          {problem}
        </p>
      )}
      {error !== null && (
        <p id={errorId} className="admin-field-error">
          Lỗi: {error}
        </p>
      )}
    </fieldset>
  )
}
