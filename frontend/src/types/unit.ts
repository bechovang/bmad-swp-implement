export type UnitStatus =
  | 'AVAILABLE'
  | 'RESERVED'
  | 'RENTED'
  | 'PREPARING'
  | 'MAINTENANCE'
  | 'RETIRED'

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
