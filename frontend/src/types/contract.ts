export type ContractStatus =
  | 'DRAFT'
  | 'PRINTED'
  | 'SIGNED'
  | 'ACTIVE'
  | 'CLOSED'
  | 'SUPERSEDED'

export interface ContractContentSnapshot {
  code: string
  reservationCode: string
  unitCode: string
  monthlyRate: number
  baseRent: number
  totalRent: number
  depositAmount: number
  depositRate: number
  durationMonths: number
  policyVersion: string
  currency: string
  startDate?: string
  endDate?: string
  customerName?: string
  customerEmail?: string
  customerPhone?: string
  facilityName?: string
  facilityAddress?: string
  zoneCode?: string
  floor?: number
  sizeM2?: number
}

export interface ContractDto {
  id: number
  code: string
  reservationId: number
  reservationCode?: string | null
  policyId?: number | null
  policyVersion?: string | null
  contentSnapshot: string
  snapshot?: ContractContentSnapshot | null
  signedPhotoUrl?: string | null
  status: ContractStatus
  supersedesContractId?: number | null
  isLatest: number
}

export interface SignContractRequest {
  signedPhotoUrl: string
}

export interface AttachmentUploadResponseDto {
  fileUrl: string
  fileName: string
  size: number
  contentType: string
}

