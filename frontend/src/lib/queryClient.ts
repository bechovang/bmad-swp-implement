import { QueryClient } from '@tanstack/react-query'

// The one TanStack Query client for server-state (ARCHITECTURE-SPINE
// "Consistency Conventions": server-state via TanStack Query, local UI-state
// stays in components).
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,
    },
  },
})
