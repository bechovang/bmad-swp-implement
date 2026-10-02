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

  // ------------------------------------------------------------ Notifications endpoints (1.6)
  http.get('/api/v1/notifications/unread-count', ({ request }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { code: 'UNAUTHENTICATED', message: 'Full authentication is required to access this resource' },
        { status: 401 }
      )
    }

    const currentUserId = extractUserId(authHeader)
    const count = mockNotifications.filter(
      (n) => n.userId === currentUserId && !n.isRead
    ).length
    return HttpResponse.json({ count }, { status: 200 })
  }),

  http.get('/api/v1/notifications', ({ request }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { code: 'UNAUTHENTICATED', message: 'Full authentication is required to access this resource' },
        { status: 401 }
      )
    }

    const currentUserId = extractUserId(authHeader)
    const url = new URL(request.url)
    const page = Math.max(1, parseInt(url.searchParams.get('page') || '1', 10))
    const pageSize = Math.max(1, parseInt(url.searchParams.get('pageSize') || '25', 10))

    const userNotifications = mockNotifications
      .filter((n) => n.userId === currentUserId)
      .sort((a, b) => {
        if (a.isRead !== b.isRead) {
          return a.isRead ? 1 : -1
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      })

    const start = (page - 1) * pageSize
    const items = userNotifications.slice(start, start + pageSize)

    return HttpResponse.json(
      {
        items,
        page,
        pageSize,
        total: userNotifications.length,
      },
      { status: 200 }
    )
  }),

  http.post('/api/v1/notifications/:id/read', ({ request, params }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { code: 'UNAUTHENTICATED', message: 'Full authentication is required to access this resource' },
        { status: 401 }
      )
    }

    const currentUserId = extractUserId(authHeader)
    const id = parseInt(params.id as string, 10)
    const item = mockNotifications.find((n) => n.id === id)

    if (!item) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: 'Notification not found' },
        { status: 404 }
      )
    }

    if (item.userId !== currentUserId) {
      return HttpResponse.json(
        { code: 'FORBIDDEN', message: 'Forbidden' },
        { status: 403 }
      )
    }

    item.isRead = true
    return HttpResponse.json(item, { status: 200 })
  }),

  http.post('/api/v1/notifications/mark-all-read', ({ request }) => {
    const authHeader = request.headers.get('Authorization')
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return HttpResponse.json(
        { code: 'UNAUTHENTICATED', message: 'Full authentication is required to access this resource' },
        { status: 401 }
      )
    }

    const currentUserId = extractUserId(authHeader)
    mockNotifications.forEach((n) => {
      if (n.userId === currentUserId) {
        n.isRead = true
      }
    })

    return HttpResponse.json({ count: 0 }, { status: 200 })
  }),

  // ------------------------------------------------------------ Units & Pricing (2.1)
  http.get('/api/v1/units/:code', ({ params }) => {
    const code = (params.code as string)?.toUpperCase()
    const unit = MOCK_UNITS[code]

    if (!unit) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Unit ${code} not found`,
        },
        { status: 404 }
      )
    }

    return HttpResponse.json(unit, { status: 200 })
  }),

  http.get('/api/v1/pricing/calculate', ({ request }) => {
    const url = new URL(request.url)
    const unitCode = url.searchParams.get('unitCode')?.toUpperCase() || ''
    const durationParam = url.searchParams.get('durationMonths')
    const durationMonths = durationParam ? parseInt(durationParam, 10) : 0

    if (!durationMonths || durationMonths < 1 || durationMonths > 120) {
      return HttpResponse.json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Duration must be between 1 and 120 months',
          fieldErrors: [{ field: 'durationMonths', message: 'Duration must be between 1 and 120 months' }],
        },
        { status: 400 }
      )
    }

    const unit = MOCK_UNITS[unitCode]
    if (!unit) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Unit ${unitCode} not found`,
        },
        { status: 404 }
      )
    }

    const monthlyRate = unit.monthlyRate
    const depositRate = unit.depositRate
    const baseRent = monthlyRate * durationMonths
    const depositAmount = Math.round((baseRent * depositRate) / 100)

    return HttpResponse.json(
      {
        unitCode: unit.code,
        durationMonths,
        monthlyRate,
        baseRent,
        surcharges: [],
        totalRent: baseRent,
        depositRate,
        depositAmount,
        depositRefundable: true,
        totalDueNow: depositAmount,
        currency: 'VND',
        policyVersion: 'v3',
      },
      { status: 200 }
    )
  }),
]

