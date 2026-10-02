export type Role =
  | 'CUSTOMER'
  | 'STAFF'
  | 'FACILITY_MANAGER'
  | 'BUSINESS_OPS'
  | 'SYSTEM_ADMINISTRATOR'

export interface AuthUser {
  id: number
  fullName: string
  email: string
  role: Role
  phone?: string | null
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  user: AuthUser
}

export interface RegisterRequest {
  fullName: string
  phone: string
  email: string
  password: string
  confirmPassword: string
  agreeToTerms: boolean
}

export interface RegisterResponse {
  id: number
  fullName: string
  email: string
  role: Role
  phone?: string | null
}

export interface ForgotPasswordRequest {
  email: string
}

export interface ForgotPasswordResponse {
  message: string
}

export interface FieldError {
  field: string
  message: string
}

export interface ApiError {
  code: string
  message: string
  fieldErrors?: FieldError[]
}
