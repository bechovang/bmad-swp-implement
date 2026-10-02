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

export async function getMyReservations(): Promise<ReservationDto[]> {
  const response = await apiClient.get<ReservationDto[]>('/reservations/my')
  return response.data
}

export async function getReservationAccessCode(
  id: number | string
): Promise<import('../types/reservation').AccessCodeResponseDto> {
  const response = await apiClient.get<import('../types/reservation').AccessCodeResponseDto>(`/reservations/${id}/access-code`)
  return response.data
}

