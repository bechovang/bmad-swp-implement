import { apiClient } from './client'
import type {
  ContractDto,
  SignContractRequest,
  AttachmentUploadResponseDto,
} from '../types/contract'

export async function getContract(id: number | string): Promise<ContractDto> {
  const response = await apiClient.get<ContractDto>(`/contracts/${id}`)
  return response.data
}

export async function getContractByReservation(reservationId: number | string): Promise<ContractDto> {
  const response = await apiClient.get<ContractDto>(`/contracts/reservation/${reservationId}`)
  return response.data
}

export async function getContractChain(reservationId: number | string): Promise<ContractDto[]> {
  const response = await apiClient.get<ContractDto[]>(`/contracts/reservation/${reservationId}/chain`)
  return response.data
}

export async function printContract(id: number | string): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/print`)
  return response.data
}

export async function signContract(id: number | string, data: SignContractRequest): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/sign`, data)
  return response.data
}

export async function uploadAttachment(file: File): Promise<AttachmentUploadResponseDto> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<AttachmentUploadResponseDto>('/attachments', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  return response.data
}

export async function reDraftContract(id: number | string): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/re-draft`)
  return response.data
}

export async function expireContract(id: number | string, reason?: string): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/expire`, { reason })
  return response.data
}

export async function voidContract(id: number | string, reason: string): Promise<ContractDto> {
  const response = await apiClient.post<ContractDto>(`/contracts/${id}/void`, { reason })
  return response.data
}

