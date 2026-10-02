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

describe('Story 4.2: Extension Fee, Top-up Deposit, Payment & Effective Date', () => {
  beforeEach(() => {
    localStorage.clear()
    setSession({
      id: 1,
      fullName: 'Lan Nguyen',
      email: 'lan@storagehub.dev',
      role: 'CUSTOMER',
    })
  })

  it('triggers payment modal from extension quote with both PayOS QR and Cash options', async () => {
    renderWithRouter(['/rentals/2'])

    // Open extension modal
    const extendBtn = await screen.findByTestId('extend-rental-btn')
    fireEvent.click(extendBtn)

    // Wait for quote to calculate
    const proceedBtn = await screen.findByTestId('proceed-to-payment-btn')
    expect(proceedBtn).toBeEnabled()
    fireEvent.click(proceedBtn)

    // Payment modal opens
    expect(await screen.findByTestId('payment-modal')).toBeInTheDocument()
    expect(screen.getByText('PayOS QR Payment')).toBeInTheDocument()
    expect(screen.getByText('Cash at Desk')).toBeInTheDocument()
  })

  it('allows selecting Cash payment and generates desk collection guide', async () => {
    renderWithRouter(['/rentals/2'])

    const extendBtn = await screen.findByTestId('extend-rental-btn')
    fireEvent.click(extendBtn)

    const proceedBtn = await screen.findByTestId('proceed-to-payment-btn')
    fireEvent.click(proceedBtn)

    expect(await screen.findByTestId('payment-modal')).toBeInTheDocument()

    // Switch to Cash at Desk tab
    const cashTab = screen.getByTestId('payment-method-cash')
    fireEvent.click(cashTab)

    const payBtn = screen.getByTestId('pay-with-qr-btn')
    expect(payBtn).toHaveTextContent(/Pay with Cash/i)
    fireEvent.click(payBtn)

    // Awaiting cash confirmation state renders
    expect(await screen.findByTestId('desk-cash-panel')).toBeInTheDocument()
    expect(screen.getByText('Cash Collection at Desk')).toBeInTheDocument()
    expect(screen.getByTestId('confirm-cash-btn')).toBeInTheDocument()
  })
})
