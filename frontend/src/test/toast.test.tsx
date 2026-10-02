import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, expect, it, vi, beforeEach, afterEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { Toast } from '../components/ui/Toast'
import { ToastProvider } from '../context/ToastContext'
import { useToast } from '../hooks/useToast'
import type { ToastNotification } from '../types/notification'

describe('Toast System', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders toast with title, tone accent bar, and dismiss button', () => {
    const handleDismiss = vi.fn()
    const toastData: ToastNotification = {
      id: 'test-1',
      title: 'Deposit received successfully',
      tone: 'success',
      deepLink: '/rentals/1',
      actionLabel: 'View details',
    }

    render(
      <MemoryRouter>
        <Toast toast={toastData} onDismiss={handleDismiss} />
      </MemoryRouter>
    )

    expect(screen.getByText('Deposit received successfully')).toBeInTheDocument()
    const link = screen.getByRole('link', { name: 'View details' })
    expect(link).toHaveAttribute('href', '/rentals/1')

    const accentBar = screen.getByTestId('toast-accent-bar')
    expect(accentBar).toHaveClass('bg-sh-success')

    const dismissBtn = screen.getByRole('button', { name: /dismiss notification/i })
    fireEvent.click(dismissBtn)
    expect(handleDismiss).toHaveBeenCalledWith('test-1')
  })

  it('auto-dismisses after duration (default 4000ms)', () => {
    const handleDismiss = vi.fn()
    const toastData: ToastNotification = {
      id: 'test-auto',
      title: 'Auto dismissing toast',
    }

    render(
      <MemoryRouter>
        <Toast toast={toastData} onDismiss={handleDismiss} duration={4000} />
      </MemoryRouter>
    )

    expect(handleDismiss).not.toHaveBeenCalled()
    act(() => {
      vi.advanceTimersByTime(3999)
    })
    expect(handleDismiss).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(handleDismiss).toHaveBeenCalledWith('test-auto')
  })

  it('pauses countdown on hover and resumes on mouse leave', () => {
    const handleDismiss = vi.fn()
    const toastData: ToastNotification = {
      id: 'test-hover',
      title: 'Hover pause toast',
    }

    render(
      <MemoryRouter>
        <Toast toast={toastData} onDismiss={handleDismiss} duration={4000} />
      </MemoryRouter>
    )

    // Advance 2000ms
    act(() => {
      vi.advanceTimersByTime(2000)
    })
    expect(handleDismiss).not.toHaveBeenCalled()

    // Hover mouse
    const toastEl = screen.getByRole('status')
    fireEvent.mouseEnter(toastEl)

    // Wait 5000ms while hovered
    act(() => {
      vi.advanceTimersByTime(5000)
    })
    expect(handleDismiss).not.toHaveBeenCalled()

    // Mouse leave - remaining 2000ms should count down
    fireEvent.mouseLeave(toastEl)

    act(() => {
      vi.advanceTimersByTime(1999)
    })
    expect(handleDismiss).not.toHaveBeenCalled()

    act(() => {
      vi.advanceTimersByTime(2)
    })
    expect(handleDismiss).toHaveBeenCalledWith('test-hover')
  })

  it('renders different tone accent bars correctly', () => {
    const tones = [
      { tone: 'info', className: 'bg-sh-primary' },
      { tone: 'success', className: 'bg-sh-success' },
      { tone: 'warning', className: 'bg-sh-warning-bar' },
      { tone: 'error', className: 'bg-sh-error' },
    ] as const

    for (const { tone, className } of tones) {
      const { unmount } = render(
        <MemoryRouter>
          <Toast
            toast={{ id: tone, title: `${tone} toast`, tone }}
            onDismiss={vi.fn()}
          />
        </MemoryRouter>
      )
      const bar = screen.getByTestId('toast-accent-bar')
      expect(bar).toHaveClass(className)
      unmount()
    }
  })

  it('provides showToast and dismissToast through ToastProvider and useToast', () => {
    function TestConsumer() {
      const { showToast, dismissToast, toasts } = useToast()
      return (
        <div>
          <button
            type="button"
            onClick={() =>
              showToast({
                id: 'custom-toast',
                title: 'Mutation succeeded',
                tone: 'success',
                deepLink: '/rentals/1',
              })
            }
          >
            Trigger Toast
          </button>
          <button type="button" onClick={() => dismissToast('custom-toast')}>
            Manual Dismiss
          </button>
          <span data-testid="toast-count">{toasts.length}</span>
        </div>
      )
    }

    render(
      <MemoryRouter>
        <ToastProvider>
          <TestConsumer />
        </ToastProvider>
      </MemoryRouter>
    )

    expect(screen.getByTestId('toast-count')).toHaveTextContent('0')

    fireEvent.click(screen.getByRole('button', { name: 'Trigger Toast' }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('1')
    expect(screen.getByText('Mutation succeeded')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Manual Dismiss' }))
    expect(screen.getByTestId('toast-count')).toHaveTextContent('0')
  })

  it('throws error when useToast is used outside ToastProvider', () => {
    function InvalidConsumer() {
      useToast()
      return null
    }

    expect(() => render(<InvalidConsumer />)).toThrow(
      'useToast must be used within a ToastProvider'
    )
  })
})
