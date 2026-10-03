import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { getEscalations, getEscalationById, processSeverityDecision } from '../api/support'
import { resetMockEscalations, resetMockSupportTickets, resetMockUnits } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser } from '../types/auth'

function setSession(user: AuthUser, token = 'mock-jwt-token-fm-3') {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function renderWithRouter(initialEntries: string[] = ['/escalations']) {
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

describe('Story 5.3: Facility Manager Severity Decision & Relocation', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockEscalations()
    resetMockSupportTickets()
    resetMockUnits()
    setSession({
      id: 3,
      fullName: 'Hoa Pham',
      email: 'manager@storagehub.dev',
      role: 'FACILITY_MANAGER',
    })
  })

  it('renders Escalation Inbox with escalated tickets and detail panel', async () => {
    renderWithRouter(['/escalations'])

    expect(await screen.findByTestId('escalations-page')).toBeInTheDocument()
    expect(screen.getByText('Escalation Inbox')).toBeInTheDocument()
    expect(screen.getAllByText('Facility Manager').length).toBeGreaterThan(0)

    // Check escalation cards
    expect(await screen.findByTestId('escalation-row-1')).toBeInTheDocument()
    expect(screen.getAllByText('SR-0033').length).toBeGreaterThan(0)
    expect(screen.getByTestId('escalation-detail-panel')).toBeInTheDocument()
    expect(screen.getByTestId('escalation-customer-description')).toBeInTheDocument()
    expect(screen.getByTestId('escalation-staff-note')).toBeInTheDocument()
  })

  it('allows Facility Manager to mark SEVERE and relocate customer to an available unit', async () => {
    renderWithRouter(['/escalations'])

    expect(await screen.findByTestId('escalation-row-1')).toBeInTheDocument()

    // Switch to severe mode (default)
    const severeModeBtn = screen.getByTestId('mode-severe-btn')
    fireEvent.click(severeModeBtn)

    // Select target unit
    const unitSelect = screen.getByTestId('target-unit-select')
    fireEvent.change(unitSelect, { target: { value: '3' } })

    // Input note
    const noteInput = screen.getByTestId('manager-note-input')
    fireEvent.change(noteInput, {
      target: { value: 'Confirmed severe roof joint leak. Emergency relocation to Unit M-5 authorized.' },
    })

    // Click submit button to open confirmation modal
    const submitBtn = screen.getByTestId('submit-decision-btn')
    fireEvent.click(submitBtn)

    // Destructive confirmation modal appears
    expect(await screen.findByTestId('severity-confirm-modal')).toBeInTheDocument()
    expect(screen.getByText(/Confirm Emergency Relocation/i)).toBeInTheDocument()

    // Confirm decision
    const confirmBtn = screen.getByTestId('confirm-severe-btn')
    fireEvent.click(confirmBtn)

    // Verify success banner and updated decision
    await waitFor(() => {
      expect(screen.getByTestId('escalation-success-banner')).toBeInTheDocument()
    })
    expect(screen.getByText(/Marked SEVERE: Relocated customer/i)).toBeInTheDocument()
  })

  it('allows Facility Manager to mark NOT SEVERE and return ticket to staff', async () => {
    renderWithRouter(['/escalations'])

    expect(await screen.findByTestId('escalation-row-1')).toBeInTheDocument()

    // Switch to Not Severe mode
    const notSevereModeBtn = screen.getByTestId('mode-not-severe-btn')
    fireEvent.click(notSevereModeBtn)

    // Input instructions
    const noteInput = screen.getByTestId('manager-note-input')
    fireEvent.change(noteInput, {
      target: { value: 'Minor condensation only. On-site staff please provide dehumidifier.' },
    })

    // Open confirmation modal
    const submitBtn = screen.getByTestId('submit-decision-btn')
    fireEvent.click(submitBtn)

    expect(await screen.findByTestId('severity-confirm-modal')).toBeInTheDocument()
    expect(screen.getByText(/Confirm Return to Staff/i)).toBeInTheDocument()

    // Confirm return
    const confirmBtn = screen.getByTestId('confirm-not-severe-btn')
    fireEvent.click(confirmBtn)

    await waitFor(() => {
      expect(screen.getByTestId('escalation-success-banner')).toBeInTheDocument()
    })
    expect(screen.getByText(/Marked NOT SEVERE: Ticket returned to on-site staff/i)).toBeInTheDocument()
  })

  it('validates mandatory manager note before allowing decision', async () => {
    renderWithRouter(['/escalations'])

    expect(await screen.findByTestId('escalation-row-1')).toBeInTheDocument()

    const submitBtn = screen.getByTestId('submit-decision-btn')
    fireEvent.click(submitBtn)

    expect(await screen.findByTestId('escalation-error-banner')).toBeInTheDocument()
    expect(screen.getByText(/Please enter a manager decision note/i)).toBeInTheDocument()
  })

  it('supports direct API calls for escalation inbox and severity decision', async () => {
    const list = await getEscalations()
    expect(list.length).toBeGreaterThan(0)
    expect(list[0].ticketCode).toBe('SR-0033')

    const single = await getEscalationById(1)
    expect(single.ticketCode).toBe('SR-0033')

    const decisionResult = await processSeverityDecision(1, {
      decision: 'MAINTENANCE_RELOCATE',
      managerNote: 'Direct API relocation test',
      targetUnitId: 3,
    })
    expect(decisionResult.decision).toBe('MAINTENANCE_RELOCATE')
    expect(decisionResult.relocatedToUnitId).toBe(3)
  })
})
