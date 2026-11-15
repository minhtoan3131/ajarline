import type { TopicAdminItem } from '../model/topic'

/** Table of Topics. An inactive Topic stays in the list, marked as turned off. */
export function TopicList({ topics }: { topics: TopicAdminItem[] }) {
  if (topics.length === 0) {
    return <p className="admin-status">Chưa có Topic nào.</p>
  }
  return (
    <table className="topic-table" aria-label="Danh sách Topic">
      <thead>
        <tr>
          <th scope="col">Tên</th>
          <th scope="col">Slug</th>
          <th scope="col">Trạng thái</th>
        </tr>
      </thead>
      <tbody>
        {topics.map((topic) => (
          <tr key={topic.id}>
            <td>
              <strong>{topic.name}</strong>
              <p className="topic-description">{topic.description ?? 'Không có mô tả.'}</p>
            </td>
            <td>
              <code>{topic.slug}</code>
            </td>
            <td>
              <ActiveBadge active={topic.active} />
            </td>
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
