import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach, vi, afterEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { PaymentModal } from '../components/payment/PaymentModal'
import { BookingSummaryPage } from '../pages/booking/BookingSummaryPage'
import { ToastProvider } from '../context/ToastContext'
import {
  resetMockPayments,
  resetMockReservations,
  setMockPaymentStatus,
} from '../mocks/handlers'
import { createPaymentLink, getPaymentStatus } from '../api/payment'

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

function renderPaymentModal(props: Partial<React.ComponentProps<typeof PaymentModal>> = {}) {
  const queryClient = createTestQueryClient()
  const defaultProps = {
    open: true,
    onOpenChange: vi.fn(),
    reservationId: 1,
    unitCode: 'S-3',
    amount: 103500,
    purpose: 'DEPOSIT' as const,
    ...props,
  }

  return {
    ...render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/']}>
          <ToastProvider>
            <Routes>
              <Route path="/" element={<PaymentModal {...defaultProps} />} />
              <Route
                path="/rentals/:id"
                element={<div data-testid="rental-detail-target">Rental Detail View</div>}
              />
            </Routes>
          </ToastProvider>
        </MemoryRouter>
      </QueryClientProvider>
    ),
    props: defaultProps,
    queryClient,
  }
}

describe('Story 2.6: Payment Modal FE — QR PayOS tại Deposit, khép Flow 1', () => {
  beforeEach(() => {
    resetMockPayments()
    resetMockReservations()
    localStorage.clear()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  describe('Payment API Client', () => {
    it('createPaymentLink creates PayOS payment with expiresAt and QR data', async () => {
      const res = await createPaymentLink({
        reservationId: 1,
        purpose: 'DEPOSIT',
        method: 'PAYOS',
        amount: 103500,
      })

      expect(res).toBeDefined()
      expect(res.paymentId).toBeDefined()
      expect(res.orderCode).toBeDefined()
      expect(res.amount).toBe(103500)
      expect(res.purpose).toBe('DEPOSIT')
      expect(res.method).toBe('PAYOS')
      expect(res.status).toBe('PENDING')
      expect(res.checkoutUrl).toContain('pay.payos.vn')
      expect(res.qrCode).toBeDefined()
      expect(res.expiresAt).toBeDefined()
    })

    it('getPaymentStatus retrieves payment detail by ID', async () => {
      const created = await createPaymentLink({
        reservationId: 1,
        purpose: 'DEPOSIT',
        method: 'PAYOS',
        amount: 103500,
      })

      const status = await getPaymentStatus(created.paymentId)
      expect(status.id).toBe(created.paymentId)
      expect(status.amount).toBe(103500)
      expect(status.status).toBe('PENDING')
    })
  })

  describe('PaymentModal Component States and UI', () => {
    it('renders initial METHOD_SELECT state for deposit: only PayOS QR rendered (no Cash option)', async () => {
      renderPaymentModal({
        purpose: 'DEPOSIT',
        amount: 103500,
        unitCode: 'S-3',
      })

      expect(screen.getByTestId('payment-modal')).toBeInTheDocument()
      expect(screen.getByText('Deposit Payment')).toBeInTheDocument()
      expect(screen.getByTestId('payment-unit-badge')).toHaveTextContent('S-3')
      expect(screen.getByTestId('modal-amount-display')).toHaveTextContent('103.500 ₫')

      // Strictly renders PayOS QR only for deposit
      expect(screen.getByTestId('payment-method-payos')).toBeInTheDocument()
      expect(screen.queryByTestId('payment-method-cash')).not.toBeInTheDocument()

      expect(screen.getByTestId('pay-with-qr-btn')).toHaveTextContent('Pay with QR (103.500 ₫)')
    })

    it('transitions to AWAITING state upon Pay with QR: shows QR, order code, and countdown timer derived from expiresAt', async () => {
      renderPaymentModal({
        purpose: 'DEPOSIT',
        amount: 103500,
        unitCode: 'S-3',
      })

      fireEvent.click(screen.getByTestId('pay-with-qr-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      expect(screen.getByText('Scan QR to Pay')).toBeInTheDocument()
      expect(screen.getByTestId('countdown-timer')).toBeInTheDocument()
      expect(screen.getByText(/Awaiting payment confirmation/i)).toBeInTheDocument()
      expect(screen.getByTestId('cancel-payment-btn')).toBeInTheDocument()
    })

    it('transitions to SUCCESS state when payment succeeds during polling: shows check tile, consequence line, and View Rental CTA', async () => {
      const { queryClient } = renderPaymentModal({
        reservationId: 1,
        amount: 103500,
        unitCode: 'S-3',
      })

      fireEvent.click(screen.getByTestId('pay-with-qr-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      // Simulate backend payment success (e.g. from PayOS webhook)
      setMockPaymentStatus('SUCCEEDED')
      // Refetch payment query
      await queryClient.invalidateQueries({ queryKey: ['payment-status'] })

      await waitFor(() => {
        expect(screen.getByTestId('payment-success-tile')).toBeInTheDocument()
      })

      expect(screen.getByText('Payment Successful')).toBeInTheDocument()
      expect(screen.getByText('Unit S-3 is reserved for you until check-in.')).toBeInTheDocument()
      expect(screen.getByTestId('view-rental-btn')).toBeInTheDocument()

      // Click View Rental CTA navigates to /rentals/1
      fireEvent.click(screen.getByTestId('view-rental-btn'))
      await waitFor(() => {
        expect(screen.getByTestId('rental-detail-target')).toBeInTheDocument()
      })
    })

    it('clicking Cancel Payment stops polling and reverts to method selection without charging', async () => {
      renderPaymentModal({
        purpose: 'DEPOSIT',
        amount: 103500,
        unitCode: 'S-3',
      })

      fireEvent.click(screen.getByTestId('pay-with-qr-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      fireEvent.click(screen.getByTestId('cancel-payment-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('pay-with-qr-btn')).toBeInTheDocument()
      })
      expect(screen.getByText('Deposit Payment')).toBeInTheDocument()
    })

    it('transitions to FAILED state on payment failure and shows reception hint after 2 failures', async () => {
      const { queryClient } = renderPaymentModal({
        reservationId: 1,
        amount: 103500,
        unitCode: 'S-3',
      })

      // First attempt
      fireEvent.click(screen.getByTestId('pay-with-qr-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      setMockPaymentStatus('FAILED')
      await queryClient.invalidateQueries({ queryKey: ['payment-status'] })

      await waitFor(() => {
        expect(screen.getByTestId('payment-failed-tile')).toBeInTheDocument()
      })
      expect(screen.getByText('No money was taken.')).toBeInTheDocument()
      expect(screen.getByTestId('retry-payment-btn')).toBeInTheDocument()
      // First failure: hint not displayed yet
      expect(screen.queryByTestId('reception-counter-hint')).not.toBeInTheDocument()

      // Retry (second attempt)
      fireEvent.click(screen.getByTestId('retry-payment-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      setMockPaymentStatus('FAILED')
      await queryClient.invalidateQueries({ queryKey: ['payment-status'] })

      await waitFor(() => {
        expect(screen.getByTestId('payment-failed-tile')).toBeInTheDocument()
      })

      // Second failure: shows reception counter hint
      expect(screen.getByTestId('reception-counter-hint')).toBeInTheDocument()
      expect(
        screen.getByText(
          /Having trouble\? You may also complete your booking with staff at our reception counter\./i
        )
      ).toBeInTheDocument()
    })

    it('transitions to EXPIRED state when payment expires or countdown reaches zero and offers Generate New QR', async () => {
      const { queryClient } = renderPaymentModal({
        reservationId: 1,
        amount: 103500,
        unitCode: 'S-3',
      })

      fireEvent.click(screen.getByTestId('pay-with-qr-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })

      // Simulate payment expiration from polling
      setMockPaymentStatus('EXPIRED')
      await queryClient.invalidateQueries({ queryKey: ['payment-status'] })

      await waitFor(() => {
        expect(screen.getByTestId('payment-expired-tile')).toBeInTheDocument()
      })

      expect(screen.getByText('Payment Link Expired')).toBeInTheDocument()
      expect(screen.getByText('No money was taken.')).toBeInTheDocument()
      expect(screen.getByTestId('retry-payment-btn')).toHaveTextContent('Generate New QR')

      // Clicking Generate New QR initiates new payment link creation
      fireEvent.click(screen.getByTestId('retry-payment-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payos-qr-code')).toBeInTheDocument()
      })
      expect(screen.getByText('Scan QR to Pay')).toBeInTheDocument()
    })
  })

  describe('Flow 1 Integration in BookingSummaryPage', () => {
    it('confirming booking launches PaymentModal with deposit amount and unit details', async () => {
      const queryClient = createTestQueryClient()
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/booking/summary?unit=S-3&duration=3&startDate=2026-10-05']}>
            <ToastProvider>
              <Routes>
                <Route path="/booking/summary" element={<BookingSummaryPage />} />
                <Route
                  path="/rentals/:id"
                  element={<div data-testid="rental-detail-target">Rental Detail View</div>}
                />
              </Routes>
            </ToastProvider>
          </MemoryRouter>
        </QueryClientProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('confirm-booking-btn')).toBeInTheDocument()
      })

      fireEvent.click(screen.getByTestId('confirm-booking-btn'))

      await waitFor(() => {
        expect(screen.getByTestId('payment-modal')).toBeInTheDocument()
      })

      expect(screen.getByText('Deposit Payment')).toBeInTheDocument()
      expect(screen.getByTestId('modal-amount-display')).toHaveTextContent('103.500 ₫')
      expect(screen.getByTestId('pay-with-qr-btn')).toBeInTheDocument()
    })
  })
})
