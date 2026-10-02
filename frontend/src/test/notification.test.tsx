import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route, createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { BellButton } from '../components/notification/BellButton'
import { NotificationList } from '../components/notification/NotificationCenter'
import { NotificationCenterPage } from '../pages/notification/NotificationCenterPage'
import { resetMockNotifications } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import {
  getUnreadCount,
  getNotifications,
  markAsRead,
  markAllAsRead,
} from '../api/notification'

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
        gcTime: 0,
      },
    },
  })
}

function setTestSession(userId = 1, role = 'CUSTOMER') {
  localStorage.setItem(AUTH_TOKEN_KEY, `mock-jwt-token-for-${role.toLowerCase()}-${userId}`)
  localStorage.setItem(
    AUTH_USER_KEY,
    JSON.stringify({
      id: userId,
      fullName: 'Test User',
      email: 'test@storagehub.dev',
      role,
    })
  )
}

describe('Notification API and Components', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockNotifications()
  })

  describe('Notification API Client', () => {
    it('returns unread count for authenticated user', async () => {
      setTestSession(1)
      const data = await getUnreadCount()
      // Initial user 1 has 1 unread notification (id 1) and 1 read (id 2)
      expect(data.count).toBe(1)
    })

    it('returns paginated notifications ordered unread first', async () => {
      setTestSession(1)
      const response = await getNotifications(1, 10)
      expect(response.total).toBe(2)
      expect(response.items.length).toBe(2)
      // Unread item should be first
      expect(response.items[0].isRead).toBe(false)
      expect(response.items[1].isRead).toBe(true)
    })

    it('marks a single notification as read', async () => {
      setTestSession(1)
      const updated = await markAsRead(1)
      expect(updated.id).toBe(1)
      expect(updated.isRead).toBe(true)

      const unread = await getUnreadCount()
      expect(unread.count).toBe(0)
    })

    it('marks all notifications as read', async () => {
      setTestSession(1)
      const res = await markAllAsRead()
      expect(res.count).toBe(0)

      const unread = await getUnreadCount()
      expect(unread.count).toBe(0)
    })

    it('returns 401 when calling notification endpoints without authentication', async () => {
      // No session in localStorage
      await expect(getUnreadCount()).rejects.toThrow()
    })
  })

  describe('BellButton Component', () => {
    it('renders bell icon and error-red badge when unread count > 0', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <BellButton refetchInterval={0} />
          </MemoryRouter>
        </QueryClientProvider>
      )

      expect(screen.getByRole('button', { name: /notifications/i })).toBeInTheDocument()

      const badge = await screen.findByTestId('bell-badge')
      expect(badge).toBeInTheDocument()
      expect(badge).toHaveTextContent('1')
      expect(badge).toHaveClass('bg-sh-error')
    })

    it('hides badge when unread count is 0', async () => {
      // User with 0 unread notifications
      setTestSession(1)
      resetMockNotifications([
        {
          id: 10,
          userId: 1,
          type: 'INFO',
          title: 'All caught up',
          isRead: true,
          createdAt: '2026-10-02T10:00:00Z',
        },
      ])

      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <BellButton refetchInterval={0} />
          </MemoryRouter>
        </QueryClientProvider>
      )

      expect(screen.getByRole('button', { name: /notifications/i })).toBeInTheDocument()

      await waitFor(() => {
        expect(screen.queryByTestId('bell-badge')).not.toBeInTheDocument()
      })
    })

    it('opens NotificationDrawer when clicked', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <BellButton refetchInterval={0} />
          </MemoryRouter>
        </QueryClientProvider>
      )

      const bell = screen.getByRole('button', { name: /notifications/i })
      fireEvent.click(bell)

      expect(await screen.findByRole('dialog')).toBeInTheDocument()
      expect(screen.getByText('Notifications')).toBeInTheDocument()
    })
  })

  describe('NotificationList & Drawer Interactions', () => {
    it('switches between All and Unread filter tabs', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <NotificationList />
          </MemoryRouter>
        </QueryClientProvider>
      )

      // Both notifications show under All tab
      expect(await screen.findByText('Deposit received for Unit S-04')).toBeInTheDocument()
      expect(screen.getByText('Reservation confirmed for Unit M-12')).toBeInTheDocument()

      // Switch to Unread tab
      const unreadTab = screen.getByRole('tab', { name: /unread/i })
      fireEvent.click(unreadTab)

      expect(screen.getByText('Deposit received for Unit S-04')).toBeInTheDocument()
      expect(screen.queryByText('Reservation confirmed for Unit M-12')).not.toBeInTheDocument()
    })

    it('displays EmptyState when there are no notifications', async () => {
      setTestSession(1)
      resetMockNotifications([])
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <NotificationList />
          </MemoryRouter>
        </QueryClientProvider>
      )

      expect(await screen.findByText('No notifications yet')).toBeInTheDocument()
    })

    it('navigates to deepLink and marks as read when clicking a notification item', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter initialEntries={['/']}>
            <Routes>
              <Route path="/" element={<NotificationList />} />
              <Route path="/rentals/1" element={<div>Rental Detail Page 1</div>} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      )

      const item = await screen.findByTestId('notification-item-1')
      fireEvent.click(item)

      expect(await screen.findByText('Rental Detail Page 1')).toBeInTheDocument()
      await waitFor(async () => {
        const unread = await getUnreadCount()
        expect(unread.count).toBe(0)
      })
    })

    it('marks all as read when clicking Mark all as read button', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <NotificationList />
          </MemoryRouter>
        </QueryClientProvider>
      )

      const markAllBtn = await screen.findByRole('button', { name: /mark all as read/i })
      await waitFor(() => {
        expect(markAllBtn).not.toBeDisabled()
      })

      fireEvent.click(markAllBtn)

      await waitFor(() => {
        expect(markAllBtn).toBeDisabled()
      })
    })
  })

  describe('NotificationCenterPage Component', () => {
    it('renders the standalone Notification Center page layout', async () => {
      setTestSession(1)
      const testQc = createTestQueryClient()

      render(
        <QueryClientProvider client={testQc}>
          <MemoryRouter>
            <NotificationCenterPage />
          </MemoryRouter>
        </QueryClientProvider>
      )

      expect(screen.getByRole('heading', { level: 1, name: 'Notification Center' })).toBeInTheDocument()
      expect(await screen.findByText('Deposit received for Unit S-04')).toBeInTheDocument()
    })

    it('resolves /notifications route under routesConfig for authenticated user', async () => {
      setTestSession(1, 'CUSTOMER')
      const testQc = createTestQueryClient()
      const router = createMemoryRouter(routesConfig, { initialEntries: ['/notifications'] })

      render(
        <QueryClientProvider client={testQc}>
          <AuthProvider>
            <RouterProvider router={router} />
          </AuthProvider>
        </QueryClientProvider>
      )

      expect(await screen.findByRole('heading', { level: 1, name: 'Notification Center' })).toBeInTheDocument()
    })
  })
})
