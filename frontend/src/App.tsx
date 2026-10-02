import { RouterProvider, createBrowserRouter } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ToastProvider } from './context/ToastContext'
import { routesConfig } from './router/routes'

interface AppProps {
  router?: ReturnType<typeof createBrowserRouter>
}

const defaultRouter = createBrowserRouter(routesConfig)

export function App({ router }: AppProps) {
  const appRouter = router || defaultRouter
  return (
    <AuthProvider>
      <ToastProvider>
        <RouterProvider router={appRouter} />
      </ToastProvider>
    </AuthProvider>
  )
}

export default App
