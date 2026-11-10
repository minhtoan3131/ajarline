import { Route, Routes } from 'react-router'

import { AdminLoginPage } from '../pages/admin/AdminLoginPage'
import { AdminNotFoundPage } from '../pages/admin/AdminNotFoundPage'
import { ArticlePage } from '../pages/ArticlePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RuntimeStatusPage } from '../pages/RuntimeStatusPage'
import { AdminLayout } from './AdminLayout'
import { ReaderLayout } from './ReaderLayout'

/** The single route tree. The app and the tests both render it. */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<ReaderLayout />}>
        <Route index element={<RuntimeStatusPage />} />
        <Route path="articles/:slug" element={<ArticlePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="admin" element={<AdminLayout />}>
        <Route path="login" element={<AdminLoginPage />} />
        <Route path="*" element={<AdminNotFoundPage />} />
      </Route>
    </Routes>
  )
}
