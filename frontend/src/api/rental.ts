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

export async function getExtensionBoundary(
  id: number | string
): Promise<import('../types/extension').ExtensionBoundaryDto> {
  const response = await apiClient.get<import('../types/extension').ExtensionBoundaryDto>(
    `/reservations/${id}/extension-boundary`
  )
  return response.data
}

export async function getExtensionQuote(
  id: number | string,
  request: import('../types/extension').ExtensionQuoteRequest
): Promise<import('../types/extension').ExtensionQuoteDto> {
  const response = await apiClient.post<import('../types/extension').ExtensionQuoteDto>(
    `/reservations/${id}/extension-quote`,
    request
  )
  return response.data
}
