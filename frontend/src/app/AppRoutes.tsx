import { Route, Routes } from 'react-router'

import { ArticlePage } from '../pages/ArticlePage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { RuntimeStatusPage } from '../pages/RuntimeStatusPage'
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
    </Routes>
  )
}
