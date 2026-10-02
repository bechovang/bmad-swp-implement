import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { TaskBoardPage } from '../pages/staff/TaskBoardPage'
import { ToastProvider } from '../context/ToastContext'
import { resetMockTasks, resetMockPayments, resetMockContracts, resetMockReservations } from '../mocks/handlers'
import type { TaskDto } from '../types/task'

function renderTaskBoard() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <ToastProvider>
        <BrowserRouter>
          <TaskBoardPage />
        </BrowserRouter>
      </ToastProvider>
    </QueryClientProvider>
  )
}

describe('Story 3.5: Snap-back chống Done ảo (Task Closing Guards)', () => {
  beforeEach(() => {
    resetMockTasks()
    resetMockReservations()
    resetMockPayments()
    resetMockContracts()
  })

  it('snaps back Check-in task when moved to DONE if rent is unpaid and shows structured toast', async () => {
    // Custom task: Check-in in IN_PROGRESS for reservation 1 (which has unpaid rent in initial state)
    const customTasks: TaskDto[] = [
      {
        id: 10,
        type: 'CHECK_IN',
        refCode: 'BK-1042',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-03',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: 'Lan Nguyen',
        dueDate: '2026-10-03',
        timeSlot: 'Morning',
        title: 'Check-in BK-1042',
        description: 'Check-in customer Lan Nguyen',
      },
    ]
    resetMockTasks(customTasks)

    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-10')).toBeInTheDocument()
    })

    // Try moving to Done using select dropdown
    const select = screen.getByTestId('status-select-10')
    fireEvent.change(select, { target: { value: 'DONE' } })

    // Snap-back: Task should snap back to IN_PROGRESS column, not stay in DONE
    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('1')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('0')
    })

    // Toast message rendered from payload
    expect(await screen.findByText(/Rent payment is still pending on this check-in/i)).toBeInTheDocument()
  })

  it('allows moving CLEANING and SUPPORT tasks to DONE without closing step guards', async () => {
    const customTasks: TaskDto[] = [
      {
        id: 20,
        type: 'CLEANING',
        refCode: 'S-3',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-03',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: null,
        dueDate: '2026-10-03',
        timeSlot: 'Morning',
        title: 'Cleaning S-3',
        description: 'Turnover cleaning',
      },
    ]
    resetMockTasks(customTasks)

    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-20')).toBeInTheDocument()
    })

    const moveRightBtn = screen.getByTestId('move-right-20')
    fireEvent.click(moveRightBtn)

    // Successfully moves to DONE
    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('0')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('1')
    })

    // Success toast
    expect(await screen.findByText(/moved to Done/i)).toBeInTheDocument()
  })

  it('snaps back via keyboard ArrowRight when closing steps are incomplete', async () => {
    const customTasks: TaskDto[] = [
      {
        id: 30,
        type: 'CHECK_IN',
        refCode: 'BK-1042',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-03',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: 'Lan Nguyen',
        dueDate: '2026-10-03',
        timeSlot: 'Morning',
        title: 'Check-in BK-1042',
        description: 'Check-in customer Lan Nguyen',
      },
    ]
    resetMockTasks(customTasks)

    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-30')).toBeInTheDocument()
    })

    const card = screen.getByTestId('task-card-30')
    card.focus()
    fireEvent.keyDown(card, { key: 'ArrowRight' })

    // Snap-back occurs
    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('1')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('0')
    })

    expect(await screen.findByText(/Rent payment is still pending on this check-in/i)).toBeInTheDocument()
  })
})
