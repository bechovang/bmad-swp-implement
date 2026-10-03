import { apiClient } from './client'
import type {
  CheckoutRequestDto,
  CreateCheckoutRequest,
  CheckoutTaskDetailDto,
  SubmitInspectionRequest,
} from '../types/checkout'

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

export async function getCheckoutTaskDetail(taskId: number): Promise<CheckoutTaskDetailDto> {
  const res = await apiClient.get<CheckoutTaskDetailDto>(
    `/checkout-tasks/${taskId}`
  )
  return res.data
}

export async function getInspectionsByReservation(reservationId: number): Promise<CheckoutTaskDetailDto> {
  const res = await apiClient.get<CheckoutTaskDetailDto>(
    `/reservations/${reservationId}/inspections`
  )
  return res.data
}

export async function submitInspection(
  reservationId: number,
  data: SubmitInspectionRequest
): Promise<CheckoutTaskDetailDto> {
  const res = await apiClient.post<CheckoutTaskDetailDto>(
    `/reservations/${reservationId}/inspections`,
    data
  )
  return res.data
}
