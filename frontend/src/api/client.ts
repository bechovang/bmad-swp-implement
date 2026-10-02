import axios from 'axios'

export const AUTH_TOKEN_KEY = 'storagehub_token'
export const AUTH_USER_KEY = 'storagehub_user'

// Base of every FE API call. The Vite dev server proxies /api to the backend
// (vite.config.ts); the contract pins the API under /api/v1 (AD-1/AD-2).
export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// Request interceptor: inject JWT Bearer token
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(AUTH_TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Response interceptor: handle 401 Unauthorized and auto-dispatch toast notification
apiClient.interceptors.response.use(
  (response) => {
    if (
      typeof window !== 'undefined' &&
      response?.data &&
      typeof response.data === 'object' &&
      response.data.notification
    ) {
      window.dispatchEvent(
        new CustomEvent('storagehub:toast', { detail: response.data.notification })
      )
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || ''
      // Only clear session and redirect if this was not an auth endpoint
      if (!requestUrl.includes('/auth/')) {
        localStorage.removeItem(AUTH_TOKEN_KEY)
        localStorage.removeItem(AUTH_USER_KEY)
        if (typeof window !== 'undefined') {
          window.dispatchEvent(new CustomEvent('storagehub:auth-unauthorized'))
          if (window.location.pathname !== '/login') {
            const redirectParam = encodeURIComponent(window.location.pathname + window.location.search)
            window.location.href = `/login?redirect=${redirectParam}&expired=true`
          }
        }
      }
    }
    return Promise.reject(error)
  }
)
