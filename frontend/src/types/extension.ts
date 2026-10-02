export interface ExtensionBoundaryDto {
  rentalId: number
  unitCode: string
  currentEndDate: string
  latestPossibleCheckoutDate: string | null
  conflictStartDate: string | null
  conflictReservationCode: string | null
  isExtendable: boolean
  message: string | null
}

export interface ExtensionQuoteRequest {
  newEndDate: string
}

export interface ExtensionQuoteDto {
  rentalId: number
  unitCode: string
  currentEndDate: string
  newEndDate: string
  additionalDays: number
  additionalMonths: number
  monthlyRate: number
  additionalRent: number
  currentHeldDeposit: number
  newTotalDepositRequired: number
  depositTopUp: number
  totalFee: number
  currency: string
  policyVersion?: string | null
}
