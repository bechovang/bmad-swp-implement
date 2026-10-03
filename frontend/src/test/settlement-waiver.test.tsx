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

describe('Story 6.4: Settlement waiver trong trần WAIVER_CAP (P2)', () => {
  beforeEach(() => {
    resetMockSettlements()
  })

  it('adjusts net deduction and increases refund when waiver is applied within WAIVER_CAP', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Baseline: Deposit 172.500
    expect(screen.getByTestId('deposit-held-display')).toHaveTextContent('172.500 ₫')

    // Enter damage fee of 40,000 VND with mandatory reason
    fireEvent.change(screen.getByTestId('input-damage-fee'), { target: { value: '40000' } })
    fireEvent.change(screen.getByTestId('input-damage-reason'), { target: { value: 'Scratched door panel' } })

    // Without waiver: refund is 132.500 VND
    await waitFor(() => {
      expect(screen.getByTestId('settlement-net-balance')).toHaveTextContent('132.500 ₫')
    })

    // Apply 20,000 VND waiver with valid reason (within 50,000 VND cap)
    fireEvent.change(screen.getByTestId('input-waiver-amount'), { target: { value: '20000' } })
    fireEvent.change(screen.getByTestId('input-waiver-reason'), { target: { value: 'Customer goodwill discount' } })

    // Verify waiver discount line and updated refund = 152.500 VND
    await waitFor(() => {
      expect(screen.getByTestId('preview-waiver-amount')).toHaveTextContent('+20.000 ₫')
      expect(screen.getByTestId('settlement-net-balance')).toHaveTextContent('152.500 ₫')
    })

    // Finalize button is enabled
    expect(screen.getByTestId('btn-finalize-settlement')).toBeEnabled()
  })

  it('blocks finalization and displays error when waiver amount > 0 and waiver reason is missing', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Enter damage fee with reason
    fireEvent.change(screen.getByTestId('input-damage-fee'), { target: { value: '40000' } })
    fireEvent.change(screen.getByTestId('input-damage-reason'), { target: { value: 'Scratched door panel' } })

    // Enter waiver amount without reason
    fireEvent.change(screen.getByTestId('input-waiver-amount'), { target: { value: '20000' } })
    fireEvent.change(screen.getByTestId('input-waiver-reason'), { target: { value: '' } })

    // Verify error banner is shown
    expect(screen.getByTestId('error-waiver-reason-required')).toBeInTheDocument()

    // Verify finalize button is disabled
    expect(screen.getByTestId('btn-finalize-settlement')).toBeDisabled()
  })

  it('blocks finalization and displays warning when waiver amount exceeds WAIVER_CAP (50.000 ₫)', async () => {
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
    fireEvent.change(screen.getByTestId('input-damage-fee'), { target: { value: '100000' } })
    fireEvent.change(screen.getByTestId('input-damage-reason'), { target: { value: 'Major damage' } })

    // Enter 60,000 VND waiver (exceeding 50,000 VND cap)
    fireEvent.change(screen.getByTestId('input-waiver-amount'), { target: { value: '60000' } })
    fireEvent.change(screen.getByTestId('input-waiver-reason'), { target: { value: 'Exceeding cap attempt' } })

    // Verify waiver exceeded error is shown
    expect(screen.getByTestId('error-waiver-exceeded')).toHaveTextContent(/Waiver exceeds the 50.000 ₫ cap in Rental Policy v3/i)

    // Verify finalize button is disabled
    expect(screen.getByTestId('btn-finalize-settlement')).toBeDisabled()
  })

  it('finalizes settlement with waiver and displays itemized adjustment on permanent receipt', async () => {
    renderWithClient(
      <CheckoutSettlementSection
        reservationId={871}
        reservationCode="BK-2026-00871"
        unitCode="S-3"
        customerName="Lan Nguyen"
        depositHeld={172500}
      />
    )

    // Enter 40,000 VND damage with reason
    fireEvent.change(screen.getByTestId('input-damage-fee'), { target: { value: '40000' } })
    fireEvent.change(screen.getByTestId('input-damage-reason'), { target: { value: 'Lost card badge' } })

    // Apply 30,000 VND waiver with reason
    fireEvent.change(screen.getByTestId('input-waiver-amount'), { target: { value: '30000' } })
    fireEvent.change(screen.getByTestId('input-waiver-reason'), { target: { value: '6-month loyalty waiver' } })

    // Wait for button to be enabled and click finalize
    await waitFor(() => {
      expect(screen.getByTestId('btn-finalize-settlement')).toBeEnabled()
    })

    fireEvent.click(screen.getByTestId('btn-finalize-settlement'))

    // Verify permanent receipt card appears
    await waitFor(() => {
      expect(screen.getByTestId('settlement-receipt-card')).toBeInTheDocument()
      expect(screen.getByTestId('receipt-code')).toHaveTextContent(/^STL-2026-/)
      expect(screen.getByTestId('receipt-waiver-amount')).toHaveTextContent('-30.000 ₫')
      expect(screen.getByTestId('receipt-final-amount')).toHaveTextContent('162.500 ₫')
      expect(screen.getByTestId('receipt-waiver-reason-box')).toHaveTextContent('6-month loyalty waiver')
    })
  })
})
