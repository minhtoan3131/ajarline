import { BrowserRouter } from 'react-router'

import { HttpClient } from '../shared/api/httpClient'
import { HttpClientProvider } from '../shared/api/HttpClientContext'
import { AppRoutes } from './AppRoutes'

// One client for the whole tab, so the CSRF token is shared by all features.
const httpClient = new HttpClient()

export function App() {
  return (
    <HttpClientProvider client={httpClient}>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </HttpClientProvider>
  )
}
