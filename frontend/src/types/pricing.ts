export interface SurchargeItemDto {
  type: string
  name: string
  amount: number
  description?: string | null
}

export interface PricingBreakdownDto {
  unitCode: string
  durationMonths: number
  monthlyRate: number
  baseRent: number
  surcharges: SurchargeItemDto[]
  totalRent: number
  depositRate: number
  depositAmount: number
  depositRefundable: boolean
  totalDueNow: number
  currency: string
  policyVersion: string
}

export interface PricingCalculateParams {
  unitCode: string
  durationMonths: number
  startDate?: string
}
