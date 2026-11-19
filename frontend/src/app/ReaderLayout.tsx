import { Link, NavLink, Outlet } from 'react-router'

/**
 * Reader frame. The navigation links only to destinations that exist and
 * never to the Admin area.
 */
export function ReaderLayout() {
  return (
    <div className="reader-scope">
      <header className="reader-header">
        <Link to="/" className="reader-brand">
          Ajarline
        </Link>
        <nav className="reader-nav" aria-label="Điều hướng chính">
          {/* "end": without it "/" would match every Reader URL. */}
          <NavLink to="/" end className="reader-nav-link">
            Trang chủ
          </NavLink>
          <NavLink to="/topics" className="reader-nav-link">
            Chủ đề
          </NavLink>
          <NavLink to="/series" className="reader-nav-link">
            Series
          </NavLink>
        </nav>
      </header>
      <main className="reader-main">
        <Outlet />
      </main>
    </div>
  )
}
