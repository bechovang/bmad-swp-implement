import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'

export function RootRedirect() {
  const { user, isAuthenticated, getRoleLanding } = useAuth()
  if (isAuthenticated && user) {
    return <Navigate to={getRoleLanding(user.role)} replace />
  }
  return <Navigate to="/login" replace />
}
