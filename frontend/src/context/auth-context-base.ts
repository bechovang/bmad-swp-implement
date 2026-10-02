import { createContext } from 'react'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser, LoginRequest, RegisterRequest, Role } from '../types/auth'

export class AutoLoginError extends Error {
  constructor(message = 'Account created successfully. Please sign in.') {
    super(message)
    this.name = 'AutoLoginError'
  }
}

export interface AuthContextType {
  user: AuthUser | null
  token: string | null
  isAuthenticated: boolean
  login: (data: LoginRequest) => Promise<AuthUser>
  register: (data: RegisterRequest) => Promise<AuthUser>
  logout: () => void
  getRoleLanding: (role?: Role | null) => string
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function isJwtExpired(token: string): boolean {
  try {
    const parts = token.split('.')
    if (parts.length !== 3) return false
    const base64Url = parts[1]
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/')
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    )
    const payload = JSON.parse(jsonPayload)
    if (typeof payload.exp === 'number') {
      return Date.now() >= payload.exp * 1000
    }
    return false
  } catch {
    return false
  }
}

export function getStoredUser(): AuthUser | null {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY)
    if (!raw) return null
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function getStoredToken(): string | null {
  return localStorage.getItem(AUTH_TOKEN_KEY)
}

export function getValidStoredSession(): { token: string | null; user: AuthUser | null } {
  try {
    const token = localStorage.getItem(AUTH_TOKEN_KEY)
    const user = getStoredUser()
    if (!token || !user) {
      return { token: null, user: null }
    }
    if (isJwtExpired(token)) {
      localStorage.removeItem(AUTH_TOKEN_KEY)
      localStorage.removeItem(AUTH_USER_KEY)
      return { token: null, user: null }
    }
    return { token, user }
  } catch {
    return { token: null, user: null }
  }
}
