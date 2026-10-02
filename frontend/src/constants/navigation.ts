import type { Role } from '../types/auth'
import { APP_ROLES, type AppRole } from '../tokens'

export interface NavItem {
  id: string
  path: string
  label: string
}

export const ROLE_NAV_ITEMS: Record<Role, NavItem[]> = {
  CUSTOMER: [
    { id: 'units', path: '/units', label: 'Browse Units' },
    { id: 'rentals', path: '/rentals', label: 'My Rentals' },
    { id: 'support', path: '/support', label: 'Support' },
  ],
  STAFF: [
    { id: 'tasks', path: '/tasks', label: 'Task Board' },
    { id: 'support', path: '/support', label: 'Support' },
  ],
  FACILITY_MANAGER: [
    { id: 'overview', path: '/overview', label: 'Facility Overview' },
    { id: 'units', path: '/units', label: 'Units' },
    { id: 'staffing', path: '/staffing', label: 'Staff & Shifts' },
    { id: 'operations', path: '/operations', label: 'Operations' },
    { id: 'activity', path: '/activity-log', label: 'Activity Log' },
    { id: 'escalations', path: '/escalations', label: 'Escalations' },
  ],
  BUSINESS_OPS: [
    { id: 'business-overview', path: '/business-overview', label: 'Business Overview' },
    { id: 'policy', path: '/policy', label: 'Policy' },
    { id: 'reports', path: '/reports', label: 'Reports' },
  ],
  SYSTEM_ADMINISTRATOR: [
    { id: 'users', path: '/users', label: 'User Management' },
    { id: 'login-history', path: '/login-history', label: 'Login History' },
  ],
}

export function getRoleLanding(role?: Role | null): string {
  if (!role) return '/units'
  const config = APP_ROLES[role as AppRole]
  return config ? config.landing : '/units'
}
