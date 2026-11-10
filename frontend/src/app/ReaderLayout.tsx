import { Link, Outlet } from 'react-router'

/** Reader frame. It has no link to the Admin area by design. */
export function ReaderLayout() {
  return (
    <div className="reader-scope">
      <header className="reader-header">
        <Link to="/" className="reader-brand">
          Ajarline
        </Link>
      </header>
      <main className="reader-main">
        <Outlet />
      </main>
    </div>
  )
}
