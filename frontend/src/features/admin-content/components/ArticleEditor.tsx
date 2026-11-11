import { useId, type ChangeEvent } from 'react'

import type { ArticleAdminDetail, ArticleFields, PublicationStatus, Versioned } from '../model/article'
import type { EditorNotice, PendingMutation } from '../model/articleEditorState'
import type { EditorFailure } from '../model/editorFailure'
import { useArticleEditor } from '../useArticleEditor'

export interface ArticleEditorProps {
  /** Loaded Article, or null when the Admin creates a new one. */
  initial: Versioned<ArticleAdminDetail> | null
  initialNotice: EditorNotice | null
  onCreated?: (articleId: string) => void
}

/**
 * Markdown editor for one Article.
 *
 * Save and publication are separate actions. The form never reports success
 * before the server answers, and it keeps the typed text on every failure.
 */
export function ArticleEditor({ initial, initialNotice, onCreated }: ArticleEditorProps) {
  const editor = useArticleEditor(initial, initialNotice, onCreated)
  const { state, dirty } = editor
  const article = state.current?.value ?? null
  // The public URL must stay stable once readers may have seen it.
  const slugLocked = article !== null && article.publishedAt !== null
  const idPrefix = useId()

  function fieldProps(field: keyof ArticleFields) {
    const failure = state.failure
    return {
      id: `${idPrefix}-${field}`,
      value: state.fields[field],
      error: failure?.kind === 'field' && failure.field === field ? failure.message : null,
      onChange: (value: string) => editor.changeField(field, value),
    }
  }

  return (
    <form
      className="article-editor"
      noValidate
      onSubmit={(event) => {
        event.preventDefault()
        void editor.save()
      }}
    >
      <div className="article-editor-bar">
        <h1>{article === null ? 'Bài viết mới' : 'Sửa bài viết'}</h1>
        <PublicationBadge status={article?.status ?? null} />
        <SaveStatus pending={state.pending} dirty={dirty} saved={article !== null} />
        <div className="article-editor-actions">
          <button type="submit" className="admin-button admin-button-primary" disabled={state.pending !== null}>
            {state.pending === 'saving' ? 'Đang lưu…' : 'Lưu'}
          </button>
        </div>
      </div>
      <EditorMessages
        failure={state.failure}
        notice={state.notice}
        reloading={state.pending === 'reloading'}
        onReload={() => void editor.reload()}
      />
      <div className="article-editor-fields">
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
        <EditorField label="Nội dung Markdown" {...fieldProps('bodyMarkdown')} rows={24} monospace />
      </div>
    </form>
  )
}

interface EditorFieldProps {
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

function EditorField({ id, label, value, error, onChange, hint, readOnly = false, rows, monospace = false }: EditorFieldProps) {
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

const STATUS_LABELS: Record<PublicationStatus, string> = {
  DRAFT: 'Bản nháp',
  PUBLISHED: 'Đã xuất bản',
  UNPUBLISHED: 'Đã gỡ xuất bản',
}

function PublicationBadge({ status }: { status: PublicationStatus | null }) {
  const label = status === null ? 'Chưa lưu lần nào' : STATUS_LABELS[status]
  const variant = status === null ? 'new' : status.toLowerCase()
  return <span className={`admin-badge admin-badge-${variant}`}>Trạng thái: {label}</span>
}

function SaveStatus({ pending, dirty, saved }: { pending: PendingMutation | null; dirty: boolean; saved: boolean }) {
  let text = 'Đã lưu mọi thay đổi'
  if (pending === 'saving') {
    text = 'Đang lưu…'
  } else if (dirty) {
    text = 'Có thay đổi chưa lưu'
  } else if (!saved) {
    text = 'Chưa có nội dung'
  }
  return (
    <p className="article-save-status" aria-live="polite">
      {text}
    </p>
  )
}

const NOTICE_TEXT: Record<EditorNotice, string> = {
  created: 'Đã tạo bản nháp.',
  saved: 'Đã lưu.',
  reloaded: 'Đã tải bản mới nhất từ máy chủ.',
}

interface EditorMessagesProps {
  failure: EditorFailure | null
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

function failureText(failure: EditorFailure): string {
  switch (failure.kind) {
    case 'conflict':
      return 'Bài viết đã được thay đổi ở nơi khác sau khi bạn mở. Thay đổi của bạn chưa được lưu và vẫn còn trên màn hình.'
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
