import { apiClient } from './client'
import type { ContractDto } from '../types/contract'

export async function getContract(id: number | string): Promise<ContractDto> {
  const response = await apiClient.get<ContractDto>(`/contracts/${id}`)
  return response.data
}

export async function getContractByReservation(reservationId: number | string): Promise<ContractDto> {
  const response = await apiClient.get<ContractDto>(`/contracts/reservation/${reservationId}`)
  return response.data
}

export async function reDraftContract(id: number | string): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/re-draft`)
  return response.data
}
