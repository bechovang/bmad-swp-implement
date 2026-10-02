import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, beforeAll, afterAll } from 'vitest'
import { server } from '../mocks/server'

// Start MSW server before tests run
beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }))

// vitest runs without `globals: true`, so RTL's auto-cleanup never registers;
// clean the DOM explicitly to keep renders isolated between tests.
afterEach(() => {
  cleanup()
  server.resetHandlers()
  localStorage.clear()
})

// Close MSW server after all tests
afterAll(() => server.close())
