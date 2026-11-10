import { createContext, useContext, type ReactNode } from 'react'

import type { HttpClient } from './httpClient'

const HttpClientContext = createContext<HttpClient | null>(null)

/** Provides the one HTTP client used by every feature below it. */
export function HttpClientProvider({ client, children }: { client: HttpClient; children: ReactNode }) {
  return <HttpClientContext value={client}>{children}</HttpClientContext>
}

export function useHttpClient(): HttpClient {
  const client = useContext(HttpClientContext)
  if (client === null) {
    throw new Error('HttpClientProvider is missing above this component')
  }
  return client
}
