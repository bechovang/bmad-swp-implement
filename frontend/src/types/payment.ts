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

export interface CreatePaymentRequest {
  reservationId: number
  purpose: PaymentPurpose
  method: PaymentMethod
  amount?: number
}

export interface PaymentResponseDto {
  paymentId: number
  orderCode: number
  amount: number
  status: PaymentStatus
  method: PaymentMethod
  purpose: PaymentPurpose
  checkoutUrl?: string | null
  qrCode?: string | null
  expiresAt?: string | null
}

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

export type PaymentModalState =
  | 'METHOD_SELECT'
  | 'AWAITING'
  | 'SUCCESS'
  | 'FAILED'
  | 'EXPIRED'

export interface PaymentModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  reservationId: number
  unitCode: string
  amount: number
  purpose?: PaymentPurpose
  allowedMethods?: PaymentMethod[]
  onSuccess?: (payment: PaymentDto | PaymentResponseDto) => void
}
