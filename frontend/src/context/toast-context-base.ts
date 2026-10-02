import { createContext } from 'react'
import type { ToastNotification, ToastTone } from '../types/notification'

export interface ToastContextType {
  showToast: (toast: ToastNotification | string, tone?: ToastTone) => string
  dismissToast: (id: string) => void
  clearAllToasts: () => void
  toasts: ToastNotification[]
}

export const ToastContext = createContext<ToastContextType | undefined>(undefined)
