import { useParams } from 'react-router'

import { ModuleView } from '../features/learning-navigation/components/ModuleView'
import { usePublicModule } from '../features/learning-navigation/usePublicModule'
import { NotFoundPage } from './NotFoundPage'

export function ModulePage() {
  const { slug = '' } = useParams()
  const { state, retry } = usePublicModule(slug)

  switch (state.status) {
    case 'loading':
      return (
        <p className="reader-state" role="status">
          Đang tải Module…
        </p>
      )
    case 'not-found':
      return <NotFoundPage />
    case 'failed':
      return (
        <section className="reader-state" role="alert">
          <p>Không tải được Module do lỗi kết nối hoặc máy chủ.</p>
          <button type="button" className="reader-button" onClick={retry}>
            Thử lại
          </button>
        </section>
      )
    case 'found':
      return <ModuleView module={state.module} />
  }
}
