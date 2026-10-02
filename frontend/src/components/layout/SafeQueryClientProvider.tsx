import { useContext } from 'react'
import type { ReactNode } from 'react'
import { QueryClientContext, QueryClientProvider } from '@tanstack/react-query'
import { queryClient as defaultQueryClient } from '../../lib/queryClient'

export function SafeQueryClientProvider({ children }: { children: ReactNode }) {
  const existingClient = useContext(QueryClientContext)
  if (existingClient) {
    return <>{children}</>
  }
  return <QueryClientProvider client={defaultQueryClient}>{children}</QueryClientProvider>
}

export default SafeQueryClientProvider
