export type ReservationStatus =
  | 'PENDING_PAYMENT'
  | 'RESERVED'
  | 'CHECKED_IN'
  | 'CHECKOUT_REQUESTED'
  | 'CLOSED'
  | 'EXPIRED'
  | 'CANCELLED'

export interface CreateReservationRequest {
  unitCode: string
  startDate: string
  durationMonths: number
}

export interface ReservationDto {
  id: number
  code: string
  customerId: number
  customerName?: string | null
  unitId: number
  unitCode: string
  unitTypeName?: string | null
  facilityName?: string | null
  facilityAddress?: string | null
  zoneCode?: string | null
  floor?: number | null
  sizeM2?: number | null
  accessType?: string | null
  startDate: string
  endDate: string
  durationMonths: number
  depositAmount: number
  monthlyRate: number
  baseRent: number
  totalRent: number
  policyVersion: string
  accessCode?: string | null
  status: ReservationStatus
  payments?: import('./rental').PaymentDto[]
}

export interface AccessCodeResponseDto {
  accessCode: string
  accessType: string
  unitCode: string
}


