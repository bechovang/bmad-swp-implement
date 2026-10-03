import axios from 'axios'
import type {
  CreateSupportTicketRequest,
  EscalateSupportTicketRequest,
  ResolveSupportTicketRequest,
  SupportTicketDto,
  SupportTicketStatus,
} from '../types/support'

export async function createSupportTicket(data: CreateSupportTicketRequest): Promise<SupportTicketDto> {
  const token = localStorage.getItem('token')
  const res = await axios.post<SupportTicketDto>('/api/v1/support-tickets', data, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return res.data
}

export async function getSupportTickets(params?: {
  status?: SupportTicketStatus
  unitCode?: string
}): Promise<SupportTicketDto[]> {
  const token = localStorage.getItem('token')
  const res = await axios.get<SupportTicketDto[]>('/api/v1/support-tickets', {
    params,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return res.data
}

export async function getSupportTicketById(id: number): Promise<SupportTicketDto> {
  const token = localStorage.getItem('token')
  const res = await axios.get<SupportTicketDto>(`/api/v1/support-tickets/${id}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return res.data
}

export async function resolveSupportTicket(
  id: number,
  data: ResolveSupportTicketRequest,
): Promise<SupportTicketDto> {
  const token = localStorage.getItem('token')
  const res = await axios.post<SupportTicketDto>(`/api/v1/support-tickets/${id}/resolve`, data, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return res.data
}

export async function escalateSupportTicket(
  id: number,
  data: EscalateSupportTicketRequest,
): Promise<SupportTicketDto> {
  const token = localStorage.getItem('token')
  const res = await axios.post<SupportTicketDto>(`/api/v1/support-tickets/${id}/escalate`, data, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  return res.data
}

