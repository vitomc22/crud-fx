export type Piece = {
  partNumber: string
  name: string
  description: string
  revision: string
}

export type UserSession = {
  email: string
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080').replace(/\/$/, '')

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')

  if (init.body !== undefined) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(`${apiBaseUrl}${path}`, {
    ...init,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    let message = `O backend respondeu com HTTP ${response.status}.`
    try {
      const body: unknown = await response.json()
      if (typeof body === 'object' && body !== null) {
        const details = body as { message?: unknown; detail?: unknown }
        if (typeof details.message === 'string') message = details.message
        else if (typeof details.detail === 'string') message = details.detail
      }
    } catch {
      // Keep the status-based message for empty/non-JSON responses.
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) return undefined as T
  const body = await response.text()
  if (!body) return undefined as T
  return JSON.parse(body) as T
}

export const api = {
  currentUser: () => request<UserSession>('/api/auth/me'),
  login: (email: string, password: string) =>
    request<UserSession>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  pieces: () => request<Piece[]>('/api/pieces'),
  createPiece: (piece: Piece) =>
    request<Piece>('/api/pieces', { method: 'POST', body: JSON.stringify(piece) }),
  updatePiece: (piece: Piece) =>
    request<Piece>(`/api/pieces/${encodeURIComponent(piece.partNumber)}`, {
      method: 'PUT',
      body: JSON.stringify(piece),
    }),
  deletePiece: (partNumber: string) =>
    request<void>(`/api/pieces/${encodeURIComponent(partNumber)}`, { method: 'DELETE' }),
}