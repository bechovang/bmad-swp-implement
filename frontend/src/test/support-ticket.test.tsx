import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SupportPage } from '../pages/support/SupportPage'
import { RentalDetailPage } from '../pages/rentals/RentalDetailPage'
import { createSupportTicket, getSupportTickets } from '../api/support'
import { resetMockReservations, resetMockSupportTickets } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'

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
      fullName: 'Lan Nguyen',
      email: 'lan@storagehub.dev',
      role,
    })
  )
}

function renderSupportPage() {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/support']}>
        <Routes>
          <Route path="/support" element={<SupportPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

function renderRentalDetailPage(id = 2) {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/rentals/${id}`]}>
        <Routes>
          <Route path="/rentals/:id" element={<RentalDetailPage />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 5.1: Support Ticket Creation and Routing', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
    resetMockReservations()
    resetMockSupportTickets()
  })

  it('API client creates support ticket and returns SR- code', async () => {
    const ticket = await createSupportTicket({
      unitId: 3,
      incidentType: 'LOST_ACCESS',
      description: 'Cannot unlock keypad with my PIN code',
    })

    expect(ticket).toBeDefined()
    expect(ticket.code).toMatch(/^SR-/)
    expect(ticket.incidentType).toBe('LOST_ACCESS')
    expect(ticket.status).toBe('OPEN')
    expect(ticket.assignedStaffName).toBe('Minh Tran')
  })

  it('API client retrieves support tickets list', async () => {
    const tickets = await getSupportTickets()
    expect(Array.isArray(tickets)).toBe(true)
    expect(tickets.length).toBeGreaterThan(0)
    expect(tickets[0].code).toMatch(/^SR-/)
  })

  it('renders SupportPage ticket list and status filters', async () => {
    renderSupportPage()

    expect(screen.getByRole('heading', { level: 1, name: /support requests/i })).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByTestId('support-ticket-card-SR-0032')).toBeInTheDocument()
      expect(screen.getByText('SR-0032')).toBeInTheDocument()
      expect(screen.getByText('DEVICE ISSUE')).toBeInTheDocument()
      expect(screen.getByText(/Sticky door latch repaired/i)).toBeInTheDocument()
    })
  })

  it('validates description min length in NewSupportModal', async () => {
    renderSupportPage()

    await waitFor(() => {
      expect(screen.getByTestId('open-support-modal-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('open-support-modal-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('new-support-modal')).toBeInTheDocument()
    })

    const descInput = screen.getByTestId('support-description-input')
    fireEvent.change(descInput, { target: { value: 'abc' } })
    fireEvent.blur(descInput)

    await waitFor(() => {
      expect(screen.getByText(/Description must be at least 5 characters/i)).toBeInTheDocument()
    })
  })

  it('submits new support ticket from SupportPage successfully', async () => {
    renderSupportPage()

    await waitFor(() => {
      expect(screen.getByTestId('open-support-modal-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('open-support-modal-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('new-support-modal')).toBeInTheDocument()
    })

    const unitSelect = screen.getByTestId('support-unit-select')
    fireEvent.change(unitSelect, { target: { value: '3' } })

    const incidentSelect = screen.getByTestId('incident-type-select')
    fireEvent.change(incidentSelect, { target: { value: 'DEVICE_ISSUE' } })

    const descInput = screen.getByTestId('support-description-input')
    fireEvent.change(descInput, { target: { value: 'Smart lock latch mechanism is jammed' } })

    const submitBtn = screen.getByTestId('submit-support-ticket-btn')
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(screen.queryByTestId('new-support-modal')).not.toBeInTheDocument()
    })
  })

  it('opens support ticket modal directly from RentalDetailPage via Report Issue button', async () => {
    renderRentalDetailPage(2)

    await waitFor(() => {
      expect(screen.getByTestId('report-issue-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('report-issue-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('new-support-modal')).toBeInTheDocument()
    })

    const descInput = screen.getByTestId('support-description-input')
    fireEvent.change(descInput, { target: { value: 'Cage door hinge is broken' } })

    const submitBtn = screen.getByTestId('submit-support-ticket-btn')
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(screen.queryByTestId('new-support-modal')).not.toBeInTheDocument()
    })
  })
})
