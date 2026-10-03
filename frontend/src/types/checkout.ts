export type CheckoutRequestStatus = 'PENDING' | 'DONE' | 'CANCELLED'

export type InspectionItem = 'ACCESS_CARD' | 'PADLOCK' | 'CLEANLINESS' | 'STRUCTURE'

export type InspectionResult = 'OK' | 'MINOR' | 'MAJOR'

export interface CreateCheckoutRequest {
  requestedDate: string
  notes?: string | null
}

export interface CheckoutRequestDto {
  id: number
  reservationId: number
  reservationCode: string
  unitId?: number | null
  unitCode: string
  requestedDate: string
  status: CheckoutRequestStatus
  notes?: string | null
  keyReturned?: boolean
  unitEmptied?: boolean
  createdAt: string
  updatedAt?: string | null
}

export interface InspectionItemInput {
  item: InspectionItem
  result: InspectionResult
  note?: string | null
}

export interface SubmitInspectionRequest {
  items: InspectionItemInput[]
  keyReturned?: boolean
  unitEmptied?: boolean
  generalNotes?: string | null
}

export interface InspectionItemDto {
  id: number
  item: InspectionItem
  result: InspectionResult
  note?: string | null
  inspectorStaffId?: number | null
  inspectorStaffName?: string | null
  createdAt: string
}

export interface CheckoutTaskDetailDto {
  taskId?: number | null
  taskStatus?: string | null
  reservationId: number
  reservationCode: string
  customerId?: number | null
  customerName?: string | null
  customerPhone?: string | null
  unitId?: number | null
  unitCode: string
  requestedDate?: string | null
  keyReturned: boolean
  unitEmptied: boolean
  inspections: InspectionItemDto[]
  hasMajorDamage: boolean
  majorItems: InspectionItem[]
}
