import { apiClient } from './client'
import type { CreatePaymentRequest, PaymentResponseDto, PaymentDto } from '../types/payment'

export async function createPaymentLink(
  data: CreatePaymentRequest
): Promise<PaymentResponseDto> {
  const response = await apiClient.post<PaymentResponseDto>('/payments/create', data)
  return response.data
}

export async function getPaymentStatus(id: number | string): Promise<PaymentDto> {
  const response = await apiClient.get<PaymentDto>(`/payments/${id}`)
  return response.data
}

export async function cancelPayment(id: number | string): Promise<void> {
  try {
    await apiClient.post(`/payments/${id}/cancel`)
  } catch {
    // Gracefully handle if cancel endpoint is optional / simulated
  }
}
