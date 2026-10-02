import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BookingSummaryPage } from '../pages/booking/BookingSummaryPage'
import { createReservation, getReservation } from '../api/reservation'
import { ToastProvider } from '../context/ToastContext'
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

function renderBookingSummaryPage(search = '?unit=S-3&duration=3&startDate=2026-10-05') {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/booking/summary${search}`]}>
        <ToastProvider>
          <Routes>
            <Route path="/booking/summary" element={<BookingSummaryPage />} />
            <Route path="/units/:code" element={<div data-testid="unit-detail-mock">Unit Detail Mock</div>} />
            <Route path="/units" element={<div data-testid="browse-units-mock">Browse Units Mock</div>} />
          </Routes>
        </ToastProvider>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 2.3: Reserve re-check + Booking Summary', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
  })

  describe('Reservation API Client', () => {
    it('createReservation sends payload and receives 201 with price snapshot and BK- code', async () => {
      const res = await createReservation({
        unitCode: 'S-3',
        startDate: '2026-10-05',
        durationMonths: 3,
      })

      expect(res).toBeDefined()
      expect(res.code).toMatch(/^BK-/)
      expect(res.unitCode).toBe('S-3')
      expect(res.durationMonths).toBe(3)
      expect(res.monthlyRate).toBe(345000)
      expect(res.baseRent).toBe(1035000)
      expect(res.depositAmount).toBe(103500)
      expect(res.status).toBe('PENDING_PAYMENT')
    })

    it('getReservation retrieves reservation by ID', async () => {
      const created = await createReservation({
        unitCode: 'S-3',
        startDate: '2026-10-05',
        durationMonths: 1,
      })

      const fetched = await getReservation(created.id)
      expect(fetched.id).toBe(created.id)
      expect(fetched.code).toBe(created.code)
      expect(fetched.unitCode).toBe('S-3')
    })
  })

  describe('BookingSummaryPage Component Rendering', () => {
    it('renders all booking summary sections matching layout design notes', async () => {
      renderBookingSummaryPage('?unit=S-3&duration=3&startDate=2026-10-05')

      await waitFor(() => {
        expect(screen.getByTestId('booking-summary-page')).toBeInTheDocument()
      })

      // Header
      expect(screen.getByRole('heading', { level: 1, name: 'Review Booking Terms' })).toBeInTheDocument()
      expect(screen.getByTestId('unit-code-badge')).toHaveTextContent('S-3')

      // Section 1: Storage Space & Facility
      expect(screen.getByTestId('storage-unit-facility-section')).toBeInTheDocument()
      expect(screen.getByTestId('summary-unit-code')).toHaveTextContent('S-3')
      expect(screen.getByTestId('summary-unit-type')).toHaveTextContent('S')
      expect(screen.getByTestId('summary-unit-location')).toHaveTextContent('Floor 1 · Zone A')
      expect(screen.getByTestId('summary-unit-size')).toHaveTextContent('5 m²')
      expect(screen.getByTestId('summary-unit-access')).toHaveTextContent('PIN Keyless Access')
      expect(screen.getByTestId('summary-facility-name')).toHaveTextContent('Tan Binh Depot')
      expect(screen.getByTestId('summary-facility-address')).toHaveTextContent('45 Nguyen Van Troi')

      // Section 2: Rental Timeline
      expect(screen.getByTestId('rental-timeline-section')).toBeInTheDocument()
      expect(screen.getByTestId('summary-start-date')).toHaveTextContent('Oct 5, 2026')
      expect(screen.getByTestId('summary-duration')).toHaveTextContent('3 Months')

      // Section 3: Itemized Financial Terms
      expect(screen.getByTestId('financial-terms-section')).toBeInTheDocument()
      expect(screen.getByTestId('policy-version-badge')).toHaveTextContent('Policy v3')
      expect(screen.getByTestId('item-monthly-rate')).toHaveTextContent('345.000 ₫ / month')
      expect(screen.getByTestId('item-base-rent')).toHaveTextContent('1.035.000 ₫')
      expect(screen.getByTestId('item-deposit-amount')).toHaveTextContent('103.500 ₫')
      expect(screen.getByTestId('item-total-rent')).toHaveTextContent('1.035.000 ₫')

      // Deposit due now highlight card
      expect(screen.getByTestId('deposit-due-now-highlight')).toBeInTheDocument()
      expect(screen.getByTestId('deposit-due-now-value')).toHaveTextContent('103.500 ₫')

      // Section 4: Legal notice
      expect(screen.getByTestId('legal-contract-notice')).toHaveTextContent(
        'Your rental agreement will be auto-drafted from these exact terms and signed during check-in. Deposit is 100% refundable upon move-out settlement.'
      )

      // Actions
      expect(screen.getByTestId('summary-back-btn')).toBeInTheDocument()
      expect(screen.getByTestId('confirm-booking-btn')).toBeInTheDocument()
    })

    it('renders error card when unit does not exist', async () => {
      renderBookingSummaryPage('?unit=UNKNOWN-99&duration=1&startDate=2026-10-05')

      await waitFor(() => {
        expect(screen.getByTestId('booking-summary-error')).toBeInTheDocument()
      })
      expect(screen.getByText('Unable to Load Booking Summary')).toBeInTheDocument()
    })
  })

  describe('Booking Confirmation & 409 Race Conflict Handling', () => {
    it('successfully confirms booking and shows success notification', async () => {
      renderBookingSummaryPage('?unit=S-3&duration=3&startDate=2026-10-05')

      await waitFor(() => {
        expect(screen.getByTestId('confirm-booking-btn')).toBeInTheDocument()
      })

      fireEvent.click(screen.getByTestId('confirm-booking-btn'))

      await waitFor(() => {
        expect(screen.getByText(/Reservation created successfully/i)).toBeInTheDocument()
      })
    })

    it('bounces to /units with error toast when unit receives 409 UNIT_UNAVAILABLE conflict', async () => {
      // Unit M-2 in mock handlers returns 409 UNIT_UNAVAILABLE
      renderBookingSummaryPage('?unit=M-2&duration=1&startDate=2026-10-05')

      await waitFor(() => {
        expect(screen.getByTestId('confirm-booking-btn')).toBeInTheDocument()
      })

      fireEvent.click(screen.getByTestId('confirm-booking-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('browse-units-mock')).toBeInTheDocument()
        expect(screen.getByText(/was just reserved. Similar units still available/i)).toBeInTheDocument()
      })
    })
  })
})
