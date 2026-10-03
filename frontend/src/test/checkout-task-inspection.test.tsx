import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach } from 'vitest'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TaskDetailPage } from '../pages/staff/TaskDetailPage'
import { CheckoutTaskModal } from '../components/checkout/CheckoutTaskModal'
import { getCheckoutTaskDetail, submitInspection } from '../api/checkout'
import { resetMockTasks, resetMockReservations, resetMockCheckoutRequests, resetMockInspections } from '../mocks/handlers'
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

function setTestSession(userId = 2, role = 'STAFF') {
  localStorage.setItem(AUTH_TOKEN_KEY, `mock-jwt-token-for-${role.toLowerCase()}-${userId}`)
  localStorage.setItem(
    AUTH_USER_KEY,
    JSON.stringify({
      id: userId,
      fullName: 'Minh Tran',
      email: 'staff@storagehub.dev',
      role,
    })
  )
}

function renderTaskDetailPage(taskId = '3') {
  const queryClient = createTestQueryClient()
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[`/tasks/${taskId}`]}>
        <Routes>
          <Route path="/tasks/:id" element={<TaskDetailPage />} />
          <Route path="/tasks" element={<div data-testid="task-board-view">Task Board</div>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>
  )
}

describe('Story 6.2: Checkout Task — Nhận kho & 4-Point Inspection', () => {
  beforeEach(() => {
    localStorage.clear()
    setTestSession(2, 'STAFF')
    resetMockTasks()
    resetMockReservations()
    resetMockCheckoutRequests()
    resetMockInspections()
  })

  it('API client getCheckoutTaskDetail and submitInspection operate correctly', async () => {
    const detail = await getCheckoutTaskDetail(3)
    expect(detail).toBeDefined()
    expect(detail.unitCode).toBeDefined()

    const submitRes = await submitInspection(2, {
      items: [
        { item: 'ACCESS_CARD', result: 'OK', note: 'Card returned intact' },
        { item: 'PADLOCK', result: 'MAJOR', note: 'Broken key inside lock cylinder' },
        { item: 'CLEANLINESS', result: 'MINOR', note: 'Needs floor sweep' },
        { item: 'STRUCTURE', result: 'OK', note: 'Walls undamaged' },
      ],
      keyReturned: true,
      unitEmptied: true,
      generalNotes: 'Customer handed over keys at desk',
    })

    expect(submitRes.keyReturned).toBe(true)
    expect(submitRes.unitEmptied).toBe(true)
    expect(submitRes.hasMajorDamage).toBe(true)
    expect(submitRes.majorItems).toContain('PADLOCK')
  })

  it('renders Checkout Reception and 4-Point Inspection matrix on TaskDetailPage', async () => {
    renderTaskDetailPage('3')

    await waitFor(() => {
      expect(screen.getByTestId('checkout-task-workflow')).toBeInTheDocument()
      expect(screen.getByText(/Checkout Desk Reception & Inspection/i)).toBeInTheDocument()
      expect(screen.getByTestId('key-returned-checkbox')).toBeInTheDocument()
      expect(screen.getByTestId('unit-emptied-checkbox')).toBeInTheDocument()
      expect(screen.getByTestId('inspection-matrix')).toBeInTheDocument()
    })

    // Check all 4 inspection rows exist
    expect(screen.getByTestId('inspection-row-access_card')).toBeInTheDocument()
    expect(screen.getByTestId('inspection-row-padlock')).toBeInTheDocument()
    expect(screen.getByTestId('inspection-row-cleanliness')).toBeInTheDocument()
    expect(screen.getByTestId('inspection-row-structure')).toBeInTheDocument()
  })

  it('flags MAJOR damage dynamically with warning banner and saves inspection findings', async () => {
    renderTaskDetailPage('3')

    await waitFor(() => {
      expect(screen.getByTestId('checkout-task-workflow')).toBeInTheDocument()
    })

    // Check key handover
    const keyCb = screen.getByTestId('key-returned-checkbox')
    fireEvent.click(keyCb)

    const unitCb = screen.getByTestId('unit-emptied-checkbox')
    fireEvent.click(unitCb)

    // Initially no major damage warning
    expect(screen.queryByTestId('major-damage-warning-banner')).not.toBeInTheDocument()

    // Mark PADLOCK as MAJOR
    const padlockMajorBtn = screen.getByTestId('btn-padlock-major')
    fireEvent.click(padlockMajorBtn)

    // Warning banner appears
    await waitFor(() => {
      expect(screen.getByTestId('major-damage-warning-banner')).toBeInTheDocument()
      expect(screen.getByText(/Major Damage Finding Flagged/i)).toBeInTheDocument()
    })

    // Add note for padlock
    const padlockNoteInput = screen.getByTestId('input-note-padlock')
    fireEvent.change(padlockNoteInput, { target: { value: 'Padlock broken key' } })

    // Submit inspection
    fireEvent.click(screen.getByTestId('submit-inspection-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('inspection-success-banner')).toBeInTheDocument()
      expect(screen.getByText(/Unit inspection findings recorded/i)).toBeInTheDocument()
    })
  })

  it('renders and submits standalone CheckoutTaskModal component', async () => {
    const queryClient = createTestQueryClient()
    const handleClose = () => {}
    const handleSuccess = () => {}

    render(
      <QueryClientProvider client={queryClient}>
        <CheckoutTaskModal
          isOpen={true}
          onClose={handleClose}
          onSuccess={handleSuccess}
          taskId={3}
          reservationId={2}
        />
      </QueryClientProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('checkout-task-modal')).toBeInTheDocument()
      expect(screen.getByText(/Checkout Reception & Unit Inspection/i)).toBeInTheDocument()
      expect(screen.getByTestId('key-returned-checkbox')).toBeInTheDocument()
    })

    // Click unit emptied
    fireEvent.click(screen.getByTestId('unit-emptied-checkbox'))

    // Mark Structure as MINOR
    fireEvent.click(screen.getByTestId('btn-structure-minor'))

    // Submit
    fireEvent.click(screen.getByTestId('submit-inspection-btn'))

    await waitFor(() => {
      expect(screen.queryByTestId('inspection-error-banner')).not.toBeInTheDocument()
    })
  })
})
