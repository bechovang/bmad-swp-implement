import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { RentalDetailPage } from '../pages/rentals/RentalDetailPage'
import { getRentalDetail } from '../api/rental'
import { resetMockReservations } from '../mocks/handlers'
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

function renderRentalDetailPage(rentalId = '1') {
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

describe('Story 2.5: Rental Detail Page', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
    resetMockReservations()
  })

  it('getRentalDetail API client retrieves single reservation with payments', async () => {
    const data = await getRentalDetail(1)
    expect(data.id).toBe(1)
    expect(data.code).toBe('BK-2026-0001')
    expect(data.payments).toBeDefined()
    expect(data.payments?.length).toBe(1)
  })

  it('renders modular cards: specs, timeline, deposit status, and receipt ledger', async () => {
    renderRentalDetailPage('1')

    await waitFor(() => {
      // Header
      expect(screen.getByText('Unit S-3')).toBeInTheDocument()
      expect(screen.getAllByText('Reserved').length).toBeGreaterThan(0)
      expect(screen.getAllByText('BK-2026-0001').length).toBeGreaterThan(0)

      // Card 1: Specs
      expect(screen.getByText('Unit & Facility Specifications')).toBeInTheDocument()
      expect(screen.getByText('5 m²')).toBeInTheDocument()
      expect(screen.getByText(/45 Nguyen Van Troi/i)).toBeInTheDocument()

      // Card 2: Timeline
      expect(screen.getByText('Rental Schedule & Timeline')).toBeInTheDocument()
      expect(screen.getByText('2026-10-05')).toBeInTheDocument()
      expect(screen.getByText('2027-01-05')).toBeInTheDocument()

      // Card 3: Financial & Deposit Held
      expect(screen.getByText('Financial Terms & Deposit Status')).toBeInTheDocument()
      expect(screen.getByText('Held')).toBeInTheDocument()
      expect(screen.getAllByText('103.500 ₫').length).toBeGreaterThan(0)

      // Card 4: Payment Receipts Ledger
      expect(screen.getByText('Payment Receipts Ledger')).toBeInTheDocument()
      expect(screen.getByText('RC-101')).toBeInTheDocument()
      expect(screen.getByText('PAYOS QR')).toBeInTheDocument()
      expect(screen.getByText('Paid')).toBeInTheDocument()
    })
  })

  it('renders multi-receipt payment history for checked-in rental with cash payment', async () => {
    renderRentalDetailPage('2')

    await waitFor(() => {
      expect(screen.getByText('Unit M-5')).toBeInTheDocument()
      expect(screen.getAllByText('Active Rental').length).toBeGreaterThan(0)

      // Access PIN/Code
      expect(screen.getByText('482913')).toBeInTheDocument()

      // Payment receipts table
      expect(screen.getByText('RC-102')).toBeInTheDocument()
      expect(screen.getByText('RC-103')).toBeInTheDocument()
      expect(screen.getByText('CASH')).toBeInTheDocument()
    })
  })

  it('opens Check-in Pass modal from Rental Detail page', async () => {
    renderRentalDetailPage('1')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /view check-in pass/i })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /view check-in pass/i }))

    await waitFor(() => {
      expect(screen.getByText('Check-in Pass')).toBeInTheDocument()
      expect(screen.getByTestId('check-in-pass-code')).toHaveTextContent('BK-2026-0001')
    })
  })

  it('navigates back to My Rentals when clicking back link', async () => {
    renderRentalDetailPage('1')

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /back to my rentals/i })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /back to my rentals/i }))

    await waitFor(() => {
      expect(screen.getByTestId('my-rentals-view')).toBeInTheDocument()
    })
  })

  it('renders dynamic deposit status badge and banner for PENDING_PAYMENT, EXPIRED, and CLOSED', async () => {
    resetMockReservations([
      {
        id: 10,
        code: 'BK-2026-0010',
        customerId: 1,
        customerName: 'Lan Nguyen',
        unitId: 1,
        unitCode: 'S-3',
        unitTypeName: 'S',
        facilityName: 'Tan Binh Depot',
        facilityAddress: '45 Nguyen Van Troi',
        zoneCode: 'A',
        floor: 1,
        sizeM2: 5.0,
        accessType: 'PIN',
        startDate: '2026-10-10',
        endDate: '2027-01-10',
        durationMonths: 3,
        depositAmount: 103500,
        monthlyRate: 345000,
        baseRent: 1035000,
        totalRent: 1035000,
        policyVersion: 'v3',
        accessCode: null,
        status: 'PENDING_PAYMENT',
        payments: [],
      },
      {
        id: 11,
        code: 'BK-2026-0011',
        customerId: 1,
        customerName: 'Lan Nguyen',
        unitId: 1,
        unitCode: 'S-3',
        unitTypeName: 'S',
        facilityName: 'Tan Binh Depot',
        facilityAddress: '45 Nguyen Van Troi',
        zoneCode: 'A',
        floor: 1,
        sizeM2: 5.0,
        accessType: 'PIN',
        startDate: '2026-10-10',
        endDate: '2027-01-10',
        durationMonths: 3,
        depositAmount: 103500,
        monthlyRate: 345000,
        baseRent: 1035000,
        totalRent: 1035000,
        policyVersion: 'v3',
        accessCode: null,
        status: 'EXPIRED',
        payments: [],
      },
      {
        id: 12,
        code: 'BK-2026-0012',
        customerId: 1,
        customerName: 'Lan Nguyen',
        unitId: 1,
        unitCode: 'S-3',
        unitTypeName: 'S',
        facilityName: 'Tan Binh Depot',
        facilityAddress: '45 Nguyen Van Troi',
        zoneCode: 'A',
        floor: 1,
        sizeM2: 5.0,
        accessType: 'PIN',
        startDate: '2026-06-01',
        endDate: '2026-09-01',
        durationMonths: 3,
        depositAmount: 103500,
        monthlyRate: 345000,
        baseRent: 1035000,
        totalRent: 1035000,
        policyVersion: 'v3',
        accessCode: null,
        status: 'CLOSED',
        payments: [],
      },
    ])

    // Test PENDING_PAYMENT
    const render10 = renderRentalDetailPage('10')
    await waitFor(() => {
      expect(screen.getByText('Deposit Payment Required')).toBeInTheDocument()
      expect(screen.getByText('Pending Payment')).toBeInTheDocument()
      expect(screen.getByText('10% Refundable Deposit Due')).toBeInTheDocument()
    })
    render10.unmount()

    // Test EXPIRED
    const render11 = renderRentalDetailPage('11')
    await waitFor(() => {
      expect(screen.getByText('Forfeited')).toBeInTheDocument()
      expect(screen.getByText('Deposit Forfeited (No-Show)')).toBeInTheDocument()
    })
    render11.unmount()

    // Test CLOSED
    const render12 = renderRentalDetailPage('12')
    await waitFor(() => {
      expect(screen.getByText('Settled')).toBeInTheDocument()
      expect(screen.getByText('Deposit Settled / Refunded')).toBeInTheDocument()
    })
    render12.unmount()
  })
})
