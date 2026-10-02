import type { RouteObject } from 'react-router-dom'
import { AdaptiveShell } from '../components/layout/AdaptiveShell'
import { ProtectedRoute } from '../components/layout/ProtectedRoute'
import { LoginPage } from '../pages/auth/LoginPage'
import { RegisterPage } from '../pages/auth/RegisterPage'
import { ForgotPasswordPage } from '../pages/auth/ForgotPasswordPage'
import { ProfilePage } from '../pages/profile/ProfilePage'
import { NotificationCenterPage } from '../pages/notification/NotificationCenterPage'
import { ForbiddenPage } from '../pages/error/ForbiddenPage'
import { NotFoundPage } from '../pages/error/NotFoundPage'
import {
  MyRentalsPage,
  SupportPage,
  TaskBoardPage,
  FacilityOverviewPage,
  StaffingPage,
  OperationsPage,
  ActivityLogPage,
  EscalationsPage,
  BusinessOverviewPage,
  PolicyPage,
  ReportsPage,
  UserManagementPage,
  LoginHistoryPage,
  RentalDetailPage,
  TaskDetailPage,
  SupportTicketDetailPage,
  EscalationDetailPage,
  PolicyEditorPage,
} from '../pages/placeholders'
import { BrowseUnitsPage } from '../pages/unit/BrowseUnitsPage'
import { UnitDetailPage } from '../pages/unit/UnitDetailPage'

import { RootRedirect } from './RootRedirect'

export const routesConfig: RouteObject[] = [
  {
    path: '/',
    element: <RootRedirect />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
  {
    path: '/forgot-password',
    element: <ForgotPasswordPage />,
  },
  {
    path: '/403',
    element: <ForbiddenPage />,
  },
  {
    element: (
      <ProtectedRoute>
        <AdaptiveShell />
      </ProtectedRoute>
    ),
    children: [
      {
        path: '/profile',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER', 'STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR']}>
            <ProfilePage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/notifications',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER', 'STAFF', 'FACILITY_MANAGER', 'BUSINESS_OPS', 'SYSTEM_ADMINISTRATOR']}>
            <NotificationCenterPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/units',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER', 'FACILITY_MANAGER']}>
            <BrowseUnitsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/units/:code',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER']}>
            <UnitDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/rentals',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER']}>
            <MyRentalsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/rentals/:id',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER']}>
            <RentalDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/tasks',
        element: (
          <ProtectedRoute allowedRoles={['STAFF']}>
            <TaskBoardPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/tasks/:id',
        element: (
          <ProtectedRoute allowedRoles={['STAFF']}>
            <TaskDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/support',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER', 'STAFF']}>
            <SupportPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/support/:id',
        element: (
          <ProtectedRoute allowedRoles={['CUSTOMER', 'STAFF']}>
            <SupportTicketDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/overview',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <FacilityOverviewPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/staffing',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <StaffingPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/operations',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <OperationsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/activity-log',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <ActivityLogPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/escalations',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <EscalationsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/escalations/:id',
        element: (
          <ProtectedRoute allowedRoles={['FACILITY_MANAGER']}>
            <EscalationDetailPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/business-overview',
        element: (
          <ProtectedRoute allowedRoles={['BUSINESS_OPS']}>
            <BusinessOverviewPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/policy',
        element: (
          <ProtectedRoute allowedRoles={['BUSINESS_OPS']}>
            <PolicyPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/policy/:version',
        element: (
          <ProtectedRoute allowedRoles={['BUSINESS_OPS']}>
            <PolicyEditorPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/reports',
        element: (
          <ProtectedRoute allowedRoles={['BUSINESS_OPS']}>
            <ReportsPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/users',
        element: (
          <ProtectedRoute allowedRoles={['SYSTEM_ADMINISTRATOR']}>
            <UserManagementPage />
          </ProtectedRoute>
        ),
      },
      {
        path: '/login-history',
        element: (
          <ProtectedRoute allowedRoles={['SYSTEM_ADMINISTRATOR']}>
            <LoginHistoryPage />
          </ProtectedRoute>
        ),
      },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]
