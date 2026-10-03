import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { CheckoutSettlementSection } from '../components/checkout/CheckoutSettlementSection'
import { resetMockSettlements } from '../mocks/handlers'

function renderWithClient(ui: React.ReactElement) {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>{ui}</BrowserRouter>
    </QueryClientProvider>
  )
}

describe('Story 6.3: Settlement Charge, Preview, Extra Fee, and Rental Closure', () => {
  beforeEach(() => {
    resetMockSettlements()
  })

  it('renders deposit held and calculates refund when damage fee is entered', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Verify deposit baseline display
    expect(screen.getByTestId('deposit-held-display')).toHaveTextContent('172.500 ₫')

    // Initial refund amount without damage
    await waitFor(() => {
      expect(screen.getByTestId('settlement-net-balance')).toHaveTextContent('172.500 ₫')
    })

    // Enter damage fee of 40,000 VND
    const damageFeeInput = screen.getByTestId('input-damage-fee')
    fireEvent.change(damageFeeInput, { target: { value: '40000' } })

    // Provide mandatory damage reason
    const reasonInput = screen.getByTestId('input-damage-reason')
    fireEvent.change(reasonInput, { target: { value: 'Lost access keycard badge' } })

    // Verify real-time balance becomes 132.500 VND
    await waitFor(() => {
      expect(screen.getByTestId('settlement-net-balance')).toHaveTextContent('132.500 ₫')
    })
  })

  it('blocks finalization and displays error when damage fee > 0 and reason is empty', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Enter damage fee
    const damageFeeInput = screen.getByTestId('input-damage-fee')
    fireEvent.change(damageFeeInput, { target: { value: '40000' } })

    // Clear reason
    const reasonInput = screen.getByTestId('input-damage-reason')
    fireEvent.change(reasonInput, { target: { value: '' } })

    // Verify error banner is visible
    expect(screen.getByTestId('error-damage-reason-required')).toBeInTheDocument()

    // Verify finalize button is disabled
    const finalizeBtn = screen.getByTestId('btn-finalize-settlement')
    expect(finalizeBtn).toBeDisabled()
  })

  it('detects extra fee required when charges exceed deposit held', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Enter 200,000 VND damage fee (exceeding 172,500 deposit)
    const damageFeeInput = screen.getByTestId('input-damage-fee')
    fireEvent.change(damageFeeInput, { target: { value: '200000' } })

    const reasonInput = screen.getByTestId('input-damage-reason')
    fireEvent.change(reasonInput, { target: { value: 'Heavy roller door track damage' } })

    // Verify Extra fee notice is displayed
    await waitFor(() => {
      expect(screen.getByTestId('extra-fee-notice')).toBeInTheDocument()
      expect(screen.getByTestId('settlement-net-balance')).toHaveTextContent('+27.500 ₫')
    })

    // Button disabled until cash received is checked
    const finalizeBtn = screen.getByTestId('btn-finalize-settlement')
    expect(finalizeBtn).toBeDisabled()

    // Check cash received
    const cashCheckbox = screen.getByTestId('checkbox-cash-received')
    fireEvent.click(cashCheckbox)

    // Button enabled
    expect(finalizeBtn).not.toBeDisabled()
  })

  it('finalizes settlement and renders immutable receipt card with receipt code', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Enter 40,000 VND damage fee with valid reason
    fireEvent.change(screen.getByTestId('input-damage-fee'), { target: { value: '40000' } })
    fireEvent.change(screen.getByTestId('input-damage-reason'), {
      target: { value: 'Scratched wall paint and missing key fob' },
    })

    const finalizeBtn = screen.getByTestId('btn-finalize-settlement')
    await waitFor(() => {
      expect(finalizeBtn).not.toBeDisabled()
    })

    fireEvent.click(finalizeBtn)

    // Verify receipt card rendered
    await waitFor(() => {
      expect(screen.getByTestId('settlement-receipt-card')).toBeInTheDocument()
      expect(screen.getByTestId('receipt-code')).toHaveTextContent(/STL-2026-/)
      expect(screen.getByTestId('receipt-final-amount')).toHaveTextContent('132.500 ₫')
    })
  })
})
