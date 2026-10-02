export type TaskType = 'CHECK_IN' | 'CHECKOUT' | 'CLEANING' | 'SUPPORT' | 'CONTRACT'

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE'

export interface TaskDto {
  id: number
  type: TaskType
  refCode?: string | null
  assignedStaffId: number
  assignedStaffName?: string | null
  workDate: string
  status: TaskStatus
  unitCode?: string | null
  customerName?: string | null
  dueDate?: string | null
  timeSlot?: string | null
  title?: string | null
  description?: string | null
}

export interface CreateTaskRequest {
  type: TaskType
  refCode?: string | null
  assignedStaffId?: number | null
  workDate: string
  status?: TaskStatus | null
  unitCode?: string | null
  customerName?: string | null
  dueDate?: string | null
  timeSlot?: string | null
  title?: string | null
  description?: string | null
}

export interface UpdateTaskStatusRequest {
  status: TaskStatus
  reason?: string | null
}

export interface TaskTypeConfig {
  type: TaskType
  label: string
  barColor: string
  tintBg: string
  borderColor: string
  textColor: string
}

export const TASK_TYPE_CONFIG: Record<TaskType, TaskTypeConfig> = {
  CHECK_IN: {
    type: 'CHECK_IN',
    label: 'Check-in',
    barColor: '#4F46E5', // Indigo
    tintBg: '#EEF2FF',
    borderColor: '#C7D2FE',
    textColor: '#3730A3',
  },
  CHECKOUT: {
    type: 'CHECKOUT',
    label: 'Checkout',
    barColor: '#0284C7', // Sky
    tintBg: '#F0F9FF',
    borderColor: '#BAE6FD',
    textColor: '#0369A1',
  },
  CLEANING: {
    type: 'CLEANING',
    label: 'Cleaning',
    barColor: '#F59E0B', // Amber
    tintBg: '#FFFBEB',
    borderColor: '#FDE68A',
    textColor: '#B45309',
  },
  SUPPORT: {
    type: 'SUPPORT',
    label: 'Support',
    barColor: '#DC2626', // Red
    tintBg: '#FEF2F2',
    borderColor: '#FECACA',
    textColor: '#B91C1C',
  },
  CONTRACT: {
    type: 'CONTRACT',
    label: 'Contract',
    barColor: '#475569', // Slate
    tintBg: '#F1F5F9',
    borderColor: '#CBD5E1',
    textColor: '#334155',
  },
}

export interface ValidateCheckInRequest {
  reservationCode: string
}

export interface CheckInValidationDto {
  valid: boolean
  errorCode?: string | null
  errorMessage?: string | null
  taskId: number
  reservationId?: number | null
  reservationCode?: string | null
  customerName?: string | null
  unitCode?: string | null
  depositAmountPaid?: number | null
  totalRentDue?: number | null
  depositReceiptCode?: string | null
  rentPaid: boolean
  rentReceiptCode?: string | null
  status?: string | null
}

export interface CheckInActivationDto {
  accessCode: string
  reservationCode: string
  reservationId: number
  unitCode: string
  reservationStatus: string
  unitStatus: string
  taskStatus: string
}

