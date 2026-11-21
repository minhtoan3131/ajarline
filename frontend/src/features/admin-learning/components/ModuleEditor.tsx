import { useId } from 'react'

import { EditorDeleteControl } from '../../admin-content/components/EditorDeleteControl'
import { EditorField } from '../../admin-content/components/EditorField'
import {
  PublicationBadge,
  PublicationControls,
  PublicationHint,
} from '../../admin-content/components/PublicationControls'
import { SaveStatus } from '../../admin-content/components/SaveStatus'
import { TopicPicker } from '../../admin-content/components/TopicPicker'
import type { Versioned } from '../../admin-content/model/article'
import type { CarriedNotice } from '../../admin-content/model/indexNotice'
import { useTopicList } from '../../admin-topics/useTopicList'
import type { LearningFields, LearningTextField, ParentFields } from '../model/learning'
import { listFailureOf } from '../model/learningEditorFailure'
import type { LearningNotice } from '../model/learningEditorState'
import type { ModuleAdminDetail } from '../model/module'
import { useModuleEditor } from '../useModuleEditor'
import { useSeriesChoices } from '../useSeriesChoices'
import { ChildOrderList } from './ChildOrderList'
import { LearningEditorMessages } from './LearningEditorMessages'
import { useUnpublishWarning } from './UnpublishWarning'

export interface ModuleEditorProps {
  /** Loaded Module, or null when the Admin creates a new one. */
  initial: Versioned<ModuleAdminDetail> | null
  initialNotice: LearningNotice | null
  /** Form values of a new Module, for example with the Series chosen on entry. */
  blank?: LearningFields
  onCreated?: (moduleId: string) => void
  /** Called after the Module was deleted or found gone. The page then leaves the editor. */
  onRemoved?: (notice: CarriedNotice) => void
}

const NO_PARENT: ParentFields = { seriesId: '', position: '' }

/**
 * Editor for one Module. Besides the text fields it has a parent: the Series
 * and the position in it. Save, order and publication are separate actions.
 */
export function ModuleEditor({ initial, initialNotice, blank, onCreated, onRemoved }: ModuleEditorProps) {
  const editor = useModuleEditor(initial, initialNotice, { blank, onCreated })
  const { state, dirty } = editor
  const topicList = useTopicList()
  const unpublishWarning = useUnpublishWarning()
  const idPrefix = useId()
  const module = state.current?.value ?? null
  // The public URL must stay stable once readers may have seen it.
  const slugLocked = module !== null && module.publishedAt !== null
  const failure = state.failure
  const topicError = failure?.kind === 'field' && failure.field === 'topicIds' ? failure.message : null
  const parentError = failure?.kind === 'field' && failure.field === 'parent' ? failure.message : null
  const publishHintId = `${idPrefix}-publish-hint`

  function fieldProps(field: LearningTextField) {
    return {
      id: `${idPrefix}-${field}`,
      value: state.fields[field],
      error: failure?.kind === 'field' && failure.field === field ? failure.message : null,
      onChange: (value: string) => editor.changeField(field, value),
    }
  }

  return (
    <>
      <form
        className="learning-editor"
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          void editor.save()
        }}
      >
        <div className="article-editor-bar">
          <h1>{module === null ? 'Module mới' : 'Sửa Module'}</h1>
          <PublicationBadge status={module?.status ?? null} />
          <SaveStatus pending={state.pending} dirty={dirty} saved={module !== null} />
          <div className="article-editor-actions">
            <button type="submit" className="admin-button admin-button-primary" disabled={state.pending !== null}>
              {state.pending === 'saving' ? 'Đang lưu…' : 'Lưu'}
            </button>
            <PublicationControls
              status={module?.status ?? null}
              dirty={dirty}
              pending={state.pending}
              hintId={publishHintId}
              onPublish={() => void editor.publish()}
              onUnpublish={() => void editor.unpublish()}
              confirmUnpublish={unpublishWarning.confirm}
            />
            <EditorDeleteControl
              type="module"
              current={state.current}
              disabled={state.pending !== null}
              onRemoved={onRemoved}
            />
          </div>
        </div>
        <PublicationHint id={publishHintId} status={module?.status ?? null} dirty={dirty} />
        <LearningEditorMessages
          failure={failure}
          notice={state.notice}
          reloading={state.pending === 'reloading'}
          onReload={() => void editor.reload()}
        />
        <section aria-label="Thông tin Module">
          <EditorField label="Tiêu đề" {...fieldProps('title')} />
          <EditorField
            label="Slug"
            {...fieldProps('slug')}
            readOnly={slugLocked}
            hint={
              slugLocked
                ? 'Slug đã cố định từ lần xuất bản đầu tiên.'
                : 'Chữ thường, chữ số và dấu gạch ngang; dùng làm đường dẫn công khai.'
            }
          />
          <EditorField label="Tóm tắt" {...fieldProps('summary')} rows={3} />
          <ParentSection
            idPrefix={idPrefix}
            parent={state.fields.parent ?? NO_PARENT}
            error={parentError}
            disabled={state.pending !== null}
            onChange={editor.changeParent}
          />
          <TopicPicker
            load={topicList.load}
            selectedIds={state.fields.topicIds}
            attachedIds={state.baseline.topicIds}
            error={topicError}
            disabled={state.pending !== null}
            onChange={editor.changeTopics}
            onRetry={topicList.reload}
          />
          <EditorField label="Giới thiệu (overview)" {...fieldProps('overviewMarkdown')} rows={12} monospace />
        </section>
        {module !== null && (
          <ChildOrderList
            heading="Danh sách bài viết"
            listLabel="Bài viết của Module"
            emptyText="Module này chưa có bài viết. Thêm bài vào Module từ màn sửa bài."
            items={editor.order.children}
            dirty={editor.order.dirty}
            pending={state.pending}
            failure={listFailureOf(failure)}
            onMove={editor.order.move}
            onSave={() => void editor.order.save()}
            onReload={() => void editor.reload()}
          />
        )}
      </form>
      {unpublishWarning.warning}
    </>
  )
}

