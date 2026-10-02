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

function renderWithRouter(initialEntries: string[] = ['/tasks/1']) {
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

describe('Story 3.3: Check-in Task — Validate Reservation + 100% Rent Payment', () => {
  beforeEach(() => {
    localStorage.clear()
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })
  })

  it('renders Check-in Task detail page with header, status pill and validation box', async () => {
    renderWithRouter(['/tasks/1'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()
    expect(screen.getByText('Check-in')).toBeInTheDocument()
    expect(screen.getByText('Check-in BK-1042')).toBeInTheDocument()
    expect(screen.getByTestId('reservation-code-input')).toBeInTheDocument()
    expect(screen.getByTestId('validate-code-btn')).toBeInTheDocument()
  })

  it('validates reservation code and renders 2-line financial breakdown', async () => {
    renderWithRouter(['/tasks/1'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()

    // Verify 2-line financial breakdown is rendered
    expect(await screen.findByText(/Financial Breakdown/i)).toBeInTheDocument()
    expect(screen.getByText(/10% Deposit Paid/i)).toBeInTheDocument()
    expect(screen.getByText(/100% Full Rent Due/i)).toBeInTheDocument()
    expect(screen.getByTestId('collect-rent-btn')).toBeInTheDocument()
  })

  it('shows error banner when validating unpaid deposit reservation', async () => {
    renderWithRouter(['/tasks/1'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()

    const input = screen.getByTestId('reservation-code-input')
    fireEvent.change(input, { target: { value: 'BK-UNPAID' } })

    const validateBtn = screen.getByTestId('validate-code-btn')
    fireEvent.click(validateBtn)

    expect(await screen.findByTestId('check-in-error-banner')).toBeInTheDocument()
    expect(screen.getByText(/Cannot proceed to check-in because deposit is not paid/i)).toBeInTheDocument()
  })

  it('opens payment modal for collecting 100% rent with PayOS and Cash methods', async () => {
    renderWithRouter(['/tasks/1'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()

    const collectBtn = await screen.findByTestId('collect-rent-btn')
    fireEvent.click(collectBtn)

    // Modal opens
    expect(await screen.findByTestId('payment-modal')).toBeInTheDocument()
    expect(screen.getByText('Rent Payment')).toBeInTheDocument()
    expect(screen.getByTestId('payment-method-payos')).toBeInTheDocument()
    expect(screen.getByTestId('payment-method-cash')).toBeInTheDocument()
  })
})
