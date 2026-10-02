import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser } from '../types/auth'

function setSession(user: AuthUser, token = 'mock-jwt-token-123') {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function renderWithRouter(initialEntries: string[] = ['/rentals/2']) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  const router = createMemoryRouter(routesConfig, { initialEntries })
  return render(
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <ToastProvider>
          <RouterProvider router={router} />
        </ToastProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

describe('Story 4.1: Rental Extension & Conflict Boundary', () => {
  beforeEach(() => {
    localStorage.clear()
    setSession({
      id: 1,
      fullName: 'Lan Nguyen',
      email: 'lan@storagehub.dev',
      role: 'CUSTOMER',
    })
  })

  it('renders "Extend Rental" button for CHECKED_IN rental on or before endDate', async () => {
    renderWithRouter(['/rentals/2'])

    const extendBtn = await screen.findByTestId('extend-rental-btn')
    expect(extendBtn).toBeInTheDocument()
    expect(extendBtn).toHaveTextContent('Extend Rental')
  })

  it('does not render "Extend Rental" button for RESERVED / non-checked-in rental', async () => {
    renderWithRouter(['/rentals/1'])

    // Wait for page to load
    expect(await screen.findByText('View Check-in Pass')).toBeInTheDocument()
    expect(screen.queryByTestId('extend-rental-btn')).not.toBeInTheDocument()
  })

  it('opens ExtensionModal and displays boundary and 2-line financial breakdown', async () => {
    renderWithRouter(['/rentals/2'])

    const extendBtn = await screen.findByTestId('extend-rental-btn')
    fireEvent.click(extendBtn)

    // Modal opens
    expect(await screen.findByTestId('extension-modal')).toBeInTheDocument()
    expect(screen.getByText('Extend Rental Period')).toBeInTheDocument()
    expect(screen.getByTestId('extension-unit-badge')).toHaveTextContent('M-5')
    expect(screen.getByTestId('extension-date-input')).toBeInTheDocument()

    // Financial breakdown card displays Line 1 (additional rent) and Line 2 (deposit top-up)
    expect(await screen.findByTestId('extension-breakdown-card')).toBeInTheDocument()
    expect(screen.getByText(/Additional Rent/i)).toBeInTheDocument()
    expect(screen.getByText(/Deposit Top-up/i)).toBeInTheDocument()
    expect(screen.getByTestId('extension-total-amount')).toBeInTheDocument()
    expect(screen.getByTestId('proceed-to-payment-btn')).toBeInTheDocument()
  })

  it('displays top-of-form conflict error banner (NFR-8) when selecting date in conflict zone', async () => {
    renderWithRouter(['/rentals/2'])

    const extendBtn = await screen.findByTestId('extend-rental-btn')
    fireEvent.click(extendBtn)

    expect(await screen.findByTestId('extension-modal')).toBeInTheDocument()

    // Select date past boundary (boundary max checkout is 2027-01-14)
    const dateInput = screen.getByTestId('extension-date-input')
    fireEvent.change(dateInput, { target: { value: '2027-02-01' } })

    // Top-of-form error banner renders (NFR-8 format)
    const banner = await screen.findByTestId('extension-conflict-banner')
    expect(banner).toBeInTheDocument()
    expect(banner).toHaveTextContent(/Date Conflict Detected/i)
    expect(banner).toHaveTextContent(/Can't extend to 2027-02-01 — M-5 has a reservation starting 2027-01-15/i)

    // Proceed to payment button should be disabled when conflict exists
    const proceedBtn = screen.getByTestId('proceed-to-payment-btn')
    expect(proceedBtn).toBeDisabled()
  })
})
