import type { ReservationStatus } from './reservation'

export type { ReservationStatus }

export type PaymentStatus =
  | 'PENDING'
  | 'PENDING_CASH'
  | 'PROCESSING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'EXPIRED'

export type PaymentPurpose =
  | 'DEPOSIT'
  | 'RENT'
  | 'EXTENSION_FEE'
  | 'DAMAGE_FEE'
  | 'EXTRA_FEE'

export type PaymentMethod =
  | 'PAYOS'
  | 'CASH'
  | 'CARD'
  | 'MOMO'
  | 'VNPAY'

export interface PaymentDto {
  id: number
  receiptCode: string
  payerId: number
  reservationId?: number | null
  orderCode?: number | null
  purpose: PaymentPurpose
  method: PaymentMethod
  amount: number
  status: PaymentStatus
  createdAt?: string | null
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
  payments?: PaymentDto[]
}

export type RentalTab = 'active' | 'reservations' | 'history'
