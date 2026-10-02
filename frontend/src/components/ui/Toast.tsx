import { useEffect, useRef, useCallback } from 'react'
import { cn } from '../../lib/utils'
import type { ToastNotification, ToastTone } from '../../types/notification'

export interface ToastProps {
  toast: ToastNotification
  onDismiss: (id: string) => void
  duration?: number
  className?: string
}

const toneBarStyles: Record<ToastTone, string> = {
  info: 'bg-sh-primary',
  success: 'bg-sh-success',
  warning: 'bg-sh-warning-bar',
  error: 'bg-sh-error',
}

export function Toast({
  toast,
  onDismiss,
  duration = 4000,
  className,
}: ToastProps) {
  const tone = toast.tone || 'info'
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const remainingRef = useRef<number>(duration)
  const startRef = useRef<number>(0)

  const clearTimer = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
  }, [])

  const startTimer = useCallback(() => {
    clearTimer()
    startRef.current = Date.now()
    timerRef.current = setTimeout(() => {
      if (toast.id) {
        onDismiss(toast.id)
      }
    }, remainingRef.current)
  }, [clearTimer, onDismiss, toast.id])

  const handleMouseEnter = () => {
    if (!timerRef.current) return
    clearTimer()
    const elapsed = Date.now() - startRef.current
    remainingRef.current = Math.max(0, remainingRef.current - elapsed)
  }

  const handleMouseLeave = () => {
    if (remainingRef.current > 0) {
      startTimer()
    } else if (toast.id) {
      onDismiss(toast.id)
    }
  }

  const handleLinkClick = () => {
    if (toast.id) {
      onDismiss(toast.id)
    }
  }

  useEffect(() => {
    startTimer()
    return () => clearTimer()
  }, [startTimer, clearTimer])

  return (
    <div
      role="status"
      aria-live="polite"
      onMouseEnter={handleMouseEnter}
      onMouseLeave={handleMouseLeave}
      className={cn(
        'relative flex items-start justify-between gap-3 p-3.5 pl-4',
        'bg-sh-surface border border-sh-border rounded-sh-md shadow-sh-overlay',
        'w-[360px] max-w-[calc(100vw-2rem)] overflow-hidden transition-all',
        className
      )}
    >
      {/* 3px left accent bar */}
      <span
        aria-hidden="true"
        data-testid="toast-accent-bar"
        className={cn('absolute left-0 top-0 bottom-0 w-[3px]', toneBarStyles[tone])}
      />

      <div className="flex-1 min-w-0 pr-1 text-left">
        <p className="typography-body text-sh-ink font-medium leading-snug">{toast.title}</p>
        {toast.deepLink && (
          <div className="mt-1">
            <a
              href={toast.deepLink}
              onClick={handleLinkClick}
              className="typography-meta text-sh-primary hover:underline font-semibold inline-block focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-sh-primary rounded-sh-sm"
            >
              {toast.actionLabel || 'View details'}
            </a>
          </div>
        )}
      </div>

      <button
        type="button"
        aria-label="Dismiss notification"
        onClick={() => toast.id && onDismiss(toast.id)}
        className="text-sh-muted hover:text-sh-ink p-1 rounded-sh-sm transition-colors text-[14px] leading-none shrink-0 cursor-pointer focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary"
      >
        <span aria-hidden="true">✕</span>
      </button>
    </div>
  )
}

