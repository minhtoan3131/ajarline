import { useConceptList, type ConceptListLoad } from '../useConceptList'
import { useConceptManager } from '../useConceptManager'
import { ConceptForm } from './ConceptForm'
import { ConceptList } from './ConceptList'
import { ConceptMessages } from './ConceptMessages'

/** Concept Management screen: the retrieval vocabulary. */
export function ConceptManager() {
  const { load, reload } = useConceptList()
  const manager = useConceptManager(reload)
  const { state, dirty } = manager
  // Typed text is never dropped silently: while a form has unsaved changes or
  // an action is running, no other form can be opened. Cancel is the way out.
  const locked = dirty || state.pending !== null

  return (
    <section className="concept-manager">
      <div className="concept-manager-bar">
        <h1>Concept</h1>
        <button type="button" className="admin-button admin-button-primary" disabled={locked} onClick={manager.openCreate}>
          Tạo Concept
        </button>
      </div>
      <p className="admin-field-hint">
        Concept là từ vựng truy hồi, không phải Topic: mỗi Concept gom các cách nói của cùng một ý để Search hiểu chúng.
        Concept không có trang công khai.
      </p>
      {dirty && <p className="admin-field-hint">Lưu hoặc hủy thay đổi hiện tại trước khi mở Concept khác.</p>}
      <ConceptMessages failure={state.failure} notice={state.notice} />
      {state.panel.mode !== 'closed' && (
        <ConceptForm
          fields={state.fields}
          failure={state.failure}
          pending={state.pending}
          dirty={dirty}
          onChange={manager.changeFields}
          onAddExpression={manager.addExpression}
          onChangeExpression={manager.changeExpression}
          onRemoveExpression={manager.removeExpression}
          onSubmit={() => void manager.save()}
          onCancel={manager.close}
        />
      )}
      <ConceptListSection load={load} onRetry={reload} />
    </section>
  )
}

interface ConceptListSectionProps {
  load: ConceptListLoad
  onRetry: () => void
}

function ConceptListSection({ load, onRetry }: ConceptListSectionProps) {
  switch (load.status) {
    case 'loading':
      return (
        <p className="admin-status" role="status">
          Đang tải danh sách Concept…
        </p>
      )
    case 'failed':
      return (
        <div className="admin-notice admin-notice-error" role="alert">
          <p>Không tải được danh sách Concept do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="admin-button" onClick={onRetry}>
            Thử lại
          </button>
        </div>
      )
    case 'ready':
      return <ConceptList concepts={load.concepts} />
  }
}
