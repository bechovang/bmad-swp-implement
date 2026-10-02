export type UnitStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'RENTED'
  | 'PREPARING'
  | 'MAINTENANCE'
  | 'RETIRED'

export interface BrowseUnitDto {
  id: number
  code: string
  typeName: string
  typeDescription: string | null
  zoneCode: string
  facilityName: string
  floor: number
  sizeM2: number
  accessType: string
  status: UnitStatus | string
  imageUrl: string
  monthlyRate: number
  depositRate: number
  availabilityStatus: string
  availableFromDate: string
  isImmediatelyAvailable: boolean
  isInCleaningBuffer: boolean
}

export type BrowseUnit = BrowseUnitDto

export interface BrowseUnitsResponse {
  items: BrowseUnitDto[]
  totalAvailable: number
  totalUnits: number
}

export interface UnitDetailDto {
  id: number
  code: string
  typeName: string
  typeDescription: string | null
  facilityName: string
  facilityAddress: string
  zoneCode: string
  floor: number
  sizeM2: number
  accessType: string
  status: UnitStatus | string
  imageUrl: string
  monthlyRate: number
  depositRate: number
  securityFeatures: string[]
}

export interface BrowseFilter {
  type?: string
  size?: string
  startDate?: string
  durationMonths?: number
}