interface ParentSectionProps {
  idPrefix: string
  parent: ParentFields
  error: string | null
  disabled: boolean
  onChange: (parent: ParentFields) => void
}

/** The Series that holds the Module and its position there. Choosing another Series empties the position. */
function ParentSection({ idPrefix, parent, error, disabled, onChange }: ParentSectionProps) {
  const choices = useSeriesChoices()
  const items = choices.load.status === 'ready' ? choices.load.items : []
  // The Series may be older than the first page of the list. Keep it as a choice.
  const listed = items.some((item) => item.id === parent.seriesId)
  const errorId = `${idPrefix}-parent-error`

  return (
    <fieldset className="learning-parent" aria-describedby={error === null ? undefined : errorId}>
      <legend>Series cha và vị trí</legend>
      {choices.load.status === 'failed' && (
        <div className="admin-notice admin-notice-warning" role="status">
          <p>Không tải được danh sách Series. Series đã chọn vẫn được giữ khi bạn lưu.</p>
          <button type="button" className="admin-button" onClick={choices.retry}>
            Thử lại
          </button>
        </div>
      )}
      <div className="admin-field">
        <label htmlFor={`${idPrefix}-series`}>Series cha</label>
        <select
          id={`${idPrefix}-series`}
          className="admin-input"
          value={parent.seriesId}
          disabled={disabled}
          onChange={(event) => onChange({ seriesId: event.target.value, position: '' })}
        >
          <option value="">— Chọn Series —</option>
          {parent.seriesId !== '' && !listed && <option value={parent.seriesId}>Series hiện tại</option>}
          {items.map((item) => (
            <option key={item.id} value={item.id}>
              {item.title}
            </option>
          ))}
        </select>
      </div>
      <EditorField
        id={`${idPrefix}-position`}
        label="Vị trí"
        value={parent.position}
        error={null}
        hint="Số nguyên từ 1. Để ở cuối Series, nhập số Module hiện có cộng 1."
        onChange={(position) => onChange({ ...parent, position })}
      />
      {error !== null && (
        <p id={errorId} className="admin-field-error">
          Lỗi: {error}
        </p>
      )}
    </fieldset>
  )
}
