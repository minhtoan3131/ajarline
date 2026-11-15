/** Problem Details body returned by the backend for failed requests. */
export interface ApiProblem {
  type?: string
  title?: string
  status?: number
  detail?: string
  code?: string
  requestId?: string
}

/** CSRF proof that the server issued for the current session. */
export interface CsrfToken {
  headerName: string
  token: string
}

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

export interface RequestOptions {
  method?: HttpMethod
  /** JSON body. Leave undefined to send no body at all. */
  body?: unknown
  /** Exact ETag of the version being changed. Sent as If-Match. */
  ifMatch?: string
  signal?: AbortSignal
}

export interface ApiResponse<T> {
  status: number
  body: T
  etag: string | null
}

/**
 * Failed HTTP exchange. `status` is 0 when no HTTP response was received.
 * Screens branch on `status` and `code`, never on the raw response body.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string | null
  readonly detail: string | null
  readonly requestId: string | null
  readonly retryAfterSeconds: number | null

  constructor(status: number, problem: ApiProblem | null, retryAfterSeconds: number | null) {
    super(`HTTP request failed with status ${status}`)
    this.name = 'ApiError'
    this.status = status
    this.code = typeof problem?.code === 'string' ? problem.code : null
    this.detail = typeof problem?.detail === 'string' ? problem.detail : null
    this.requestId = typeof problem?.requestId === 'string' ? problem.requestId : null
    this.retryAfterSeconds = retryAfterSeconds
  }
}

/** True when a request was cancelled on purpose, for example on unmount. */
export function isAbortError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError'
}

/**
 * Base HTTP client for the same-origin backend API.
 *
 * It owns transport rules shared by every feature: cookies, JSON, Problem
 * Details, the CSRF header on unsafe methods, If-Match and the ETag of the
 * response. It knows no endpoint. The CSRF token lives only in memory.
 */
export class HttpClient {
  #csrfToken: CsrfToken | null = null
  readonly #unauthorizedListeners = new Set<() => void>()

  setCsrfToken(token: CsrfToken | null): void {
    this.#csrfToken = token
  }

  /**
   * Registers a callback for requests rejected because the Admin session is
   * missing. Returns a function that removes the callback.
   */
  onUnauthorized(listener: () => void): () => void {
    this.#unauthorizedListeners.add(listener)
    return () => {
      this.#unauthorizedListeners.delete(listener)
    }
  }

  /**
   * Sends one request and returns the parsed body with the response ETag.
   *
   * @throws ApiError when the server answers with a non-2xx status or when
   *         no response arrives
   */
  async request<T>(path: string, options: RequestOptions = {}): Promise<ApiResponse<T>> {
    const method = options.method ?? 'GET'
    const headers = new Headers({ Accept: 'application/json' })
    let body: string | undefined
    if (options.body !== undefined) {
      headers.set('Content-Type', 'application/json')
      body = JSON.stringify(options.body)
    }
    if (method !== 'GET' && this.#csrfToken !== null) {
      headers.set(this.#csrfToken.headerName, this.#csrfToken.token)
    }
    if (options.ifMatch !== undefined) {
      headers.set('If-Match', options.ifMatch)
    }

    let response: Response
    try {
      response = await fetch(path, {
        method,
        headers,
        body,
        // The Admin session is a same-origin HttpOnly cookie. Stated
        // explicitly because the API must never be called cross-origin.
        credentials: 'same-origin',
        signal: options.signal,
      })
    } catch (error) {
      if (isAbortError(error)) {
        throw error
      }
      throw new ApiError(0, null, null)
    }

    if (!response.ok) {
      const failure = new ApiError(response.status, await readProblem(response), readRetryAfter(response))
      // Only a missing Admin session counts. Wrong credentials also use 401
      // but have another code and must not end the current screen.
      if (failure.status === 401 && failure.code === 'AUTHENTICATION_REQUIRED') {
        this.#unauthorizedListeners.forEach((listener) => listener())
      }
      throw failure
    }

    const parsed = response.status === 204 ? undefined : await response.json()
    return { status: response.status, body: parsed as T, etag: response.headers.get('ETag') }
  }
}

async function readProblem(response: Response): Promise<ApiProblem | null> {
  try {
    return (await response.json()) as ApiProblem
  } catch {
    // Some failures (for example from a proxy) have no JSON body.
    return null
  }
}

function readRetryAfter(response: Response): number | null {
  const value = response.headers.get('Retry-After')
  const seconds = value === null ? Number.NaN : Number(value)
  return Number.isInteger(seconds) && seconds >= 0 ? seconds : null
}
