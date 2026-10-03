import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RentalDetailPage } from '../pages/rentals/RentalDetailPage'
import { requestCheckout, getCheckoutRequest } from '../api/checkout'
import { resetMockReservations, resetMockCheckoutRequests, resetMockTasks } from '../mocks/handlers'
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

function renderRentalDetailPage(rentalId = '2') {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/rentals/${rentalId}`]}>
        <Routes>
          <Route path="/rentals/:id" element={<RentalDetailPage />} />
          <Route path="/rentals" element={<div data-testid="my-rentals-view">My Rentals View</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 6.1: Customer Checkout Request', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
    resetMockReservations()
    resetMockCheckoutRequests()
    resetMockTasks()
  })

  it('API client requestCheckout submits requestedDate and notes successfully', async () => {
    const res = await requestCheckout(2, {
      requestedDate: '2026-11-20',
      notes: 'Moving to a new apartment',
    })

    expect(res.id).toBeDefined()
    expect(res.reservationId).toBe(2)
    expect(res.requestedDate).toBe('2026-11-20')
    expect(res.notes).toBe('Moving to a new apartment')
    expect(res.status).toBe('PENDING')

    const getRes = await getCheckoutRequest(2)
    expect(getRes.id).toBe(res.id)
    expect(getRes.requestedDate).toBe('2026-11-20')
  })

  it('shows Request Checkout button on CHECKED_IN rental and opens modal with settlement guide', async () => {
    renderRentalDetailPage('2')

    await waitFor(() => {
      expect(screen.getByText('Unit M-5')).toBeInTheDocument()
      expect(screen.getByTestId('request-checkout-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('request-checkout-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('checkout-request-modal')).toBeInTheDocument()
      expect(screen.getByText('Request Unit Checkout')).toBeInTheDocument()
      // 3-case settlement guide
      expect(screen.getByText(/Full Refund/i)).toBeInTheDocument()
      expect(screen.getByText(/Deductions/i)).toBeInTheDocument()
      expect(screen.getByText(/Excess Charges/i)).toBeInTheDocument()
    })
  })

  it('submits checkout request, closes modal, and renders CheckoutRequestedBanner', async () => {
    renderRentalDetailPage('2')

    await waitFor(() => {
      expect(screen.getByTestId('request-checkout-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('request-checkout-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('checkout-request-modal')).toBeInTheDocument()
    })

    const dateInput = screen.getByTestId('checkout-date-input')
    fireEvent.change(dateInput, { target: { value: '2026-11-25' } })

    const notesInput = screen.getByTestId('checkout-notes-input')
    fireEvent.change(notesInput, { target: { value: 'Please inspect around 10:00 AM' } })

    fireEvent.click(screen.getByTestId('submit-checkout-request-btn'))

    await waitFor(() => {
      expect(screen.queryByTestId('checkout-request-modal')).not.toBeInTheDocument()
      expect(screen.getByTestId('checkout-requested-banner')).toBeInTheDocument()
      expect(screen.getByText(/Checkout Inspection Scheduled/i)).toBeInTheDocument()
      expect(screen.getByTestId('banner-reschedule-checkout-btn')).toBeInTheDocument()
    })
  })

  it('reschedule button in banner re-opens modal to update date', async () => {
    renderRentalDetailPage('2')

    await waitFor(() => {
      expect(screen.getByTestId('request-checkout-btn')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByTestId('request-checkout-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('checkout-request-modal')).toBeInTheDocument()
    })

    const dateInput = screen.getByTestId('checkout-date-input')
    fireEvent.change(dateInput, { target: { value: '2026-11-25' } })
    fireEvent.click(screen.getByTestId('submit-checkout-request-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('checkout-requested-banner')).toBeInTheDocument()
    })

    // Click Reschedule Date
    fireEvent.click(screen.getByTestId('banner-reschedule-checkout-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('checkout-request-modal')).toBeInTheDocument()
    })

    const newDateInput = screen.getByTestId('checkout-date-input')
    fireEvent.change(newDateInput, { target: { value: '2026-11-28' } })
    fireEvent.click(screen.getByTestId('submit-checkout-request-btn'))

    await waitFor(() => {
      expect(screen.queryByTestId('checkout-request-modal')).not.toBeInTheDocument()
      expect(screen.getByTestId('checkout-requested-banner')).toBeInTheDocument()
    })
  })
})
