import { Route, Routes } from 'react-router'

import { RequireAdminSession } from '../features/admin-auth/components/RequireAdminSession'
import { AdminLoginPage } from '../pages/admin/AdminLoginPage'
import { AdminNotFoundPage } from '../pages/admin/AdminNotFoundPage'
import { ArticleEditorPage } from '../pages/admin/ArticleEditorPage'
import { TopicManagementPage } from '../pages/admin/TopicManagementPage'
import { ArticlePage } from '../pages/ArticlePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RuntimeStatusPage } from '../pages/RuntimeStatusPage'
import { StandaloneIndexPage } from '../pages/StandaloneIndexPage'
import { TopicHomePage } from '../pages/TopicHomePage'
import { TopicIndexPage } from '../pages/TopicIndexPage'
import { AdminLayout } from './AdminLayout'
import { ReaderLayout } from './ReaderLayout'

/** The single route tree. The app and the tests both render it. */
export function AppRoutes() {
  return (
    <Routes>
      <Route element={<ReaderLayout />}>
        <Route index element={<RuntimeStatusPage />} />
        <Route path="articles" element={<StandaloneIndexPage />} />
        <Route path="articles/:slug" element={<ArticlePage />} />
        <Route path="topics" element={<TopicIndexPage />} />
        <Route path="topics/:slug" element={<TopicHomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="admin" element={<AdminLayout />}>
        <Route path="login" element={<AdminLoginPage />} />
        <Route element={<RequireAdminSession />}>
          <Route path="articles/new" element={<ArticleEditorPage />} />
          <Route path="articles/:articleId" element={<ArticleEditorPage />} />
          <Route path="topics" element={<TopicManagementPage />} />
        </Route>
        <Route path="*" element={<AdminNotFoundPage />} />
      </Route>
    </Routes>
  )
}
