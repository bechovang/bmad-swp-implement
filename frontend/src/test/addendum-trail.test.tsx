import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import { resetMockContracts, resetMockTasks, resetMockReservations, resetMockPayments } from '../mocks/handlers'
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

describe('Story 4.3: Addendum Trail — 7-Day Reminder + Desk Ritual + Contract Chain', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockReservations()
    resetMockPayments()
    resetMockContracts()
    resetMockTasks()
  })

  it('renders 7-day addendum reminder banner on Rental Detail page when addendum is awaiting signature', async () => {
    setSession({
      id: 1,
      fullName: 'Lan Nguyen',
      email: 'lan@storagehub.dev',
      role: 'CUSTOMER',
    })

    renderWithRouter(['/rentals/2'])

    // Wait for rental details to load and verify reminder banner
    expect(await screen.findByTestId('addendum-reminder-banner')).toBeInTheDocument()
    expect(screen.getByText(/Extension Addendum Signing/i)).toBeInTheDocument()
    expect(screen.getAllByText(/CT-1042-A1/i).length).toBeGreaterThan(0)
  })

  it('executes the full Addendum Desk Ritual on TaskDetailPage: Print -> Upload Signed Photo -> Attach & Complete Task', async () => {
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })

    resetMockTasks([
      {
        id: 2,
        type: 'CONTRACT',
        refCode: 'CT-1042-A1',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-16',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: 'Lan Nguyen',
        dueDate: '2026-10-16',
        timeSlot: 'Morning',
        title: 'Contract Signature CT-1042-A1',
        description: 'Contract and addendum signing at front desk',
      },
    ])

    renderWithRouter(['/tasks/2'])

    // Contract workflow renders
    expect(await screen.findByTestId('contract-signature-workflow')).toBeInTheDocument()
    expect(screen.getByTestId('contract-code-display')).toHaveTextContent('CT-1042-A1')
    expect(screen.getByTestId('addendum-status-chip')).toHaveTextContent('AWAITING_SIGNATURE')

    // Step 1: Print Addendum
    const printBtn = screen.getByTestId('print-contract-btn')
    expect(printBtn).toBeInTheDocument()
    fireEvent.click(printBtn)

    // Step 2: Upload signed document photo
    const fileInput = screen.getByTestId('signed-file-input')
    const fakeFile = new File(['fake-addendum-image'], 'signed_addendum.jpg', { type: 'image/jpeg' })
    fireEvent.change(fileInput, { target: { files: [fakeFile] } })

    // Step 3: Attach & Record Signature
    const attachBtn = await screen.findByTestId('attach-and-sign-btn')
    fireEvent.click(attachBtn)

    // Verify confirmation card appears with SIGNED status
    expect(await screen.findByTestId('signed-confirmation-card')).toBeInTheDocument()
    expect(screen.getByTestId('addendum-status-chip')).toHaveTextContent('SIGNED')

    // Complete task
    const completeBtn = screen.getByTestId('complete-contract-task-btn')
    fireEvent.click(completeBtn)

    await waitFor(() => {
      expect(screen.getByTestId('task-status-badge')).toHaveTextContent('DONE')
    })
  })

  it('allows staff to close an unsigned addendum as EXPIRED after the 7-day window', async () => {
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })

    resetMockTasks([
      {
        id: 2,
        type: 'CONTRACT',
        refCode: 'CT-1042-A1',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-16',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: 'Lan Nguyen',
        dueDate: '2026-10-16',
        timeSlot: 'Morning',
        title: 'Contract Signature CT-1042-A1',
        description: 'Contract and addendum signing at front desk',
      },
    ])

    renderWithRouter(['/tasks/2'])

    expect(await screen.findByTestId('contract-signature-workflow')).toBeInTheDocument()

    const expireBtn = await screen.findByTestId('expire-contract-btn')
    fireEvent.click(expireBtn)

    // Status updates to EXPIRED
    await waitFor(() => {
      expect(screen.getByTestId('addendum-status-chip')).toHaveTextContent('EXPIRED')
    })
  })
})
