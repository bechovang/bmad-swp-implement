export type ToastTone = 'info' | 'success' | 'warning' | 'error'

export interface ToastNotification {
  id?: string
  title: string
  tone?: ToastTone
  deepLink?: string | null
  actionLabel?: string
}

export interface NotificationDto {
  id: number
  userId: number
  type: string
  title: string
  deepLink?: string | null
  isRead: boolean
  createdAt: string
}

export interface UnreadCountResponse {
  count: number
}

export interface ListEnvelope<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}
