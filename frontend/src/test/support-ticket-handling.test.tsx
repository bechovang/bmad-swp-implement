import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { resolveSupportTicket, escalateSupportTicket, getSupportTicketById } from '../api/support'
import { resetMockSupportTickets } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser } from '../types/auth'

function setSession(user: AuthUser, token = 'mock-jwt-token-123') {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function renderWithRouter(initialEntries: string[] = ['/tasks/6']) {
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

describe('Story 5.2: Support Ticket — Xử lý, Resolve & Escalate', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockSupportTickets()
    setSession({
      id: 2,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role: 'STAFF',
    })
  })

  it('renders Support Task detail page with full incident context', async () => {
    renderWithRouter(['/tasks/6'])

    expect(await screen.findByTestId('task-detail-page')).toBeInTheDocument()
    expect(await screen.findByTestId('support-ticket-workflow')).toBeInTheDocument()

    // Incident details
    expect(screen.getByTestId('support-ticket-code')).toHaveTextContent('SR-0033')
    expect(screen.getByTestId('customer-name-display')).toHaveTextContent('Lan Nguyen')
    expect(screen.getByTestId('unit-code-display')).toHaveTextContent('M-2')
    expect(screen.getByTestId('ticket-customer-description')).toBeInTheDocument()
    expect(screen.getByTestId('staff-note-input')).toBeInTheDocument()
    expect(screen.getByTestId('resolve-ticket-btn')).toBeInTheDocument()
    expect(screen.getByTestId('escalate-ticket-btn')).toBeInTheDocument()
  })

  it('allows staff to resolve a support ticket with resolution note', async () => {
    renderWithRouter(['/tasks/6'])

    expect(await screen.findByTestId('support-ticket-workflow')).toBeInTheDocument()

    const noteInput = screen.getByTestId('staff-note-input')
    fireEvent.change(noteInput, { target: { value: 'Inspected ceiling and resealed expansion joint.' } })

    const resolveBtn = screen.getByTestId('resolve-ticket-btn')
    fireEvent.click(resolveBtn)

    await waitFor(() => {
      expect(screen.getByTestId('support-success-banner')).toBeInTheDocument()
    })
    expect(screen.getByText('Support ticket resolved successfully.')).toBeInTheDocument()
  })

  it('allows staff to escalate a support ticket with mandatory note', async () => {
    renderWithRouter(['/tasks/6'])

    expect(await screen.findByTestId('support-ticket-workflow')).toBeInTheDocument()

    // Attempting escalate without note shows error
    const escalateBtn = screen.getByTestId('escalate-ticket-btn')
    fireEvent.click(escalateBtn)

    await waitFor(() => {
      expect(screen.getByTestId('support-error-banner')).toBeInTheDocument()
    })
    expect(screen.getByText(/Please enter an escalation note/i)).toBeInTheDocument()

    // Provide note and escalate
    const noteInput = screen.getByTestId('staff-note-input')
    fireEvent.change(noteInput, { target: { value: 'Structural issue requires external facility engineer.' } })
    fireEvent.click(escalateBtn)

    await waitFor(() => {
      expect(screen.getByTestId('support-success-banner')).toBeInTheDocument()
    })
    expect(screen.getByText('Support ticket escalated to Facility Manager.')).toBeInTheDocument()
  })

  it('renders resolved state and displays resolution note for resolved tickets', async () => {
    renderWithRouter(['/tasks/5'])

    expect(await screen.findByTestId('support-ticket-workflow')).toBeInTheDocument()
    expect(await screen.findByTestId('support-ticket-code')).toHaveTextContent('SR-0032')
    expect(await screen.findByTestId('resolution-note-display')).toBeInTheDocument()
    expect(screen.getByText(/Replaced door hinge and lubricated lock cylinder/i)).toBeInTheDocument()
    expect(screen.getByText(/Support ticket has been closed and tenant notified/i)).toBeInTheDocument()
  })

  it('supports direct API calls for resolve and escalate', async () => {
    const resResolved = await resolveSupportTicket(1, { note: 'Direct resolve test' })
    expect(resResolved.status).toBe('RESOLVED')
    expect(resResolved.resolutionNote).toBe('Direct resolve test')

    const resEscalated = await escalateSupportTicket(2, { note: 'Direct escalate test' })
    expect(resEscalated.status).toBe('ESCALATED')
    expect(resEscalated.escalationNote).toBe('Direct escalate test')

    const ticketAfterEscalate = await getSupportTicketById(2)
    expect(ticketAfterEscalate.status).toBe('ESCALATED')
  })
})
