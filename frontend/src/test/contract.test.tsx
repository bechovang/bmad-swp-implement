import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { ContractPreviewCard } from '../components/contract/ContractPreviewCard'
import { RentalDetailPage } from '../pages/rentals/RentalDetailPage'
import { getContract, getContractByReservation, reDraftContract } from '../api/contract'
import { resetMockContracts, resetMockReservations } from '../mocks/handlers'
import { AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import type { ContractDto } from '../types/contract'

describe('Story 3.1: Contract Auto-Draft & Agreement Preview', () => {
  let queryClient: QueryClient

  const mockContract: ContractDto = {
    id: 1,
    code: 'CT-1042',
    reservationId: 1,
    reservationCode: 'BK-1042',
    policyId: 1,
    policyVersion: 'v3',
    contentSnapshot: JSON.stringify({
      code: 'CT-1042',
      reservationCode: 'BK-1042',
      unitCode: 'S-3',
      monthlyRate: 345000,
      baseRent: 1035000,
      totalRent: 1035000,
      depositAmount: 103500,
      depositRate: 10,
      durationMonths: 3,
      policyVersion: 'v3',
      currency: 'VND',
      startDate: '2026-10-05',
      endDate: '2027-01-05',
      customerName: 'Lan Nguyen',
      customerEmail: 'lan@storagehub.dev',
      customerPhone: '0901234567',
      facilityName: 'Tan Binh Depot',
      facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
      zoneCode: 'A',
      floor: 1,
      sizeM2: 5.0,
    }),
    snapshot: {
      code: 'CT-1042',
      reservationCode: 'BK-1042',
      unitCode: 'S-3',
      monthlyRate: 345000,
      baseRent: 1035000,
      totalRent: 1035000,
      depositAmount: 103500,
      depositRate: 10,
      durationMonths: 3,
      policyVersion: 'v3',
      currency: 'VND',
      startDate: '2026-10-05',
      endDate: '2027-01-05',
      customerName: 'Lan Nguyen',
      customerEmail: 'lan@storagehub.dev',
      customerPhone: '0901234567',
      facilityName: 'Tan Binh Depot',
      facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
      zoneCode: 'A',
      floor: 1,
      sizeM2: 5.0,
    },
    signedPhotoUrl: null,
    status: 'DRAFT',
    supersedesContractId: null,
    isLatest: 1,
  }

  beforeEach(() => {
    queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false, gcTime: 0 },
      },
    })
    localStorage.setItem(AUTH_TOKEN_KEY, 'mock-jwt-token-for-customer-1')
    localStorage.setItem(
      AUTH_USER_KEY,
      JSON.stringify({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })
    )
    resetMockContracts()
    resetMockReservations()
    vi.clearAllMocks()
  })

  const renderWithProviders = (ui: React.ReactElement) => {
    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>{ui}</MemoryRouter>
      </QueryClientProvider>
    )
  }

  describe('ContractPreviewCard Component', () => {
    it('renders contract code, status badge, locked policy version and financial terms', () => {
      renderWithProviders(<ContractPreviewCard contract={mockContract} />)

      // Contract code and status
      expect(screen.getByText('CT-1042')).toBeInTheDocument()
      expect(screen.getByText('Draft')).toBeInTheDocument()
      expect(screen.getByText(/Locked Policy/i)).toBeInTheDocument()
      expect(screen.getAllByText(/v3/i).length).toBeGreaterThan(0)

      // Parties
      expect(screen.getByText('Lan Nguyen')).toBeInTheDocument()
      expect(screen.getByText('lan@storagehub.dev')).toBeInTheDocument()
      expect(screen.getByText(/Tan Binh Depot/i)).toBeInTheDocument()

      // Unit specs
      expect(screen.getByText('S-3')).toBeInTheDocument()
      expect(screen.getByText('2026-10-05')).toBeInTheDocument()
      expect(screen.getByText('2027-01-05')).toBeInTheDocument()

      // Financial breakdown
      expect(screen.getByText('345.000 ₫')).toBeInTheDocument()
      expect(screen.getByText('1.035.000 ₫')).toBeInTheDocument()
      expect(screen.getByText('103.500 ₫')).toBeInTheDocument()
    })

    it('triggers window.print when Print Agreement button is clicked', () => {
      const printSpy = vi.spyOn(window, 'print').mockImplementation(() => {})

      renderWithProviders(<ContractPreviewCard contract={mockContract} />)

      const printBtn = screen.getByRole('button', { name: /Print Agreement/i })
      fireEvent.click(printBtn)

      expect(printSpy).toHaveBeenCalledTimes(1)
      printSpy.mockRestore()
    })

    it('displays re-draft button for staff in draft status and calls callback', () => {
      const onReDraft = vi.fn()

      renderWithProviders(
        <ContractPreviewCard contract={mockContract} isStaff={true} onReDraft={onReDraft} />
      )

      const reDraftBtn = screen.getByTestId('redraft-contract-btn')
      expect(reDraftBtn).toBeInTheDocument()

      fireEvent.click(reDraftBtn)
      expect(onReDraft).toHaveBeenCalledTimes(1)
    })
  })

  describe('Contract API Client', () => {
    it('getContract fetches contract details by ID', async () => {
      const contract = await getContract(1)
      expect(contract).toBeDefined()
      expect(contract.id).toBe(1)
      expect(contract.code).toBe('CT-1042')
      expect(contract.status).toBe('DRAFT')
    })

    it('getContractByReservation fetches latest contract for a reservation', async () => {
      const contract = await getContractByReservation(1)
      expect(contract).toBeDefined()
      expect(contract.reservationId).toBe(1)
      expect(contract.code).toBe('CT-1042')
    })

    it('reDraftContract marks previous superseded and returns new draft revision', async () => {
      const newDraft = await reDraftContract(1)
      expect(newDraft).toBeDefined()
      expect(newDraft.status).toBe('DRAFT')
      expect(newDraft.supersedesContractId).toBe(1)
      expect(newDraft.code).toContain('CT-1042')
    })
  })

  describe('RentalDetailPage Integration', () => {
    it('renders contract preview card when viewing reserved rental record', async () => {
      render(
        <QueryClientProvider client={queryClient}>
          <MemoryRouter initialEntries={['/rentals/1']}>
            <Routes>
              <Route path="/rentals/:id" element={<RentalDetailPage />} />
            </Routes>
          </MemoryRouter>
        </QueryClientProvider>
      )

      // Wait for rental details to load
      await waitFor(() => {
        expect(screen.getByText(/Unit S-3/i)).toBeInTheDocument()
      })

      // Check that contract preview section is rendered
      await waitFor(() => {
        expect(screen.getByTestId('contract-preview-card')).toBeInTheDocument()
      })

      expect(screen.getByText('CT-1042')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /Print Agreement/i })).toBeInTheDocument()
    })
  })
})
