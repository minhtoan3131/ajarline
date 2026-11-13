import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'

import { ApiError } from '../../shared/api/httpClient'
import { useHttpClient } from '../../shared/api/HttpClientContext'
import { fetchCsrfToken, fetchSessionState, loginAdmin, logoutAdmin } from './api/authApi'

export type AdminSessionStatus = 'checking' | 'signed-out' | 'signed-in' | 'expired' | 'unavailable'

export interface AdminSession {
  readonly status: AdminSessionStatus
  signIn(username: string, password: string): Promise<void>
  signOut(): Promise<void>
  renewCsrfToken(): Promise<void>
  retry(): void
}

const AdminSessionContext = createContext<AdminSession | null>(null)

/**
 * Owns the Admin session state for the Admin route tree.
 *
 * The session itself is a server-side session behind an HttpOnly cookie.
 * The browser keeps only the session status and the current CSRF token in
 * memory, never in localStorage or sessionStorage.
 */
export function AdminSessionProvider({ children }: { children: ReactNode }) {
  const client = useHttpClient()
  const [status, setStatus] = useState<AdminSessionStatus>('checking')
  const [attempt, setAttempt] = useState(0)

  const renewCsrfToken = useCallback(async () => {
    client.setCsrfToken(await fetchCsrfToken(client))
  }, [client])

  useEffect(() => {
    let active = true
    setStatus('checking')
    async function bootstrap() {
      const authenticated = await fetchSessionState(client)
      // Admin mutations need a token bound to the current session.
      const token = await fetchCsrfToken(client)
      return { authenticated, token }
    }
    bootstrap().then(
      ({ authenticated, token }) => {
        if (active) {
          client.setCsrfToken(token)
          setStatus(authenticated ? 'signed-in' : 'signed-out')
        }
      },
      () => {
        if (active) {
          setStatus('unavailable')
        }
      },
    )
    return () => {
      active = false
    }
  }, [client, attempt])

  useEffect(
    () =>
      client.onUnauthorized(() => {
        // Keep the current screen so unsaved text stays visible.
        setStatus((current) => (current === 'signed-in' ? 'expired' : current))
      }),
    [client],
  )

  const signIn = useCallback(
    async (username: string, password: string) => {
      // A fresh token avoids a CSRF failure when an older session has ended.
      await renewCsrfToken()
      await loginAdmin(client, username, password)
      // Login rotates the session, which invalidates the previous token.
      await renewCsrfToken()
      setStatus('signed-in')
    },
    [client, renewCsrfToken],
  )

  const signOut = useCallback(async () => {
    try {
      await logoutAdmin(client)
    } catch (error) {
      // 401 means the server session is already gone, which is the goal.
      if (!(error instanceof ApiError && error.status === 401)) {
        throw error
      }
    }
    client.setCsrfToken(null)
    setStatus('signed-out')
  }, [client])

  const retry = useCallback(() => setAttempt((value) => value + 1), [])

  const session = useMemo<AdminSession>(
    () => ({ status, signIn, signOut, renewCsrfToken, retry }),
    [status, signIn, signOut, renewCsrfToken, retry],
  )
  return <AdminSessionContext value={session}>{children}</AdminSessionContext>
}

export function useAdminSession(): AdminSession {
  const session = useContext(AdminSessionContext)
  if (session === null) {
    throw new Error('AdminSessionProvider is missing above this component')
  }
  return session
}
