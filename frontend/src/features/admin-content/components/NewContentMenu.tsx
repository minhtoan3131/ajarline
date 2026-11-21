import { useState } from 'react'
import { Link } from 'react-router'

import { newPathOf } from '../contentRoutes'
import { ADMIN_CONTENT_TYPES, CONTENT_TYPE_LABELS } from '../model/contentIndex'

/** Button that opens the list of types the Admin can create. */
export function NewContentMenu() {
  const [open, setOpen] = useState(false)

  return (
    <div className="content-new-menu">
      <button
        type="button"
        className="admin-button admin-button-primary"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
      >
        Tạo mới
      </button>
      {open && (
        <ul className="content-new-menu-list" aria-label="Chọn loại nội dung">
          {ADMIN_CONTENT_TYPES.map((type) => (
            <li key={type}>
              <Link to={newPathOf(type)}>{CONTENT_TYPE_LABELS[type]}</Link>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
