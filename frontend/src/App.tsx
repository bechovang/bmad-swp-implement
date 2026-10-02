import { RouterProvider, createBrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { routesConfig } from './router/routes'

interface AppProps {
  router?: ReturnType<typeof createBrowserRouter>
}

const defaultRouter = createBrowserRouter(routesConfig)

export function App({ router }: AppProps) {
  const appRouter = router || defaultRouter
  return (
    <AuthProvider>
      <RouterProvider router={appRouter} />
    </AuthProvider>
  )
}

export default App
