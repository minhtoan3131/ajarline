import { useDeferredValue, useId, useState } from 'react'

import { EditorField } from '../../admin-content/components/EditorField'
import { FullPreviewOverlay } from '../../admin-content/components/FullPreviewOverlay'
import { PublicationBadge } from '../../admin-content/components/PublicationControls'
import { SaveStatus } from '../../admin-content/components/SaveStatus'
import { TopicPicker } from '../../admin-content/components/TopicPicker'
import type { Versioned } from '../../admin-content/model/article'
import type { EditorNotice } from '../../admin-content/model/articleEditorState'
import { useTopicList } from '../../admin-topics/useTopicList'
import type { NewsAdminDetail, NewsTextField } from '../model/news'
import type { NewsEditorFailure } from '../model/newsEditorFailure'
import { useNewsEditor } from '../useNewsEditor'
import { NewsPreview } from './NewsPreview'
import { SourceFields } from './SourceFields'

export interface NewsEditorProps {
  /** Loaded News item, or null when the Admin creates a new one. */
  initial: Versioned<NewsAdminDetail> | null
  initialNotice: EditorNotice | null
  onCreated?: (newsId: string) => void
}

/**
 * Markdown editor for one News item.
 *
 * Save and publication are separate actions. The form never reports success
 * before the server answers, and it keeps the typed text on every failure.
 */
export function NewsEditor({ initial, initialNotice, onCreated }: NewsEditorProps) {
  const editor = useNewsEditor(initial, initialNotice, onCreated)
  const { state, dirty } = editor
  const topicList = useTopicList()
  const failure = state.failure
  const topicError = failure?.kind === 'field' && failure.field === 'topicIds' ? failure.message : null
  const sourceError = failure?.kind === 'field' && failure.field === 'source' ? failure.message : null
  const news = state.current?.value ?? null
  // The public URL must stay stable once readers may have seen it.
  const slugLocked = news !== null && news.publishedAt !== null
  const idPrefix = useId()
  const [pane, setPane] = useState<'write' | 'preview'>('write')
  const [previewOpen, setPreviewOpen] = useState(false)
  // Typing stays responsive; the preview catches up when React is idle.
  const previewFields = useDeferredValue(state.fields)

  function fieldProps(field: NewsTextField) {
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
        className="article-editor"
        noValidate
        onSubmit={(event) => {
          event.preventDefault()
          void editor.save()
        }}
      >
        <div className="article-editor-bar">
          <h1>{news === null ? 'Tin mới' : 'Sửa tin'}</h1>
          <PublicationBadge status={news?.status ?? null} />
          <SaveStatus pending={state.pending} dirty={dirty} saved={news !== null} />
          <div className="article-editor-actions">
            <button type="submit" className="admin-button admin-button-primary" disabled={state.pending !== null}>
              {state.pending === 'saving' ? 'Đang lưu…' : 'Lưu'}
            </button>
            <button type="button" className="admin-button" onClick={() => setPreviewOpen(true)}>
              Xem trước toàn màn hình
            </button>
          </div>
        </div>
        <EditorMessages
          failure={failure}
          notice={state.notice}
          reloading={state.pending === 'reloading'}
          onReload={() => void editor.reload()}
        />
        <div className="article-editor-workspace" data-pane={pane}>
          <div className="article-editor-pane-switch" role="group" aria-label="Chế độ soạn thảo">
            <button type="button" className="admin-button" aria-pressed={pane === 'write'} onClick={() => setPane('write')}>
              Soạn
            </button>
            <button
              type="button"
              className="admin-button"
              aria-pressed={pane === 'preview'}
              onClick={() => setPane('preview')}
            >
              Xem trước
            </button>
          </div>
          <section className="article-editor-fields" aria-label="Nội dung soạn thảo">
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
            <SourceFields
              idPrefix={idPrefix}
              name={state.fields.sourceName}
              url={state.fields.sourceUrl}
              publishedAt={state.fields.sourcePublishedAt}
              error={sourceError}
              onChange={editor.changeField}
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
            <EditorField label="Nội dung Markdown" {...fieldProps('bodyMarkdown')} rows={24} monospace />
          </section>
          <section className="article-editor-preview" aria-label="Xem trước trực tiếp">
            <NewsPreview fields={previewFields} />
          </section>
        </div>
      </form>
      {previewOpen && (
        <FullPreviewOverlay title="Xem trước tin" onClose={() => setPreviewOpen(false)}>
          <NewsPreview fields={state.fields} />
        </FullPreviewOverlay>
      )}
    </>
  )
}

const NOTICE_TEXT: Record<EditorNotice, string> = {
  created: 'Đã tạo tin nháp.',
  saved: 'Đã lưu.',
  reloaded: 'Đã tải bản mới nhất từ máy chủ.',
  published: 'Đã xuất bản.',
  unpublished: 'Đã gỡ xuất bản.',
}

interface EditorMessagesProps {
  failure: NewsEditorFailure | null
  notice: EditorNotice | null
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
      {failure !== null && (
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

function failureText(failure: NewsEditorFailure): string {
  switch (failure.kind) {
    case 'conflict':
      return 'Tin đã được thay đổi ở nơi khác sau khi bạn mở. Thay đổi của bạn chưa được lưu và vẫn còn trên màn hình.'
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
