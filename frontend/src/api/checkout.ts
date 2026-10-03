import { apiClient } from './client'
import type { CheckoutRequestDto, CreateCheckoutRequest } from '../types/checkout'

export async function requestCheckout(
  reservationId: number,
  data: CreateCheckoutRequest
): Promise<CheckoutRequestDto> {
  const res = await apiClient.post<CheckoutRequestDto>(
    `/reservations/${reservationId}/checkout-request`,
    data
  )
  return res.data
}

export async function getCheckoutRequest(reservationId: number): Promise<CheckoutRequestDto> {
  const res = await apiClient.get<CheckoutRequestDto>(
    `/reservations/${reservationId}/checkout-request`
  )
  return res.data
}
