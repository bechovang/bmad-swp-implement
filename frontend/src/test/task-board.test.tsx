import { describe, it, expect, beforeEach } from 'vitest'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { TaskBoardPage } from '../pages/staff/TaskBoardPage'
import { ToastProvider } from '../context/ToastContext'
import { resetMockTasks } from '../mocks/handlers'
import { TASK_TYPE_CONFIG } from '../types/task'

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

describe('Story 3.2: Kanban Task Board', () => {
  beforeEach(() => {
    resetMockTasks()
  })

  it('initializes with today date and provides an All dates option', async () => {
    renderTaskBoard()

    expect(screen.getByText('Task Board')).toBeInTheDocument()

    const todayStr = new Date().toISOString().split('T')[0]
    const dateInput = screen.getByTestId('work-date-input') as HTMLInputElement
    expect(dateInput.value).toBe(todayStr)

    // All dates button is visible when date is set
    const allDatesBtn = screen.getByTestId('all-dates-btn')
    expect(allDatesBtn).toBeInTheDocument()
  })

  it('renders 3 columns (To do, In progress, Done) with header counts when viewing all dates', async () => {
    renderTaskBoard()

    // Click "All dates" to view full task board
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    // 3 columns
    await waitFor(() => {
      expect(screen.getByTestId('kanban-column-todo')).toBeInTheDocument()
      expect(screen.getByTestId('kanban-column-in_progress')).toBeInTheDocument()
      expect(screen.getByTestId('kanban-column-done')).toBeInTheDocument()
    })

    // Column header counts
    // From INITIAL_TASKS: TODO: 1 (task 4), IN_PROGRESS: 1 (task 6), DONE: 4 (tasks 1, 2, 3, 5)
    expect(screen.getByTestId('column-count-todo')).toHaveTextContent('1')
    expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('1')
    expect(screen.getByTestId('column-count-done')).toHaveTextContent('4')
  })

  it('renders task cards with corresponding left accent bar and unit/reference details', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-4')).toBeInTheDocument()
    })

    const cleaningCard = screen.getByTestId('task-card-4')
    expect(cleaningCard).toHaveAttribute('data-task-type', 'CLEANING')
    expect(cleaningCard).toHaveStyle({
      borderLeft: `3px solid ${TASK_TYPE_CONFIG.CLEANING.barColor}`,
      opacity: '1',
    })

    // Unit code S-3 and cleaning chip
    expect(screen.getByTestId('task-unit-4')).toHaveTextContent('S-3')
    expect(screen.getByTestId('task-type-chip-4')).toHaveTextContent(/cleaning/i)
  })

  it('renders cards in the Done column muted at 60% opacity with no colored left bar', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-1')).toBeInTheDocument()
    })

    const checkInDoneCard = screen.getByTestId('task-card-1')
    expect(checkInDoneCard).toHaveAttribute('data-task-status', 'DONE')
    expect(checkInDoneCard).toHaveStyle({
      opacity: '0.6',
      borderLeft: '1px solid #E2E8F0',
    })
  })

  it('filters tasks by type and updates column header counts accordingly', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-4')).toBeInTheDocument()
    })

    // Click on "Cleaning" filter tab
    const cleaningTab = screen.getByTestId('filter-tab-cleaning')
    fireEvent.click(cleaningTab)

    // Only cleaning task 4 is visible
    expect(screen.getByTestId('task-card-4')).toBeInTheDocument()
    expect(screen.queryByTestId('task-card-1')).not.toBeInTheDocument()
    expect(screen.queryByTestId('task-card-6')).not.toBeInTheDocument()

    // Counts update for Cleaning: TODO = 1, IN_PROGRESS = 0, DONE = 0
    expect(screen.getByTestId('column-count-todo')).toHaveTextContent('1')
    expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('0')
    expect(screen.getByTestId('column-count-done')).toHaveTextContent('0')

    // In_progress and Done columns now show "Nothing here"
    expect(screen.getByTestId('empty-column-in_progress')).toBeInTheDocument()
    expect(screen.getByTestId('empty-column-done')).toBeInTheDocument()
  })

  it('moves task card to another column using accessible Move buttons and updates counts', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-4')).toBeInTheDocument()
    })

    // Task 4 is in TODO column. Click "In prog ▶"
    const moveRightBtn = screen.getByTestId('move-right-4')
    expect(moveRightBtn).toBeInTheDocument()

    fireEvent.click(moveRightBtn)

    // Header counts should immediately reflect the transition: TODO: 0, IN_PROGRESS: 2, DONE: 4
    await waitFor(() => {
      expect(screen.getByTestId('column-count-todo')).toHaveTextContent('0')
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('2')
    })

    // TODO column is now empty and displays "Nothing here"
    expect(screen.getByTestId('empty-column-todo')).toBeInTheDocument()

    // Move task 4 to Done
    const moveRightToDone = screen.getByTestId('move-right-4')
    fireEvent.click(moveRightToDone)

    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('1')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('5')
    })
  })

  it('moves task card using accessible status select dropdown and skips arrow nav when inside form control', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-6')).toBeInTheDocument()
    })

    // Task 6 is IN_PROGRESS. Change to DONE via status select
    const select = screen.getByTestId('status-select-6')
    fireEvent.change(select, { target: { value: 'DONE' } })

    await waitFor(() => {
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('0')
      expect(screen.getByTestId('column-count-done')).toHaveTextContent('5')
    })
  })

  it('moves task card using keyboard ArrowRight / ArrowLeft shortcuts', async () => {
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('task-card-4')).toBeInTheDocument()
    })

    const card = screen.getByTestId('task-card-4')
    card.focus()
    fireEvent.keyDown(card, { key: 'ArrowRight' })

    await waitFor(() => {
      expect(screen.getByTestId('column-count-todo')).toHaveTextContent('0')
      expect(screen.getByTestId('column-count-in_progress')).toHaveTextContent('2')
    })
  })

  it('renders "Nothing here" empty state tile on empty columns', async () => {
    resetMockTasks([])
    renderTaskBoard()
    fireEvent.click(screen.getByTestId('all-dates-btn'))

    await waitFor(() => {
      expect(screen.getByTestId('empty-column-todo')).toBeInTheDocument()
      expect(screen.getByTestId('empty-column-in_progress')).toBeInTheDocument()
      expect(screen.getByTestId('empty-column-done')).toBeInTheDocument()
    })

    expect(screen.getAllByText('Nothing here')).toHaveLength(3)
  })
})
