import { apiClient } from './client'
import type { ReservationDto } from '../types/rental'

export async function getMyReservations(): Promise<ReservationDto[]> {
  const response = await apiClient.get<ReservationDto[]>('/reservations/my')
  return response.data
}

export async function getRentalDetail(id: number | string): Promise<ReservationDto> {
  const response = await apiClient.get<ReservationDto>(`/reservations/${id}`)
  return response.data
}
