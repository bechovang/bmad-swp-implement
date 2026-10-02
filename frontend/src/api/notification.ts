import { apiClient } from './client'
import type {
  ListEnvelope,
  NotificationDto,
  UnreadCountResponse,
} from '../types/notification'

export async function getUnreadCount(): Promise<UnreadCountResponse> {
  const response = await apiClient.get<UnreadCountResponse>('/notifications/unread-count')
  return response.data
}

export async function getNotifications(
  page: number = 1,
  pageSize: number = 25
): Promise<ListEnvelope<NotificationDto>> {
  const response = await apiClient.get<ListEnvelope<NotificationDto>>('/notifications', {
    params: { page, pageSize },
  })
  return response.data
}

export async function markAsRead(id: number): Promise<NotificationDto> {
  const response = await apiClient.post<NotificationDto>(`/notifications/${id}/read`)
  return response.data
}

export async function markAllAsRead(): Promise<UnreadCountResponse> {
  const response = await apiClient.post<UnreadCountResponse>('/notifications/mark-all-read')
  return response.data
}
