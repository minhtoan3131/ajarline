import { vi } from 'vitest'

export interface StubResponse {
  status: number
  body?: unknown
  headers?: Record<string, string>
}

export interface RecordedRequest {
  method: string
  path: string
  headers: Headers
  body: unknown
}

export function jsonResponse(status: number, body: unknown, headers: Record<string, string> = {}): StubResponse {
  return { status, body, headers }
}

export function problemResponse(
  status: number,
  code: string,
  options: { detail?: string; headers?: Record<string, string> } = {},
): StubResponse {
  return {
    status,
    body: { type: 'about:blank', title: code, status, detail: options.detail ?? code, code, requestId: 'test-request' },
    headers: { 'Content-Type': 'application/problem+json', ...options.headers },
  }
}

/**
 * Replaces the global fetch with scripted backend responses and records every
 * request, so tests can check the HTTP contract that the UI sends.
 */
export class StubApiServer {
  readonly requests: RecordedRequest[] = []
  readonly #routes = new Map<string, StubResponse[]>()

  /**
   * Queues responses for one method and path. Responses are used in order;
   * the last one is repeated for later calls.
   */
  on(method: string, path: string, ...responses: StubResponse[]): this {
    const key = `${method} ${path}`
    this.#routes.set(key, [...(this.#routes.get(key) ?? []), ...responses])
    return this
  }

  install(): this {
    vi.stubGlobal('fetch', (input: RequestInfo | URL, init?: RequestInit) => this.#handle(input, init))
    return this
  }

  requestsTo(method: string, path: string): RecordedRequest[] {
    return this.requests.filter((request) => request.method === method && request.path === path)
  }

  async #handle(input: RequestInfo | URL, init?: RequestInit): Promise<Response> {
    const method = init?.method ?? 'GET'
    const path = String(input)
    const body = typeof init?.body === 'string' ? JSON.parse(init.body) : undefined
    this.requests.push({ method, path, headers: new Headers(init?.headers), body })

    const queue = this.#routes.get(`${method} ${path}`)
    if (queue === undefined || queue.length === 0) {
      throw new Error(`Unexpected request in test: ${method} ${path}`)
    }
    const next = queue.length > 1 ? queue.shift()! : queue[0]
    return new Response(next.body === undefined ? null : JSON.stringify(next.body), {
      status: next.status,
      headers: { 'Content-Type': 'application/json', ...next.headers },
    })
  }
}
