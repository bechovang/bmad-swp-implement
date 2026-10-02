import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { UnitDetailPage } from '../pages/unit/UnitDetailPage'
import { getUnitDetail } from '../api/unit'
import { calculatePricing } from '../api/pricing'
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

function renderUnitDetailPage(unitCode = 'S-3') {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/units/${unitCode}`]}>
        <Routes>
          <Route path="/units/:code" element={<UnitDetailPage />} />
          <Route path="/units" element={<div data-testid="browse-units-page">Browse Units</div>} />
          <Route path="/booking/summary" element={<div data-testid="booking-summary-page">Booking Summary</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 2.1: Unit Detail & Pricing Breakdown', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
  })

  describe('API Clients', () => {
    it('getUnitDetail returns S-3 unit specifications and default pricing rates', async () => {
      const unit = await getUnitDetail('S-3')
      expect(unit.code).toBe('S-3')
      expect(unit.typeName).toBe('S')
      expect(unit.monthlyRate).toBe(345000)
      expect(unit.depositRate).toBe(10)
      expect(unit.sizeM2).toBe(5.0)
      expect(unit.accessType).toBe('PIN')
      expect(unit.securityFeatures.length).toBeGreaterThan(0)
    })

    it('calculatePricing returns 3 months base rent and 10% refundable deposit for S-3', async () => {
      const pricing = await calculatePricing({ unitCode: 'S-3', durationMonths: 3 })
      expect(pricing.unitCode).toBe('S-3')
      expect(pricing.durationMonths).toBe(3)
      expect(pricing.monthlyRate).toBe(345000)
      expect(pricing.baseRent).toBe(1035000)
      expect(pricing.depositAmount).toBe(103500)
      expect(pricing.depositRefundable).toBe(true)
      expect(pricing.totalRent).toBe(1035000)
      expect(pricing.totalDueNow).toBe(103500)
    })
  })

  describe('UnitDetailPage Component Rendering', () => {
    it('renders S-3 unit details, photo, specifications, and security provisions', async () => {
      renderUnitDetailPage('S-3')

      await waitFor(() => {
        expect(screen.getByTestId('unit-detail-page')).toBeInTheDocument()
      })

      // Heading and basic metadata
      expect(screen.getByRole('heading', { level: 1, name: 'Unit Detail: S-3' })).toBeInTheDocument()
      expect(screen.getByText('AVAILABLE')).toBeInTheDocument()
      expect(screen.getByTestId('spec-size')).toHaveTextContent('5.0 m²')
      expect(screen.getByTestId('spec-floor')).toHaveTextContent('Floor 1')
      expect(screen.getByTestId('spec-access')).toHaveTextContent('PIN')
      expect(screen.getByTestId('spec-zone')).toHaveTextContent('Zone A')

      // Security provisions
      expect(screen.getByTestId('security-features-list')).toBeInTheDocument()
      expect(screen.getByText('24/7 CCTV Monitoring')).toBeInTheDocument()

      // Initial Pricing Card with 3 months default
      expect(screen.getByTestId('pricing-card')).toBeInTheDocument()
      expect(screen.getByTestId('monthly-rate-display')).toHaveTextContent('345.000 ₫ / mo')
      expect(screen.getByTestId('duration-display')).toHaveTextContent('3 months')

      await waitFor(() => {
        expect(screen.getByTestId('policy-version-badge')).toHaveTextContent('Active Policy v3')
        expect(screen.getByTestId('base-rent-display')).toHaveTextContent('1.035.000 ₫')
        expect(screen.getByTestId('deposit-amount-display')).toHaveTextContent('103.500 ₫')
        expect(screen.getByText('Deposit (10% refundable)', { exact: false })).toBeInTheDocument()
        expect(screen.getByTestId('total-rent-display')).toHaveTextContent('1.035.000 ₫')
        expect(screen.getByTestId('deposit-due-now-amount')).toHaveTextContent('103.500 ₫')
      })
    })

    it('dynamically recalculates pricing breakdown when duration selector is changed', async () => {
      renderUnitDetailPage('S-3')

      await waitFor(() => {
        expect(screen.getByTestId('unit-detail-page')).toBeInTheDocument()
      })

      // Click 1 Month pill
      const oneMonthButton = screen.getByTestId('duration-option-1')
      fireEvent.click(oneMonthButton)

      await waitFor(() => {
        expect(screen.getByTestId('duration-display')).toHaveTextContent('1 month')
        expect(screen.getByTestId('base-rent-display')).toHaveTextContent('345.000 ₫')
        expect(screen.getByTestId('deposit-amount-display')).toHaveTextContent('34.500 ₫')
        expect(screen.getByTestId('total-rent-display')).toHaveTextContent('345.000 ₫')
        expect(screen.getByTestId('deposit-due-now-amount')).toHaveTextContent('34.500 ₫')
      })

      // Click 6 Months pill
      const sixMonthsButton = screen.getByTestId('duration-option-6')
      fireEvent.click(sixMonthsButton)

      await waitFor(() => {
        expect(screen.getByTestId('duration-display')).toHaveTextContent('6 months')
        expect(screen.getByTestId('base-rent-display')).toHaveTextContent('2.070.000 ₫')
        expect(screen.getByTestId('deposit-amount-display')).toHaveTextContent('207.000 ₫')
        expect(screen.getByTestId('total-rent-display')).toHaveTextContent('2.070.000 ₫')
        expect(screen.getByTestId('deposit-due-now-amount')).toHaveTextContent('207.000 ₫')
      })
    })

    it('triggers navigation to booking summary when Reserve CTA is clicked', async () => {
      renderUnitDetailPage('S-3')

      await waitFor(() => {
        expect(screen.getByTestId('reserve-unit-cta')).toBeInTheDocument()
      })

      const reserveBtn = screen.getByTestId('reserve-unit-cta')
      expect(reserveBtn).not.toBeDisabled()

      fireEvent.click(reserveBtn)

      await waitFor(() => {
        expect(screen.getByTestId('booking-summary-page')).toBeInTheDocument()
      })
    })

    it('displays graceful fallback when image fails to load', async () => {
      renderUnitDetailPage('S-3')

      await waitFor(() => {
        expect(screen.getByTestId('unit-image')).toBeInTheDocument()
      })

      const img = screen.getByTestId('unit-image')
      fireEvent.error(img)

      await waitFor(() => {
        expect(screen.getByTestId('unit-image-fallback')).toBeInTheDocument()
      })
    })

    it('displays error card when unit is not found', async () => {
      renderUnitDetailPage('UNKNOWN-99')

      await waitFor(() => {
        expect(screen.getByTestId('unit-detail-error')).toBeInTheDocument()
        expect(screen.getByText('Unit Not Found')).toBeInTheDocument()
      })
    })
  })
})
