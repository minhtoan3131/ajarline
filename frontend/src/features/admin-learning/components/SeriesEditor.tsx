import { useId } from 'react'

import { EditorField } from '../../admin-content/components/EditorField'
import {
  PublicationBadge,
  PublicationControls,
  PublicationHint,
} from '../../admin-content/components/PublicationControls'
import { SaveStatus } from '../../admin-content/components/SaveStatus'
import { TopicPicker } from '../../admin-content/components/TopicPicker'
import type { Versioned } from '../../admin-content/model/article'
import { useTopicList } from '../../admin-topics/useTopicList'
import type { LearningChild, LearningTextField } from '../model/learning'
import type { LearningEditorFailure } from '../model/learningEditorFailure'
import type { LearningNotice } from '../model/learningEditorState'
import type { SeriesAdminDetail } from '../model/series'
import { useSeriesEditor } from '../useSeriesEditor'
import { StatusTag } from './StatusTag'
import { useUnpublishWarning } from './UnpublishWarning'

export interface SeriesEditorProps {
  /** Loaded Series, or null when the Admin creates a new one. */
  initial: Versioned<SeriesAdminDetail> | null
  initialNotice: LearningNotice | null
  onCreated?: (seriesId: string) => void
}

/**
 * Editor for one Series. Save and publication are separate actions. The form
 * keeps the typed text on every failure. The Module list is read-only.
 */
export function SeriesEditor({ initial, initialNotice, onCreated }: SeriesEditorProps) {
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
  const childrenFailure = failure?.kind === 'children' ? failure : null
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
          </div>
        </div>
        <PublicationHint id={publishHintId} status={series?.status ?? null} dirty={dirty} />
        <EditorMessages
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
        {series !== null && <ModuleList modules={series.modules} failure={childrenFailure} />}
      </form>
      {unpublishWarning.warning}
    </>
  )
}

/** Modules of the Series in order, with each Module's own status. */
function ModuleList({
  modules,
  failure,
}: {
  modules: LearningChild[]
  failure: Extract<LearningEditorFailure, { kind: 'children' }> | null
}) {
  const headingId = useId()
  return (
    <section className="learning-children" aria-labelledby={headingId}>
      <h2 id={headingId}>Danh sách Module</h2>
      {failure !== null && (
        <p className="admin-notice admin-notice-error" role="alert">
          {failure.message}
        </p>
      )}
      {modules.length === 0 ? (
        <p className="admin-status">Series này chưa có Module.</p>
      ) : (
        <ol className="admin-list" aria-label="Module của Series">
          {modules.map((child) => (
            <li key={child.id} className="admin-list-item">
              <span className="admin-list-item-meta">{child.position}.</span>
              <span className="admin-list-item-title">{child.title}</span>
              <span className="admin-list-item-meta">{child.slug}</span>
              <StatusTag status={child.status} />
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

const NOTICE_TEXT: Record<LearningNotice, string> = {
  created: 'Đã tạo bản nháp.',
  saved: 'Đã lưu.',
  reloaded: 'Đã tải bản mới nhất từ máy chủ.',
  published: 'Đã xuất bản.',
  unpublished: 'Đã gỡ xuất bản. Các cấp bên dưới giữ nguyên trạng thái xuất bản của chúng.',
}

interface EditorMessagesProps {
  failure: LearningEditorFailure | null
  notice: LearningNotice | null
  reloading: boolean
  onReload: () => void
}

function EditorMessages({ failure, notice, reloading, onReload }: EditorMessagesProps) {
  return (
    <>
      {notice !== null && (
        <p className="admin-notice admin-notice-success" role="status">
          {NOTICE_TEXT[notice]}
        </p>
      )}
      {failure !== null && failure.kind !== 'children' && (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>{failureText(failure)}</p>
          {failure.kind === 'conflict' && (
            <button type="button" className="admin-button" disabled={reloading} onClick={onReload}>
              Tải bản mới nhất (bỏ thay đổi chưa lưu)
            </button>
          )}
        </div>
      )}
    </>
  )
}

function failureText(failure: Exclude<LearningEditorFailure, { kind: 'children' }>): string {
  switch (failure.kind) {
    case 'conflict':
      return 'Nội dung đã được thay đổi ở nơi khác sau khi bạn mở. Thay đổi của bạn chưa được lưu và vẫn còn trên màn hình.'
    case 'field':
      return 'Có trường cần sửa; xem thông báo dưới trường đó.'
    case 'general':
      return failure.message
    case 'session-expired':
      return 'Phiên đăng nhập đã hết nên thao tác chưa được thực hiện.'
    case 'session-renewed':
      return 'Phiên làm việc vừa được làm mới. Hãy thực hiện lại thao tác.'
  }
}
