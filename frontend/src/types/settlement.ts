export interface SettlementPreviewDto {
  reservationId: number
  reservationCode: string
  unitCode?: string | null
  customerName?: string | null
  depositHeld: number
  damageFee: number
  damageReason?: string | null
  lateFee: number
  daysLate: number
  waiverAmount: number
  waiverReason?: string | null
  waiverCap: number
  policyVersion?: string | null
  waiverExceeded: boolean
  waiverReasonRequired: boolean
  totalCharges: number
  refundAmount: number
  extraFeeAmount: number
  extraFeeRequired: boolean
  extraFeePaid: boolean
  damageReasonRequired: boolean
  canFinalize: boolean
  summaryMessage?: string
}

export interface FinalizeSettlementRequest {
  damageFee?: number
  damageReason?: string | null
  lateFee?: number
  waiverAmount?: number
  waiverReason?: string | null
  paymentMethod?: 'CASH' | 'PAYOS' | string | null
  cashReceived?: boolean
  notes?: string | null
}

export interface SettlementReceiptDto {
  id: number
  receiptCode: string
  reservationId: number
  reservationCode?: string | null
  unitCode?: string | null
  customerName?: string | null
  staffName?: string | null
  depositHeld: number
  damageFee: number
  damageReason?: string | null
  lateFee: number
  waiverAmount: number
  waiverReason?: string | null
  totalCharges: number
  refundAmount: number
  extraFeeAmount: number
  status: string
  notes?: string | null
  createdAt: string
  summaryMessage?: string
}
