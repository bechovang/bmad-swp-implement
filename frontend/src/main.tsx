import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import { queryClient } from './lib/queryClient.ts'
import './index.css'

// MSW (2.x) runs only in dev and only when opted in via VITE_ENABLE_MSW=true,
// so mock-first development (AD-2) never leaks into a real build.
async function enableMocking(): Promise<void> {
  if (!import.meta.env.DEV || import.meta.env.VITE_ENABLE_MSW !== 'true') return
  const { worker } = await import('./mocks/browser.ts')
  await worker.start({ onUnhandledRequest: 'bypass' })
}

void enableMocking()
  .catch((error) => {
    // A failing MSW startup must never leave a blank page: log it, render anyway.
    console.error('MSW worker failed to start', error)
  })
  .then(() => {
  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </StrictMode>,
  )
})
