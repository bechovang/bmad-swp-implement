import { apiClient } from './client'
import type { UnitDetailDto } from '../types/unit'

export async function getUnitDetail(code: string): Promise<UnitDetailDto> {
  const response = await apiClient.get<UnitDetailDto>(`/units/${encodeURIComponent(code)}`)
  return response.data
}
