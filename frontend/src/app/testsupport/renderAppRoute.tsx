import { render } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router'

import { HttpClient } from '../../shared/api/httpClient'
import { HttpClientProvider } from '../../shared/api/HttpClientContext'
import { AppRoutes } from '../AppRoutes'

function LocationProbe() {
  const location = useLocation()
  // Not <output>: it has the implicit role "status", which would clash with
  // the real status messages that tests query by role.
  return <span data-testid="location">{location.pathname}</span>
}

/**
 * Renders the real route tree at one URL with a fresh HTTP client.
 * The probe shows the current path so tests can check navigation.
 */
export function renderAppRoute(path: string) {
  return render(
    <HttpClientProvider client={new HttpClient()}>
      <MemoryRouter initialEntries={[path]}>
        <AppRoutes />
        <LocationProbe />
      </MemoryRouter>
    </HttpClientProvider>,
  )
}
