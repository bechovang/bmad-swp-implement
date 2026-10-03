import { apiClient } from './client'
import type {
  SettlementPreviewDto,
  FinalizeSettlementRequest,
  SettlementReceiptDto,
} from '../types/settlement'

export async function getSettlementPreview(
  reservationId: number,
  params?: {
    damageFee?: number
    damageReason?: string
    waiverAmount?: number
    waiverReason?: string
    checkoutDate?: string
  }
): Promise<SettlementPreviewDto> {
  const res = await apiClient.get<SettlementPreviewDto>(
    `/reservations/${reservationId}/settlement-preview`,
    { params }
  )
  return res.data
}

export async function postSettlementPreview(
  reservationId: number,
  data?: FinalizeSettlementRequest
): Promise<SettlementPreviewDto> {
  const res = await apiClient.post<SettlementPreviewDto>(
    `/reservations/${reservationId}/settlement-preview`,
    data
  )
  return res.data
}

export async function finalizeSettlement(
  reservationId: number,
  data: FinalizeSettlementRequest
): Promise<SettlementReceiptDto> {
  const res = await apiClient.post<SettlementReceiptDto>(
    `/reservations/${reservationId}/settlement`,
    data
  )
  return res.data
}

export async function getSettlementReceipt(reservationId: number): Promise<SettlementReceiptDto> {
  const res = await apiClient.get<SettlementReceiptDto>(
    `/reservations/${reservationId}/settlement`
  )
  return res.data
}
