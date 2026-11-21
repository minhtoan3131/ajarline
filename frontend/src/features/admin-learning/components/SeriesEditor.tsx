import { useId } from 'react'
import { Link } from 'react-router'

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
import { moduleEditorPath, newModulePath } from '../learningRoutes'
import type { LearningTextField } from '../model/learning'
import { listFailureOf } from '../model/learningEditorFailure'
import type { LearningNotice } from '../model/learningEditorState'
import type { SeriesAdminDetail } from '../model/series'
import { useSeriesEditor } from '../useSeriesEditor'
import { ChildOrderList } from './ChildOrderList'
import { LearningEditorMessages } from './LearningEditorMessages'
import { useUnpublishWarning } from './UnpublishWarning'

export interface SeriesEditorProps {
  /** Loaded Series, or null when the Admin creates a new one. */
  initial: Versioned<SeriesAdminDetail> | null
  initialNotice: LearningNotice | null
  onCreated?: (seriesId: string) => void
  /** Called after the Series was deleted or found gone. The page then leaves the editor. */
  onRemoved?: (notice: CarriedNotice) => void
}

/**
 * Editor for one Series. Save, order and publication are separate actions.
 * The form keeps the typed text on every failure.
 */
export function SeriesEditor({ initial, initialNotice, onCreated, onRemoved }: SeriesEditorProps) {
  const editor = useSeriesEditor(initial, initialNotice, onCreated)
  const { state, dirty } = editor
  const topicList = useTopicList()
  const unpublishWarning = useUnpublishWarning()
  const idPrefix = useId()
  const series = state.current?.value ?? null
  // The public URL must stay stable once readers may have seen it.
  const slugLocked = series !== null && series.publishedAt !== null
  const failure = state.failure
  const topicError = failure?.kind === 'field' && failure.field === 'topicIds' ? failure.message : null
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
          <h1>{series === null ? 'Series mới' : 'Sửa Series'}</h1>
          <PublicationBadge status={series?.status ?? null} />
          <SaveStatus pending={state.pending} dirty={dirty} saved={series !== null} />
          <div className="article-editor-actions">
            <button type="submit" className="admin-button admin-button-primary" disabled={state.pending !== null}>
              {state.pending === 'saving' ? 'Đang lưu…' : 'Lưu'}
            </button>
            <PublicationControls
              status={series?.status ?? null}
              dirty={dirty}
              pending={state.pending}
              hintId={publishHintId}
              onPublish={() => void editor.publish()}
              onUnpublish={() => void editor.unpublish()}
              confirmUnpublish={unpublishWarning.confirm}
            />
            <EditorDeleteControl
              type="series"
              current={state.current}
              disabled={state.pending !== null}
              onRemoved={onRemoved}
            />
          </div>
        </div>
        <PublicationHint id={publishHintId} status={series?.status ?? null} dirty={dirty} />
        <LearningEditorMessages
          failure={failure}
          notice={state.notice}
          reloading={state.pending === 'reloading'}
          onReload={() => void editor.reload()}
        />
        <section aria-label="Thông tin Series">
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
        {series !== null && (
          <>
            <ChildOrderList
              heading="Danh sách Module"
              listLabel="Module của Series"
              emptyText="Series này chưa có Module."
              items={editor.order.children}
              dirty={editor.order.dirty}
              pending={state.pending}
              failure={listFailureOf(failure)}
              pathOf={moduleEditorPath}
              onMove={editor.order.move}
              onSave={() => void editor.order.save()}
              onReload={() => void editor.reload()}
            />
            <div className="learning-children-actions">
              <Link className="admin-button" to={newModulePath(series.id)}>
                Thêm Module
              </Link>
            </div>
          </>
        )}
      </form>
      {unpublishWarning.warning}
    </>
  )
}
