export type IncidentType =
  | 'LOST_ACCESS'
  | 'DEVICE_ISSUE'
  | 'SECURITY'
  | 'CLEANLINESS'
  | 'OTHER'

export type SupportTicketStatus =
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'RESOLVED'
  | 'ESCALATED'

export interface CreateSupportTicketRequest {
  unitId: number
  incidentType: IncidentType
  description: string
}

export interface ResolveSupportTicketRequest {
  note?: string
}

export interface EscalateSupportTicketRequest {
  note: string
}

export interface SupportTicketDto {
  id: number
  code: string
  customerId: number
  customerName: string
  unitId: number
  unitCode: string
  reservationId?: number | null
  reservationCode?: string | null
  incidentType: IncidentType
  status: SupportTicketStatus
  description: string
  assignedStaffId?: number | null
  assignedStaffName?: string | null
  resolutionNote?: string | null
  escalationNote?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}
