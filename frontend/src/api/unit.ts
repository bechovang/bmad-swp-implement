import { apiClient } from './client'
import type { UnitDetailDto, BrowseUnitsResponse, BrowseFilter } from '../types/unit'

export async function getUnitDetail(code: string): Promise<UnitDetailDto> {
  const response = await apiClient.get<UnitDetailDto>(`/units/${encodeURIComponent(code)}`)
  return response.data
}

export async function browseUnits(filters?: BrowseFilter): Promise<BrowseUnitsResponse> {
  const params: Record<string, string | number> = {}
  if (filters?.type && filters.type !== 'all') {
    params.type = filters.type
  }
  if (filters?.size && filters.size !== 'all') {
    params.size = filters.size
  }
  if (filters?.startDate) {
    params.startDate = filters.startDate
  }
  if (filters?.durationMonths) {
    params.durationMonths = filters.durationMonths
  }

  const response = await apiClient.get<BrowseUnitsResponse>('/units/browse', { params })
  return response.data
}

