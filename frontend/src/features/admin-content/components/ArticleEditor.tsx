import { useDeferredValue, useId, useState } from 'react'
import { Link } from 'react-router'

import { ArticlePreview } from './ArticlePreview'
import { EditorField } from './EditorField'
import { FullPreviewOverlay } from './FullPreviewOverlay'
import { PublicationBadge, PublicationControls, PublicationHint } from './PublicationControls'
import { SaveStatus } from './SaveStatus'
import { TopicPicker } from './TopicPicker'
import { articlePath } from '../../article-reading/articleRoutes'
import { useTopicList } from '../../admin-topics/useTopicList'
import type { ArticleAdminDetail, ArticleTextField, Versioned } from '../model/article'
import type { EditorNotice } from '../model/articleEditorState'
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
  const topicList = useTopicList()
  const topicError =
    state.failure?.kind === 'field' && state.failure.field === 'topicIds' ? state.failure.message : null
  const article = state.current?.value ?? null
  // The public URL must stay stable once readers may have seen it.
  const slugLocked = article !== null && article.publishedAt !== null
  const idPrefix = useId()
  const [pane, setPane] = useState<'write' | 'preview'>('write')
  const [previewOpen, setPreviewOpen] = useState(false)
  // Typing stays responsive; the preview catches up when React is idle.
  const previewFields = useDeferredValue(state.fields)
  const publishHintId = `${idPrefix}-publish-hint`
  const published = article !== null && article.status === 'PUBLISHED'

  function fieldProps(field: ArticleTextField) {
    const failure = state.failure
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
          <h1>{article === null ? 'Bài viết mới' : 'Sửa bài viết'}</h1>
          <PublicationBadge status={article?.status ?? null} />
          <SaveStatus pending={state.pending} dirty={dirty} saved={article !== null} />
          <div className="article-editor-actions">
            <button type="submit" className="admin-button admin-button-primary" disabled={state.pending !== null}>
              {state.pending === 'saving' ? 'Đang lưu…' : 'Lưu'}
            </button>
            <button type="button" className="admin-button" onClick={() => setPreviewOpen(true)}>
              Xem trước toàn màn hình
            </button>
            <PublicationControls
              status={article?.status ?? null}
              dirty={dirty}
              pending={state.pending}
              hintId={publishHintId}
              onPublish={() => void editor.publish()}
              onUnpublish={() => void editor.unpublish()}
            />
            {published && (
              <Link className="article-public-link" to={articlePath(article.slug)}>
                Mở trang công khai
              </Link>
            )}
          </div>
        </div>
        <PublicationHint id={publishHintId} status={article?.status ?? null} dirty={dirty} />
        <EditorMessages
          failure={state.failure}
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
            <ArticlePreview fields={previewFields} />
          </section>
        </div>
      </form>
      {previewOpen && <FullPreviewOverlay fields={state.fields} onClose={() => setPreviewOpen(false)} />}
    </>
  )
}

const NOTICE_TEXT: Record<EditorNotice, string> = {
  created: 'Đã tạo bản nháp.',
  saved: 'Đã lưu.',
  reloaded: 'Đã tải bản mới nhất từ máy chủ.',
  published: 'Đã xuất bản. Bài đã hiển thị công khai.',
  unpublished: 'Đã gỡ xuất bản. Bài không còn hiển thị công khai.',
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
