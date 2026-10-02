import { apiClient } from './client'
import type { CreateReservationRequest, ReservationDto } from '../types/reservation'

export async function createReservation(request: CreateReservationRequest): Promise<ReservationDto> {
  const response = await apiClient.post<ReservationDto>('/reservations', request)
  return response.data
}

export async function getReservation(id: number): Promise<ReservationDto> {
  const response = await apiClient.get<ReservationDto>(`/reservations/${id}`)
  return response.data
}
