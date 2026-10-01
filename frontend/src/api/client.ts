import axios from 'axios'

// Base of every FE API call. The Vite dev server proxies /api to the backend
// (vite.config.ts); the contract pins the API under /api/v1 (AD-1/AD-2).
// Request/response types are generated from contracts/openapi.yaml per story.
export const apiClient = axios.create({
  baseURL: '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})
