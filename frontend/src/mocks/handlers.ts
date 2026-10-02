import { http, HttpResponse } from 'msw'
import type {
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
  ForgotPasswordResponse,
  ApiError,
  AuthUser,
} from '../types/auth'

export const DEMO_USERS: Record<string, AuthUser & { password: string }> = {
  'lan@storagehub.dev': {
    id: 1,
    fullName: 'Lan Nguyen',
    email: 'lan@storagehub.dev',
    role: 'CUSTOMER',
    phone: '0901234567',
    password: 'Demo1234!',
  },
  'staff@storagehub.dev': {
    id: 2,
    fullName: 'Minh Tran',
    email: 'staff@storagehub.dev',
    role: 'STAFF',
    phone: '0902345678',
    password: 'Demo1234!',
  },
  'manager@storagehub.dev': {
    id: 3,
    fullName: 'Hoa Pham',
    email: 'manager@storagehub.dev',
    role: 'FACILITY_MANAGER',
    phone: '0903456789',
    password: 'Demo1234!',
  },
  'ops@storagehub.dev': {
    id: 4,
    fullName: 'Quoc Le',
    email: 'ops@storagehub.dev',
    role: 'BUSINESS_OPS',
    phone: '0904567890',
    password: 'Demo1234!',
  },
  'admin@storagehub.dev': {
    id: 5,
    fullName: 'An Hoang',
    email: 'admin@storagehub.dev',
    role: 'SYSTEM_ADMINISTRATOR',
    phone: '0905678901',
    password: 'Demo1234!',
  },
}

export const handlers = [
  // ------------------------------------------------------------ Auth endpoints
  http.post('/api/v1/auth/login', async ({ request }) => {
    const body = (await request.json()) as LoginRequest
    const found = DEMO_USERS[body.email?.toLowerCase()]

    if (found && found.password === body.password) {
      const { password: _, ...user } = found
      const response: LoginResponse = {
        token: `mock-jwt-token-for-${user.role.toLowerCase()}-${user.id}`,
        user,
      }
      return HttpResponse.json(response, { status: 200 })
    }

    // Spec: Thất bại (401) chỉ hiển thị đúng MỘT generic inline error:
    // "The email or password is not correct.", tuyệt đối không tiết lộ field nào sai.
    const errorEnvelope: ApiError = {
      code: 'UNAUTHENTICATED',
      message: 'The email or password is not correct.',
    }
    return HttpResponse.json(errorEnvelope, { status: 401 })
  }),

  http.post('/api/v1/auth/register', async ({ request }) => {
    const body = (await request.json()) as RegisterRequest

    // Check duplicate email
    if (
      body.email?.toLowerCase() === 'lan@storagehub.dev' ||
      body.email?.toLowerCase() === 'existing@storagehub.dev' ||
      Boolean(DEMO_USERS[body.email?.toLowerCase()])
    ) {
      const errorEnvelope: ApiError = {
        code: 'VALIDATION_FAILED',
        message: 'Validation failed',
        fieldErrors: [{ field: 'email', message: 'Email is already registered.' }],
      }
      return HttpResponse.json(errorEnvelope, { status: 400 })
    }

    if (body.password !== body.confirmPassword) {
      const errorEnvelope: ApiError = {
        code: 'VALIDATION_FAILED',
        message: 'Validation failed',
        fieldErrors: [{ field: 'confirmPassword', message: 'Passwords do not match.' }],
      }
      return HttpResponse.json(errorEnvelope, { status: 400 })
    }

    if (!body.agreeToTerms) {
      const errorEnvelope: ApiError = {
        code: 'VALIDATION_FAILED',
        message: 'Validation failed',
        fieldErrors: [
          { field: 'agreeToTerms', message: 'You must agree to the terms and privacy policy.' },
        ],
      }
      return HttpResponse.json(errorEnvelope, { status: 400 })
    }

    // Successfully created customer account
    const response: RegisterResponse = {
      id: Math.floor(Math.random() * 1000) + 10,
      fullName: body.fullName,
      email: body.email,
      role: 'CUSTOMER',
      phone: body.phone,
    }

    // Register this user into mock demo users so subsequent login succeeds
    DEMO_USERS[body.email.toLowerCase()] = {
      ...response,
      password: body.password,
    }

    return HttpResponse.json(response, { status: 201 })
  }),

  http.post('/api/v1/auth/forgot-password', async () => {
    // Spec: Always answers the same generic 200 whether or not the email matches
    const response: ForgotPasswordResponse = {
      message: 'If an account exists with that email, a password reset link has been sent.',
    }
    return HttpResponse.json(response, { status: 200 })
  }),

  // ------------------------------------------------------------ Units endpoint
  http.get('/api/v1/units', () => {
    return HttpResponse.json({
      items: [],
      page: 1,
      pageSize: 25,
      total: 0,
    })
  }),
]
