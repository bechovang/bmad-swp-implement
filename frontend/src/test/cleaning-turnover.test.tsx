import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { TaskBoardPage } from '../pages/staff/TaskBoardPage'
import { ToastProvider } from '../context/ToastContext'
import { resetMockTasks } from '../mocks/handlers'
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

describe('Story 6.5: Cleaning hoàn tất từ card + Turnover Buffer', () => {
  beforeEach(() => {
    resetMockTasks()
  })

  it('blocks completing CLEANING task when turnover buffer is pending and snaps card back with informative toast', async () => {
    const customTasks: TaskDto[] = [
      {
        id: 701,
        type: 'CLEANING',
        refCode: 'BUFFER_PENDING',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-03',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: null,
        dueDate: '2026-10-03',
        timeSlot: 'Morning',
        title: 'Cleaning S-3 (buffer pending)',
        description: 'Turnover cleaning S-3 buffer pending',
      },
    ]
    resetMockTasks(customTasks)

    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-701')).toBeInTheDocument()
    })

    // Try moving CLEANING card to Done via status dropdown
    const select = screen.getByTestId('status-select-701')
    fireEvent.change(select, { target: { value: 'DONE' } })

    // Snap-back: Task should snap back to IN_PROGRESS column, not stay in DONE
    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('1')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('0')
    })

    // Structured closing-step toast displaying buffer date
    expect(await screen.findByText(/turnover buffer pending until 2026-10-05/i)).toBeInTheDocument()
  })

  it('allows completing CLEANING task directly from card when turnover buffer has elapsed (FR-19)', async () => {
    const customTasks: TaskDto[] = [
      {
        id: 702,
        type: 'CLEANING',
        refCode: 'S-3',
        assignedStaffId: 2,
        assignedStaffName: 'Minh Tran',
        workDate: '2026-10-01',
        status: 'IN_PROGRESS',
        unitCode: 'S-3',
        customerName: null,
        dueDate: '2026-10-01',
        timeSlot: 'Morning',
        title: 'Cleaning S-3',
        description: 'Turnover cleaning S-3',
      },
    ]
    resetMockTasks(customTasks)

    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-702')).toBeInTheDocument()
    })

    // Direct card action: click "Done ▶" button directly on card
    const moveRightBtn = screen.getByTestId('move-right-702')
    fireEvent.click(moveRightBtn)

    // Successfully completes to DONE column without navigating away
    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('0')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('1')
    })

    expect(await screen.findByText(/moved to Done/i)).toBeInTheDocument()
  })
})
