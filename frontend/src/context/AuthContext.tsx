import React, { useCallback, useEffect, useState } from 'react'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import { loginApi, registerApi } from '../api/auth'
import type { AuthUser, LoginRequest, RegisterRequest } from '../types/auth'
import {
  AuthContext,
  getValidStoredSession,
  AutoLoginError,
  type AuthContextType,
} from './auth-context-base'
import { getRoleLanding } from '../constants/navigation'

export type { AuthContextType }

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [session, setSession] = useState(getValidStoredSession)
  const token = session.token
  const user = session.user

  const logout = useCallback(() => {
    localStorage.removeItem(AUTH_TOKEN_KEY)
    localStorage.removeItem(AUTH_USER_KEY)
    setSession({ token: null, user: null })
  }, [])

  // Listen for unauthorized 401 events
  useEffect(() => {
    const handleUnauthorized = () => {
      logout()
    }
    window.addEventListener('storagehub:auth-unauthorized', handleUnauthorized)
    return () => {
      window.removeEventListener('storagehub:auth-unauthorized', handleUnauthorized)
    }
  }, [logout])

  // Synchronize login/logout across multiple browser tabs
  useEffect(() => {
    const handleStorageChange = (e: StorageEvent) => {
      if (e.key === AUTH_TOKEN_KEY || e.key === AUTH_USER_KEY || e.key === null) {
        setSession(getValidStoredSession())
      }
    }
    window.addEventListener('storage', handleStorageChange)
    return () => {
      window.removeEventListener('storage', handleStorageChange)
    }
  }, [])

  const login = useCallback(async (data: LoginRequest): Promise<AuthUser> => {
    const res = await loginApi(data)
    localStorage.setItem(AUTH_TOKEN_KEY, res.token)
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(res.user))
    setSession({ token: res.token, user: res.user })
    return res.user
  }, [])

  const register = useCallback(
    async (data: RegisterRequest): Promise<AuthUser> => {
      await registerApi(data)
      // Auto-login upon successful customer registration
      try {
        const loginRes = await loginApi({
          email: data.email,
          password: data.password,
        })
        localStorage.setItem(AUTH_TOKEN_KEY, loginRes.token)
        localStorage.setItem(AUTH_USER_KEY, JSON.stringify(loginRes.user))
        setSession({ token: loginRes.token, user: loginRes.user })
        return loginRes.user
      } catch {
        throw new AutoLoginError('Account created successfully. Please sign in.')
      }
    },
    []
  )

  const value: AuthContextType = {
    user,
    token,
    isAuthenticated: Boolean(token && user),
    login,
    register,
    logout,
    getRoleLanding,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
