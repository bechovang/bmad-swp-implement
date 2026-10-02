import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser } from '../types/auth'

function setSession(user: AuthUser, token = 'mock-jwt-token-123') {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function renderWithRouter(initialEntries: string[] = ['/']) {
  const router = createMemoryRouter(routesConfig, { initialEntries })
  const result = render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
  return { ...result, router }
}

describe('Adaptive Shell & 5-Role Navigation', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('renders Customer top bar with correct nav links and RoleChip "Customer"', async () => {
    setSession({
      id: 1,
      fullName: 'Lan Nguyen',
      email: 'lan@storagehub.dev',
      role: 'CUSTOMER',
    })

    const { router } = renderWithRouter(['/units'])

    expect(await screen.findByRole('heading', { level: 1, name: 'Browse Units' })).toBeInTheDocument()

    // TopBar links for CUSTOMER
    expect(screen.getByRole('link', { name: 'Browse Units' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'My Rentals' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Support' })).toBeInTheDocument()

    // Ensure staff/admin navs are NOT rendered
    expect(screen.queryByRole('link', { name: 'Task Board' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'User Management' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Facility Overview' })).not.toBeInTheDocument()

    // RoleChip
    const roleChip = screen.getByTestId('role-chip')
    expect(roleChip).toHaveTextContent('Customer')
    expect(router.state.location.pathname).toBe('/units')
  })

  it('renders Staff top bar with Tasks, Support and RoleChip "Staff"', async () => {
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })

    renderWithRouter(['/tasks'])

    expect(await screen.findByRole('heading', { level: 1, name: 'Task Board' })).toBeInTheDocument()

    // Staff nav items
    expect(screen.getByRole('link', { name: 'Task Board' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Support' })).toBeInTheDocument()

    // Manager / Ops / Admin not visible
    expect(screen.queryByRole('link', { name: 'Facility Overview' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Business Overview' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'User Management' })).not.toBeInTheDocument()

    // RoleChip
    expect(screen.getByTestId('role-chip')).toHaveTextContent('Staff')
  })

  it('renders Facility Manager top bar with all 6 management nav links', async () => {
    setSession({
      id: 3,
      fullName: 'Hoa Pham',
      email: 'manager@storagehub.dev',
      role: 'FACILITY_MANAGER',
    })

    renderWithRouter(['/overview'])

    expect(await screen.findByRole('heading', { level: 1, name: 'Facility Overview' })).toBeInTheDocument()

    expect(screen.getByRole('link', { name: 'Facility Overview' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Units' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Staff & Shifts' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Operations' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Activity Log' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Escalations' })).toBeInTheDocument()

    expect(screen.getByTestId('role-chip')).toHaveTextContent('Facility Manager')
  })

  it('renders Business Ops top bar with Overview, Policy, Reports', async () => {
    setSession({
      id: 4,
      fullName: 'Quoc Le',
      email: 'ops@storagehub.dev',
      role: 'BUSINESS_OPS',
    })

    renderWithRouter(['/business-overview'])

    expect(await screen.findByRole('heading', { level: 1, name: 'Business Overview' })).toBeInTheDocument()

    expect(screen.getByRole('link', { name: 'Business Overview' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Policy' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Reports' })).toBeInTheDocument()

    expect(screen.getByTestId('role-chip')).toHaveTextContent('Business Ops')
  })

  it('renders System Administrator top bar with Users and Login History', async () => {
    setSession({
      id: 5,
      fullName: 'An Hoang',
      email: 'admin@storagehub.dev',
      role: 'SYSTEM_ADMINISTRATOR',
    })

    renderWithRouter(['/users'])

    expect(await screen.findByRole('heading', { level: 1, name: 'User Management' })).toBeInTheDocument()

    expect(screen.getByRole('link', { name: 'User Management' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Login History' })).toBeInTheDocument()

    expect(screen.getByTestId('role-chip')).toHaveTextContent('System Administrator')
  })

  describe('Route Protection & Cross-Role Access (403)', () => {
    it('renders 403 Forbidden screen with CTA "Return to Browse Units" when CUSTOMER accesses /users', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })

      const { router } = renderWithRouter(['/users'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Access Denied' })).toBeInTheDocument()
      expect(screen.getByText('403')).toBeInTheDocument()
      expect(
        screen.getByText(/you do not have permission to access this page/i)
      ).toBeInTheDocument()

      const ctaBtn = screen.getByRole('button', { name: 'Return to Browse Units' })
      expect(ctaBtn).toBeInTheDocument()

      fireEvent.click(ctaBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/units')
      })
    })

    it('renders 403 Forbidden with appropriate CTA when STAFF accesses /overview', async () => {
      setSession({
        id: 2,
        fullName: 'Minh Tran',
        email: 'staff@storagehub.dev',
        role: 'STAFF',
      })

      const { router } = renderWithRouter(['/overview'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Access Denied' })).toBeInTheDocument()
      const ctaBtn = screen.getByRole('button', { name: 'Return to Task Board' })
      expect(ctaBtn).toBeInTheDocument()

      fireEvent.click(ctaBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/tasks')
      })
    })
    it('renders 403 Forbidden with "Return to Sign In" CTA when unauthenticated visitor hits /403', async () => {
      const { router } = renderWithRouter(['/403'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Access Denied' })).toBeInTheDocument()
      const ctaBtn = screen.getByRole('button', { name: 'Return to Sign In' })
      expect(ctaBtn).toBeInTheDocument()

      fireEvent.click(ctaBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/login')
      })
    })
  })

  describe('Detail Routes Rendering', () => {
    it('renders Unit Detail page for authorized Customer', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })

      renderWithRouter(['/units/S-3'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Unit Detail: S-3' })).toBeInTheDocument()
    })

    it('renders Rental Detail page for authorized Customer', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })

      renderWithRouter(['/rentals/10'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Rental Agreement #10' })).toBeInTheDocument()
    })

    it('renders Task Detail page for authorized Staff', async () => {
      setSession({
        id: 2,
        fullName: 'Minh Tran',
        email: 'staff@storagehub.dev',
        role: 'STAFF',
      })

      renderWithRouter(['/tasks/5'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Task #5' })).toBeInTheDocument()
    })
  })

  describe('Avatar Menu & Logout', () => {
    it('opens avatar dropdown, displays user details, profile link and logs out', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })

      const { router } = renderWithRouter(['/units'])

      const avatarBtn = screen.getByRole('button', { name: /user menu/i })
      expect(avatarBtn).toHaveTextContent('L')

      fireEvent.click(avatarBtn)

      // Dropdown content
      expect(screen.getByText('Lan Nguyen')).toBeInTheDocument()
      expect(screen.getByText('lan@storagehub.dev')).toBeInTheDocument()

      const profileLink = screen.getByRole('menuitem', { name: 'Profile' })
      expect(profileLink).toBeInTheDocument()

      const logoutBtn = screen.getByRole('menuitem', { name: 'Log out' })
      expect(logoutBtn).toBeInTheDocument()

      fireEvent.click(logoutBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/login')
      })

      expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
      expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull()
    })
  })

  describe('Profile Page', () => {
    it('renders profile details and allows logout', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
        phone: '0901234567',
      })

      const { router } = renderWithRouter(['/profile'])

      expect(await screen.findByRole('heading', { level: 3, name: 'User Profile' })).toBeInTheDocument()
      expect(screen.getByText('Lan Nguyen')).toBeInTheDocument()
      expect(screen.getAllByText('lan@storagehub.dev').length).toBeGreaterThan(0)
      expect(screen.getByText('0901234567')).toBeInTheDocument()

      const logoutBtn = screen.getByRole('button', { name: 'Log out' })
      fireEvent.click(logoutBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/login')
      })
    })
  })

  describe('404 Not Found Handling', () => {
    it('renders 404 Not Found screen with "Go to Home" CTA for invalid routes', async () => {
      setSession({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })

      const { router } = renderWithRouter(['/non-existent-random-route'])

      expect(await screen.findByRole('heading', { level: 1, name: 'Page Not Found' })).toBeInTheDocument()
      expect(screen.getByText('404')).toBeInTheDocument()

      const homeBtn = screen.getByRole('button', { name: 'Go to Home' })
      fireEvent.click(homeBtn)

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/units')
      })
    })
  })

  describe('Root Route Redirect', () => {
    it('redirects root / to role landing when logged in', async () => {
      setSession({
        id: 3,
        fullName: 'Hoa Pham',
        email: 'manager@storagehub.dev',
        role: 'FACILITY_MANAGER',
      })

      const { router } = renderWithRouter(['/'])

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/overview')
      })
    })

    it('redirects root / to /login when not logged in', async () => {
      const { router } = renderWithRouter(['/'])

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/login')
      })
    })
  })
})
