import { apiClient } from './client'
import type { TaskDto, CreateTaskRequest, UpdateTaskStatusRequest, TaskType, TaskStatus, ValidateCheckInRequest, CheckInValidationDto } from '../types/task'

export interface TaskQueryParams {
  workDate?: string
  type?: TaskType
  status?: TaskStatus
  assignedStaffId?: number
}

export async function getTasks(params?: TaskQueryParams): Promise<TaskDto[]> {
  const response = await apiClient.get<TaskDto[]>('/tasks', { params })
  return response.data
}

export async function getTaskById(id: number | string): Promise<TaskDto> {
  const response = await apiClient.get<TaskDto>(`/tasks/${id}`)
  return response.data
}

export async function updateTaskStatus(
  id: number | string,
  request: UpdateTaskStatusRequest
): Promise<TaskDto> {
  const response = await apiClient.patch<TaskDto>(`/tasks/${id}/status`, request)
  return response.data
}

export async function createTask(request: CreateTaskRequest): Promise<TaskDto> {
  const response = await apiClient.post<TaskDto>('/tasks', request)
  return response.data
}

export async function validateCheckInReservation(
  taskId: number | string,
  request: ValidateCheckInRequest
): Promise<CheckInValidationDto> {
  const response = await apiClient.post<CheckInValidationDto>(`/tasks/${taskId}/validate-reservation`, request)
  return response.data
}

export async function activateCheckIn(
  taskId: number | string
): Promise<import('../types/task').CheckInActivationDto> {
  const response = await apiClient.post<import('../types/task').CheckInActivationDto>(`/tasks/${taskId}/activate-checkin`)
  return response.data
}

