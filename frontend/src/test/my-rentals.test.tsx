import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MyRentalsPage } from '../pages/rentals/MyRentalsPage'
import { getMyReservations } from '../api/rental'
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

function renderMyRentalsPage() {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/rentals']}>
        <Routes>
          <Route path="/rentals" element={<MyRentalsPage />} />
          <Route path="/rentals/:id" element={<div data-testid="rental-detail-view">Rental Detail View</div>} />
          <Route path="/units" element={<div data-testid="browse-units-view">Browse Units View</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 2.5: My Rentals + Check-in Pass', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(1, 'CUSTOMER')
    resetMockReservations()
  })

  it('getMyReservations API client retrieves reservation list for customer', async () => {
    const data = await getMyReservations()
    expect(Array.isArray(data)).toBe(true)
    expect(data.length).toBeGreaterThan(0)
    expect(data[0].code).toMatch(/^BK-/)
  })

  it('renders page header and 3 tabs with accurate counts', async () => {
    renderMyRentalsPage()

    expect(screen.getByRole('heading', { level: 1, name: /my rentals/i })).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getByTestId('tab-active')).toHaveTextContent(/Active Rentals \(1\)/i)
      expect(screen.getByTestId('tab-reservations')).toHaveTextContent(/Reservations \(1\)/i)
      expect(screen.getByTestId('tab-history')).toHaveTextContent(/History \(0\)/i)
    })
  })

  it('renders active rentals under Active Rentals tab with status badge', async () => {
    renderMyRentalsPage()

    await waitFor(() => {
      expect(screen.getByText('M-5')).toBeInTheDocument()
      expect(screen.getByText('Active Rental')).toBeInTheDocument()
      expect(screen.getByText(/Tan Binh Depot/i)).toBeInTheDocument()
    })
  })

  it('switches to Reservations tab and displays reserved unit with action buttons', async () => {
    renderMyRentalsPage()

    await waitFor(() => {
      expect(screen.getByTestId('tab-reservations')).toBeInTheDocument()
    })

    const tab = screen.getByTestId('tab-reservations')
    fireEvent.mouseDown(tab, { button: 0 })
    fireEvent.click(tab)

    await waitFor(() => {
      expect(screen.getByText('S-3')).toBeInTheDocument()
      expect(screen.getByText('Reserved')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /view check-in pass/i })).toBeInTheDocument()
    })
  })

  it('opens Check-in Pass modal displaying prominent monospace BK- code and counter instructions', async () => {
    renderMyRentalsPage()

    await waitFor(() => {
      expect(screen.getByTestId('tab-reservations')).toBeInTheDocument()
    })

    const tab = screen.getByTestId('tab-reservations')
    fireEvent.mouseDown(tab, { button: 0 })
    fireEvent.click(tab)

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /view check-in pass/i })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: /view check-in pass/i }))

    await waitFor(() => {
      expect(screen.getByText('Check-in Pass')).toBeInTheDocument()
      const codeElement = screen.getByTestId('check-in-pass-code')
      expect(codeElement).toHaveTextContent('BK-2026-0001')
      expect(screen.getByText(/Bring National ID \/ Passport:/i)).toBeInTheDocument()
      expect(screen.getByText(/Sign Rental Agreement:/i)).toBeInTheDocument()
      expect(screen.getByText(/Pay Remaining Balance:/i)).toBeInTheDocument()
    })
  })

  it('renders factual empty state with CTA to /units when a tab has no records', async () => {
    renderMyRentalsPage()

    await waitFor(() => {
      expect(screen.getByTestId('tab-history')).toBeInTheDocument()
    })

    const historyTab = screen.getByTestId('tab-history')
    fireEvent.mouseDown(historyTab, { button: 0 })
    fireEvent.click(historyTab)

    await waitFor(() => {
      expect(screen.getByText('You have no past rental history')).toBeInTheDocument()
      const browseBtns = screen.getAllByRole('button', { name: /browse units/i })
      expect(browseBtns.length).toBeGreaterThan(0)

      fireEvent.click(browseBtns[browseBtns.length - 1])
    })

    await waitFor(() => {
      expect(screen.getByTestId('browse-units-view')).toBeInTheDocument()
    })
  })
})
