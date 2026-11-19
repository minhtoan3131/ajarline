import { render } from '@testing-library/react'
import { MemoryRouter, useLocation, useNavigate } from 'react-router'

import { HttpClient } from '../../shared/api/httpClient'
import { HttpClientProvider } from '../../shared/api/HttpClientContext'
import { AppRoutes } from '../AppRoutes'

function LocationProbe() {
  const location = useLocation()
  // Not <output>: it has the implicit role "status", which would clash with
  // the real status messages that tests query by role.
  return (
    <>
      <span data-testid="location">{location.pathname}</span>
      <span data-testid="search">{location.search}</span>
    </>
  )
}

/** Goes one step back in the history, like the browser Back button. */
function HistoryBack() {
  const navigate = useNavigate()
  return (
    <button type="button" onClick={() => void navigate(-1)}>
      Back (test)
    </button>
  )
}

/**
 * Renders the real route tree at one URL with a fresh HTTP client.
 * The probes show the current path and query so tests can check navigation.
 * With `previous`, that URL is the history entry before `path`, and a Back
 * button is added, so tests can check that a redirect left no extra entry.
 */
export function renderAppRoute(path: string, options: { previous?: string } = {}) {
  const entries = options.previous === undefined ? [path] : [options.previous, path]
  return render(
    <HttpClientProvider client={new HttpClient()}>
      <MemoryRouter initialEntries={entries} initialIndex={entries.length - 1}>
        <AppRoutes />
        <LocationProbe />
        {options.previous !== undefined && <HistoryBack />}
      </MemoryRouter>
    </HttpClientProvider>,
  )
}
