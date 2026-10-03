import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { ToastProvider } from '../context/ToastContext'
import { resetMockSupportTickets, resetMockReservations } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { AuthUser } from '../types/auth'

function setCustomerSession(
  user: AuthUser = {
    id: 1,
    fullName: 'Lan Nguyen',
    email: 'lan@storagehub.dev',
    role: 'CUSTOMER',
  },
  token = 'mock-jwt-token-customer-1'
) {
  localStorage.setItem(AUTH_TOKEN_KEY, token)
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
}

function renderWithRouter(initialEntries: string[] = ['/support']) {
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

describe('Story 5.4: Customer Support List & Detail Drawer', () => {
  beforeEach(() => {
    localStorage.clear()
    resetMockSupportTickets()
    resetMockReservations()
    setCustomerSession()
  })

  it('renders Customer Support List with ticket cards, status badges, and filter tabs', async () => {
    renderWithRouter(['/support'])

    expect(await screen.findByTestId('support-page')).toBeInTheDocument()
    expect(screen.getByText('Support Requests')).toBeInTheDocument()
    expect(screen.getByTestId('open-support-modal-btn')).toBeInTheDocument()

    // Ticket list and cards
    expect(await screen.findByTestId('support-ticket-card-SR-0032')).toBeInTheDocument()
    expect(screen.getByText('SR-0032')).toBeInTheDocument()
    expect(screen.getByText('SR-0035')).toBeInTheDocument()
  })

  it('clicking a ticket card opens the 420px slide-over detail drawer with full chronological arc', async () => {
    renderWithRouter(['/support'])

    const card = await screen.findByTestId('support-ticket-card-SR-0035')
    fireEvent.click(card)

    // Verify drawer opened
    expect(await screen.findByTestId('support-detail-drawer')).toBeInTheDocument()
    expect(screen.getByTestId('drawer-ticket-code')).toHaveTextContent('SR-0035')

    // Stage 1: Reported
    expect(screen.getByTestId('timeline-stage-reported')).toBeInTheDocument()
    expect(screen.getByTestId('drawer-customer-description')).toHaveTextContent(
      'Severe pipe leak flooding Unit M-2'
    )

    // Stage 2: Assigned
    const assignedStage = screen.getByTestId('timeline-stage-assigned')
    expect(assignedStage).toBeInTheDocument()
    expect(assignedStage).toHaveTextContent('Minh Tran')

    // Stage 3: Escalated
    expect(screen.getByTestId('timeline-stage-escalated')).toBeInTheDocument()
    expect(screen.getByTestId('drawer-escalation-note')).toHaveTextContent(
      'Active flood requiring urgent customer relocation'
    )

    // Stage 4: Manager Decision & Relocation
    expect(screen.getByTestId('timeline-stage-decision')).toBeInTheDocument()
    expect(screen.getByTestId('drawer-relocation-banner')).toHaveTextContent(
      'You have been relocated to Unit M-5'
    )

    // Stage 5: Final Resolution
    expect(screen.getByTestId('timeline-stage-resolved')).toBeInTheDocument()
    expect(screen.getByTestId('drawer-resolution-note')).toHaveTextContent(
      'Relocation complete and customer access code handed over successfully.'
    )
  })

  it('closes detail drawer via close button and backdrop scrim', async () => {
    renderWithRouter(['/support'])

    const card = await screen.findByTestId('support-ticket-card-SR-0032')
    fireEvent.click(card)

    expect(await screen.findByTestId('support-detail-drawer')).toBeInTheDocument()

    // Close with top X button
    const closeBtn = screen.getByTestId('close-support-drawer-btn')
    fireEvent.click(closeBtn)

    await waitFor(() => {
      expect(screen.queryByTestId('support-detail-drawer')).not.toBeInTheDocument()
    })

    // Open again and close with scrim
    fireEvent.click(card)
    expect(await screen.findByTestId('support-detail-drawer')).toBeInTheDocument()

    const scrim = screen.getByTestId('drawer-scrim')
    fireEvent.click(scrim)

    await waitFor(() => {
      expect(screen.queryByTestId('support-detail-drawer')).not.toBeInTheDocument()
    })
  })

  it('filters tickets when status tab is selected', async () => {
    renderWithRouter(['/support'])

    expect(await screen.findByTestId('support-ticket-card-SR-0032')).toBeInTheDocument()

    // Click ESCALATED tab
    const escalatedTab = screen.getByRole('button', { name: /^ESCALATED$/i })
    fireEvent.click(escalatedTab)

    expect(screen.queryByTestId('support-ticket-card-SR-0032')).not.toBeInTheDocument()
    expect(screen.getByTestId('support-ticket-card-SR-0034')).toBeInTheDocument()
  })

  it('displays empty state with single CTA button when no tickets exist', async () => {
    resetMockSupportTickets([])

    renderWithRouter(['/support'])

    expect(await screen.findByText('No support tickets found')).toBeInTheDocument()
    expect(screen.getByTestId('empty-new-support-btn')).toBeInTheDocument()

    // Click empty CTA opens NewSupportModal
    fireEvent.click(screen.getByTestId('empty-new-support-btn'))
    expect(await screen.findByRole('dialog')).toBeInTheDocument()
    expect(screen.getByText('Open Support Ticket')).toBeInTheDocument()
  })
})