export const MOCK_UNITS: Record<string, import('../types/unit').UnitDetailDto> = {
  'S-3': {
    id: 1,
    code: 'S-3',
    typeName: 'S',
    typeDescription: 'Small unit around 5 m2',
    facilityName: 'Tan Binh Depot',
    facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
    zoneCode: 'A',
    floor: 1,
    sizeM2: 5.0,
    accessType: 'PIN',
    status: 'AVAILABLE',
    imageUrl: '/units/S-3.jpg',
    monthlyRate: 345000,
    depositRate: 10,
    securityFeatures: [
      '24/7 CCTV Monitoring',
      'Personal Access Code (PIN)',
      'Individually Alarmed Unit',
      'Climate & Humidity Controlled',
      'Fire Protection & Sprinklers',
    ],
  },
  'M-2': {
    id: 2,
    code: 'M-2',
    typeName: 'M',
    typeDescription: 'Medium unit around 8 m2',
    facilityName: 'Tan Binh Depot',
    facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
    zoneCode: 'B',
    floor: 1,
    sizeM2: 8.0,
    accessType: 'QR',
    status: 'MAINTENANCE',
    imageUrl: '/units/M-2.jpg',
    monthlyRate: 690000,
    depositRate: 10,
    securityFeatures: [
      '24/7 CCTV Monitoring',
      'Personal Access Code (QR)',
      'Individually Alarmed Unit',
      'Climate & Humidity Controlled',
      'Fire Protection & Sprinklers',
    ],
  },
  'M-5': {
    id: 3,
    code: 'M-5',
    typeName: 'M',
    typeDescription: 'Medium unit around 8 m2',
    facilityName: 'Tan Binh Depot',
    facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
    zoneCode: 'B',
    floor: 2,
    sizeM2: 8.0,
    accessType: 'QR',
    status: 'AVAILABLE',
    imageUrl: '/units/M-5.jpg',
    monthlyRate: 690000,
    depositRate: 10,
    securityFeatures: [
      '24/7 CCTV Monitoring',
      'Personal Access Code (QR)',
      'Individually Alarmed Unit',
      'Climate & Humidity Controlled',
      'Fire Protection & Sprinklers',
    ],
  },
}


export interface MockNotification {
  id: number
  userId: number
  type: string
  title: string
  deepLink?: string | null
  isRead: boolean
  createdAt: string
}

export const INITIAL_NOTIFICATIONS: MockNotification[] = [
  {
    id: 1,
    userId: 1,
    type: 'PAYMENT_SUCCEEDED',
    title: 'Deposit received for Unit S-04',
    deepLink: '/rentals/1',
    isRead: false,
    createdAt: '2026-10-02T10:00:00Z',
  },
  {
    id: 2,
    userId: 1,
    type: 'RESERVATION_CONFIRMED',
    title: 'Reservation confirmed for Unit M-12',
    deepLink: '/rentals/2',
    isRead: true,
    createdAt: '2026-10-01T15:30:00Z',
  },
  {
    id: 3,
    userId: 2,
    type: 'TASK_ASSIGNED',
    title: 'New cleaning task assigned: Unit L-01',
    deepLink: '/tasks/3',
    isRead: false,
    createdAt: '2026-10-02T08:15:00Z',
  },
]

let mockNotifications: MockNotification[] = JSON.parse(JSON.stringify(INITIAL_NOTIFICATIONS))

export function resetMockNotifications(custom?: MockNotification[]) {
  mockNotifications = custom
    ? JSON.parse(JSON.stringify(custom))
    : JSON.parse(JSON.stringify(INITIAL_NOTIFICATIONS))
}

function extractUserId(authHeader: string): number {
  const token = authHeader.replace(/^Bearer\s+/, '').trim()
  const match = token.match(/-(\d+)$/)
  if (match) {
    return parseInt(match[1], 10)
  }
  if (typeof window !== 'undefined' && window.localStorage) {
    try {
      const stored =
        localStorage.getItem('storagehub_user') ||
        localStorage.getItem('storagehub_auth_user')
      if (stored) {
        const u = JSON.parse(stored)
        if (u && typeof u.id === 'number') {
          return u.id
        }
      }
    } catch {
      // ignore
    }
  }
  return 1
}

