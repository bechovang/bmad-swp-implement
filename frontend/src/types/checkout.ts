export type CheckoutRequestStatus = 'PENDING' | 'DONE' | 'CANCELLED'

export interface CreateCheckoutRequest {
  requestedDate: string
  notes?: string | null
}

export interface CheckoutRequestDto {
  id: number
  reservationId: number
  reservationCode: string
  unitId?: number | null
  unitCode: string
  requestedDate: string
  status: CheckoutRequestStatus
  notes?: string | null
  createdAt: string
  updatedAt?: string | null
}
