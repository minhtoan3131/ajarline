import { Navigate, Route, Routes } from 'react-router'

import { RequireAdminSession } from '../features/admin-auth/components/RequireAdminSession'
import { CONTENT_INDEX_PATH, contentIndexPath } from '../features/admin-content/contentRoutes'
import { NO_FILTER } from '../features/admin-content/model/contentIndexQuery'
import { AdminLoginPage } from '../pages/admin/AdminLoginPage'
import { AdminNotFoundPage } from '../pages/admin/AdminNotFoundPage'
import { ArticleEditorPage } from '../pages/admin/ArticleEditorPage'
import { ContentIndexPage } from '../pages/admin/ContentIndexPage'
import { ModuleEditorPage } from '../pages/admin/ModuleEditorPage'
import { NewsEditorPage } from '../pages/admin/NewsEditorPage'
import { SeriesEditorPage } from '../pages/admin/SeriesEditorPage'
import { TopicManagementPage } from '../pages/admin/TopicManagementPage'
import { AboutPage } from '../pages/AboutPage'
import { ArticlePage } from '../pages/ArticlePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { HomePage } from '../pages/HomePage'
import { ModulePage } from '../pages/ModulePage'
import { NewsIndexPage } from '../pages/NewsIndexPage'
import { NewsPage } from '../pages/NewsPage'
import { SeriesIndexPage } from '../pages/SeriesIndexPage'
import { SeriesPage } from '../pages/SeriesPage'
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
        <Route index element={<HomePage />} />
        <Route path="about" element={<AboutPage />} />
        <Route path="articles" element={<StandaloneIndexPage />} />
        <Route path="articles/:slug" element={<ArticlePage />} />
        <Route path="modules/:slug" element={<ModulePage />} />
        <Route path="news" element={<NewsIndexPage />} />
        <Route path="news/:slug" element={<NewsPage />} />
        <Route path="series" element={<SeriesIndexPage />} />
        <Route path="series/:slug" element={<SeriesPage />} />
        <Route path="topics" element={<TopicIndexPage />} />
        <Route path="topics/:slug" element={<TopicHomePage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
      <Route path="admin" element={<AdminLayout />}>
        <Route path="login" element={<AdminLoginPage />} />
        <Route element={<RequireAdminSession />}>
          <Route index element={<Navigate to={CONTENT_INDEX_PATH} replace />} />
          <Route path="content" element={<ContentIndexPage />} />
          <Route path="articles/new" element={<ArticleEditorPage />} />
          <Route path="articles/:articleId" element={<ArticleEditorPage />} />
          <Route path="series" element={<Navigate to={contentIndexPath({ ...NO_FILTER, types: ['series'] })} replace />} />
          <Route path="series/new" element={<SeriesEditorPage />} />
          <Route path="series/:seriesId" element={<SeriesEditorPage />} />
          <Route path="news" element={<Navigate to={contentIndexPath({ ...NO_FILTER, types: ['news'] })} replace />} />
          <Route path="news/new" element={<NewsEditorPage />} />
          <Route path="news/:newsId" element={<NewsEditorPage />} />
          <Route path="modules/new" element={<ModuleEditorPage />} />
          <Route path="modules/:moduleId" element={<ModuleEditorPage />} />
          <Route path="topics" element={<TopicManagementPage />} />
        </Route>
        <Route path="*" element={<AdminNotFoundPage />} />
      </Route>
    </Routes>
  )
}
