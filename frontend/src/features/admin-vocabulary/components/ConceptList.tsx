import type { ConceptAdminItem } from '../model/concept'

interface ConceptListProps {
  concepts: ConceptAdminItem[]
}

/** Table of Concepts. An inactive Concept stays in the list, marked as turned off. */
export function ConceptList({ concepts }: ConceptListProps) {
  if (concepts.length === 0) {
    return <p className="admin-status">Chưa có Concept nào. Bấm “Tạo Concept” để thêm Concept đầu tiên.</p>
  }
  return (
    <table className="concept-table" aria-label="Danh sách Concept">
      <thead>
        <tr>
          <th scope="col">Key</th>
          <th scope="col">Tên</th>
          <th scope="col">Trạng thái</th>
          <th scope="col">Expression</th>
        </tr>
      </thead>
      <tbody>
        {concepts.map((concept) => (
          <tr key={concept.id}>
            <td>
              <code>{concept.key}</code>
            </td>
            <td>
              <strong>{concept.displayName}</strong>
            </td>
            <td>
              <ActiveBadge active={concept.active} />
            </td>
            <td>{concept.expressions.length}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function ActiveBadge({ active }: { active: boolean }) {
  return (
    <span className={`admin-badge ${active ? 'admin-badge-active' : 'admin-badge-inactive'}`}>
      {active ? 'Đang bật' : 'Đã tắt'}
    </span>
  )
}
