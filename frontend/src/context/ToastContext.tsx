import { useCallback, useState, useEffect } from 'react'
import type { ReactNode } from 'react'
import { Toast } from '../components/ui/Toast'
import type { ToastNotification, ToastTone } from '../types/notification'
import { ToastContext } from './toast-context-base'
export type { ToastContextType } from './toast-context-base'
export { ToastContext } from './toast-context-base'

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<ToastNotification[]>([])

  const dismissToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const clearAllToasts = useCallback(() => {
    setToasts([])
  }, [])

  const showToast = useCallback(
    (toastInput: ToastNotification | string, tone: ToastTone = 'info'): string => {
      if (!toastInput) {
        return ''
      }

      const id =
        typeof toastInput === 'object' && toastInput.id
          ? toastInput.id
          : `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`

      const newToast: ToastNotification =
        typeof toastInput === 'string'
          ? { id, title: toastInput, tone }
          : {
              id,
              title: toastInput.title,
              tone: toastInput.tone || tone,
              deepLink: toastInput.deepLink,
              actionLabel: toastInput.actionLabel,
            }

      // Limit active toast stack to max 5 items to avoid viewport overflow
      setToasts((prev) => [...prev.slice(-4), newToast])
      return id
    },
    []
  )

  useEffect(() => {
    const handleToastEvent = (e: Event) => {
      const customEvent = e as CustomEvent<ToastNotification>
      if (customEvent.detail) {
        showToast(customEvent.detail)
      }
    }
    window.addEventListener('storagehub:toast', handleToastEvent)
    return () => window.removeEventListener('storagehub:toast', handleToastEvent)
  }, [showToast])

  return (
    <ToastContext.Provider value={{ showToast, dismissToast, clearAllToasts, toasts }}>
      {children}
      <div
        aria-label="Notifications"
        className="fixed bottom-4 right-4 z-50 flex flex-col gap-2 pointer-events-none"
      >
        {toasts.map((toast) => (
          <div key={toast.id} className="pointer-events-auto">
            <Toast toast={toast} onDismiss={dismissToast} />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  )
}
