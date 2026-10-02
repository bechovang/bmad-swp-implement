import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import { resetMockPayments, resetMockContracts, resetMockTasks, resetMockReservations } from '../mocks/handlers'
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

describe('Story 3.4: Contract Ritual + Access Code + Activate Rental + Contract Chain', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockReservations()
    resetMockPayments()
    resetMockContracts()
    resetMockTasks()
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })
  })

  it('completes the full Check-in flow: Collect Rent -> Print Contract -> Attach Signed Photo -> Handover Access PIN', async () => {
    renderWithRouter(['/tasks/1'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()

    // 1. Collect Rent
    const collectBtn = await screen.findByTestId('collect-rent-btn')
    fireEvent.click(collectBtn)

    // Select Cash method and initiate
    expect(await screen.findByTestId('payment-modal')).toBeInTheDocument()
    const cashTab = screen.getByTestId('payment-method-cash')
    fireEvent.click(cashTab)

    const payBtn = screen.getByTestId('pay-with-qr-btn')
    fireEvent.click(payBtn)

    // Confirm Cash received
    const confirmCashBtn = await screen.findByTestId('confirm-cash-btn')
    fireEvent.click(confirmCashBtn)

    // Wait for rent payment confirmation and Step 3 to appear
    expect(await screen.findByTestId('contract-ritual-section')).toBeInTheDocument()
    expect(screen.getByTestId('print-contract-btn')).toBeInTheDocument()

    // Handover button is initially disabled (since contract is not SIGNED yet)
    const handoverBtn = screen.getByTestId('handover-access-code-btn')
    expect(handoverBtn).toBeDisabled()

    // 2. Upload signed contract photo
    const fileInput = screen.getByTestId('signed-file-input')
    const fakeFile = new File(['fake photo data'], 'signed_contract.jpg', { type: 'image/jpeg' })
    fireEvent.change(fileInput, { target: { files: [fakeFile] } })

    // Click Attach & Mark Signed
    const attachBtn = await screen.findByTestId('attach-and-sign-btn')
    fireEvent.click(attachBtn)

    // After signing, handover button becomes enabled
    await waitFor(() => {
      expect(screen.getByTestId('handover-access-code-btn')).not.toBeDisabled()
    })

    // 3. Handover Access Code
    fireEvent.click(screen.getByTestId('handover-access-code-btn'))

    // 4. Access PIN is revealed and check-in finalized
    expect(await screen.findByTestId('access-code-reveal-card')).toBeInTheDocument()
    const codeEl = screen.getByTestId('revealed-access-code')
    expect(codeEl).toBeInTheDocument()
    expect(codeEl.textContent?.trim()).toHaveLength(6)
  })

  it('renders contract preview and revision selector on customer Rental Detail page', async () => {
    setSession({
      id: 1,
      fullName: 'Customer User',
      email: 'customer@example.com',
      role: 'CUSTOMER',
    })

    renderWithRouter(['/rentals/1'])

    expect(await screen.findByTestId('contract-preview-card')).toBeInTheDocument()
    expect(screen.getByText('Self-Service Rental Agreement')).toBeInTheDocument()
    expect(screen.getByText('CT-1042')).toBeInTheDocument()
  })
})
