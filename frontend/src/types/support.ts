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

export type EscalationDecision =
  | 'PENDING'
  | 'MAINTENANCE_RELOCATE'
  | 'RETURN_TO_STAFF'
  | 'SEVERE'
  | 'NOT_SEVERE'

export interface SeverityDecisionRequest {
  decision: EscalationDecision
  managerNote: string
  targetUnitId?: number | null
}

export interface EscalationDto {
  id: number
  ticketId: number
  ticketCode: string
  customerId: number
  customerName: string
  unitId: number
  unitCode: string
  reservationId?: number | null
  reservationCode?: string | null
  incidentType: IncidentType
  ticketStatus: SupportTicketStatus
  ticketDescription: string
  escalatedByStaffId: number
  escalatedByStaffName: string
  escalationNote: string
  managerId?: number | null
  managerName?: string | null
  decision: EscalationDecision
  managerNote?: string | null
  relocatedToUnitId?: number | null
  relocatedToUnitCode?: string | null
  newAccessCode?: string | null
  createdAt: string
  resolvedAt?: string | null
}

