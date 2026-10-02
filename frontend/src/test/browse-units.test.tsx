import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowseUnitsPage } from '../pages/unit/BrowseUnitsPage'
import { browseUnits } from '../api/unit'
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

function renderBrowseUnitsPage() {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/units']}>
        <Routes>
          <Route path="/units" element={<BrowseUnitsPage />} />
          <Route path="/units/:code" element={<div data-testid="unit-detail-screen">Unit Detail Screen</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 2.2: Browse Units & Turnover Buffer Availability', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    setTestSession(1, 'CUSTOMER')
  })

  describe('API Client', () => {
    it('browseUnits fetches all available and buffer units without filters', async () => {
      const response = await browseUnits()
      expect(response.totalAvailable).toBe(2)
      expect(response.items).toHaveLength(2)

      const s3 = response.items.find((u) => u.code === 'S-3')
      expect(s3).toBeDefined()
      expect(s3?.isInCleaningBuffer).toBe(true)
      expect(s3?.isImmediatelyAvailable).toBe(false)
      expect(s3?.availabilityStatus).toContain('cleaning buffer')
      expect(s3?.monthlyRate).toBe(345000)

      const m5 = response.items.find((u) => u.code === 'M-5')
      expect(m5).toBeDefined()
      expect(m5?.isInCleaningBuffer).toBe(false)
      expect(m5?.isImmediatelyAvailable).toBe(true)
      expect(m5?.availabilityStatus).toBe('Available now')
      expect(m5?.monthlyRate).toBe(690000)
    })

    it('browseUnits filters by unit type S', async () => {
      const response = await browseUnits({ type: 'S' })
      expect(response.items).toHaveLength(1)
      expect(response.items[0].code).toBe('S-3')
    })
  })

  describe('BrowseUnitsPage Rendering & Interactions', () => {
    it('renders page header, live counter badge, unit cards with 3px status bar and tabular pricing', async () => {
      renderBrowseUnitsPage()

      await waitFor(() => {
        expect(screen.getByTestId('unit-card-S-3')).toBeInTheDocument()
      })

      // Live Availability Badge
      expect(screen.getByTestId('live-available-badge')).toHaveTextContent('2 units available · live')

      // S-3 unit card (buffer)
      const s3Card = screen.getByTestId('unit-card-S-3')
      expect(s3Card).toBeInTheDocument()
      expect(s3Card.querySelector('[data-testid="card-status-bar"]')).toBeInTheDocument()
      expect(s3Card).toHaveTextContent('345.000 ₫')
      expect(s3Card).toHaveTextContent('Available Oct 5 · cleaning buffer')

      // M-5 unit card (available)
      const m5Card = screen.getByTestId('unit-card-M-5')
      expect(m5Card).toBeInTheDocument()
      expect(m5Card.querySelector('[data-testid="card-status-bar"]')).toBeInTheDocument()
      expect(m5Card).toHaveTextContent('690.000 ₫')
      expect(m5Card).toHaveTextContent('Available now')
    })

    it('does not trigger API query on dropdown change until Search button is clicked', async () => {
      renderBrowseUnitsPage()

      await waitFor(() => {
        expect(screen.getByTestId('browse-units-grid')).toBeInTheDocument()
      })

      // Modify dropdown
      const typeSelect = screen.getByTestId('filter-type-select')
      fireEvent.change(typeSelect, { target: { value: 'S' } })

      // Both units should still be visible before submitting search
      expect(screen.getByTestId('unit-card-S-3')).toBeInTheDocument()
      expect(screen.getByTestId('unit-card-M-5')).toBeInTheDocument()

      // Click Search button
      const searchBtn = screen.getByTestId('filter-search-btn')
      fireEvent.click(searchBtn)

      await waitFor(() => {
        expect(screen.getByTestId('unit-card-S-3')).toBeInTheDocument()
        expect(screen.queryByTestId('unit-card-M-5')).not.toBeInTheDocument()
      })
    })

    it('displays factual empty state with filter chips and Clear filters button when no units match', async () => {
      renderBrowseUnitsPage()

      await waitFor(() => {
        expect(screen.getByTestId('browse-units-grid')).toBeInTheDocument()
      })

      // Select Locker
      const typeSelect = screen.getByTestId('filter-type-select')
      fireEvent.change(typeSelect, { target: { value: 'Locker' } })

      const searchBtn = screen.getByTestId('filter-search-btn')
      fireEvent.click(searchBtn)

      await waitFor(() => {
        expect(screen.getByTestId('browse-empty-state')).toBeInTheDocument()
      })

      expect(screen.getByText('0 of 2 units meet all criteria')).toBeInTheDocument()
      expect(screen.getByTestId('filter-chip-type')).toHaveTextContent('Type: Locker')

      // Click Clear filters button
      const clearBtn = screen.getByTestId('clear-filters-btn')
      fireEvent.click(clearBtn)

      await waitFor(() => {
        expect(screen.getByTestId('unit-card-S-3')).toBeInTheDocument()
        expect(screen.getByTestId('unit-card-M-5')).toBeInTheDocument()
      })
    })

    it('navigates to unit detail page on unit card click', async () => {
      renderBrowseUnitsPage()

      await waitFor(() => {
        expect(screen.getByTestId('unit-card-S-3')).toBeInTheDocument()
      })

      const s3Card = screen.getByTestId('unit-card-S-3')
      fireEvent.click(s3Card)

      await waitFor(() => {
        expect(screen.getByTestId('unit-detail-screen')).toBeInTheDocument()
      })
    })
  })
})
