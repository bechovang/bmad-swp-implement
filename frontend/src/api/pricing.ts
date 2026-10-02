import { apiClient } from './client'
import type { PricingBreakdownDto, PricingCalculateParams } from '../types/pricing'

export async function calculatePricing(params: PricingCalculateParams): Promise<PricingBreakdownDto> {
  const response = await apiClient.get<PricingBreakdownDto>('/pricing/calculate', {
    params: {
      unitCode: params.unitCode,
      durationMonths: params.durationMonths,
      startDate: params.startDate,
    },
  })
  return response.data
}
