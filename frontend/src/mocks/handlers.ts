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
import type { CreateReservationRequest, ReservationDto, AccessCodeResponseDto } from '../types/reservation'
import type {
  CreatePaymentRequest,
  PaymentResponseDto,
  PaymentDto,
  PaymentStatus,
} from '../types/payment'
import type { ContractDto, SignContractRequest } from '../types/contract'
import type {
  TaskDto,
  CreateTaskRequest,
  UpdateTaskStatusRequest,
  ValidateCheckInRequest,
  CheckInValidationDto,
  CheckInActivationDto,
} from '../types/task'
import type {
  ExtensionBoundaryDto,
  ExtensionQuoteRequest,
  ExtensionQuoteDto,
} from '../types/extension'
import type {
  SupportTicketDto,
  CreateSupportTicketRequest,
} from '../types/support'

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

  // ------------------------------------------------------------ Units & Pricing (2.1, 2.2)
  http.get('/api/v1/units/browse', ({ request }) => {
    const url = new URL(request.url)
    const type = url.searchParams.get('type')
    const size = url.searchParams.get('size')
    const startDate = url.searchParams.get('startDate')

    let filtered = [...MOCK_BROWSE_UNITS]

    if (startDate) {
      filtered = filtered.filter((u) => u.availableFromDate <= startDate)
    }

    if (type && type.toLowerCase() !== 'all') {
      filtered = filtered.filter((u) => u.typeName.toLowerCase() === type.toLowerCase())
    }

    if (size && size.toLowerCase() !== 'all') {
      const sizeLower = size.toLowerCase()
      filtered = filtered.filter((u) => {
        if (sizeLower === 'small') return u.sizeM2 <= 5.0
        if (sizeLower === 'medium') return u.sizeM2 > 5.0 && u.sizeM2 <= 10.0
        if (sizeLower === 'large') return u.sizeM2 > 10.0
        return (
          u.sizeM2.toString() === size ||
          `${u.sizeM2} m2`.toLowerCase() === sizeLower ||
          `${u.sizeM2} m²`.toLowerCase() === sizeLower
        )
      })
    }

    return HttpResponse.json(
      {
        items: filtered,
        totalAvailable: filtered.length,
        totalUnits: MOCK_BROWSE_UNITS.length,
      },
      { status: 200 }
    )
  }),

  http.get('/api/v1/units/:code', ({ params }) => {
    const code = (params.code as string)?.toUpperCase()
    if (code === 'BROWSE') return undefined
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

  // ------------------------------------------------------------ Reservations (2.3)
  http.post('/api/v1/reservations', async ({ request }) => {
    const body = (await request.json()) as CreateReservationRequest
    const unitCode = body.unitCode?.toUpperCase() || ''

    if (unitCode === 'M-2' || unitCode === 'CONFLICT' || unitCode === 'OCCUPIED') {
      return HttpResponse.json(
        {
          code: 'UNIT_UNAVAILABLE',
          message: `Unit ${unitCode} was just reserved. Similar units still available.`,
        },
        { status: 409 }
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

    const durationMonths = body.durationMonths || 1
    const startDate = body.startDate || '2026-10-03'
    const start = new Date(startDate)
    const end = new Date(start)
    end.setMonth(end.getMonth() + durationMonths)
    const endDate = end.toISOString().split('T')[0]

    const monthlyRate = unit.monthlyRate
    const baseRent = monthlyRate * durationMonths
    const depositAmount = Math.round((baseRent * unit.depositRate) / 100)

    const reservationId = mockReservationsList.length + 1
    const resDto: ReservationDto = {
      id: reservationId,
      code: `BK-2026-${String(reservationId).padStart(4, '0')}`,
      customerId: 1,
      customerName: 'Lan Nguyen',
      unitId: unit.id,
      unitCode: unit.code,
      unitTypeName: unit.typeName,
      facilityName: unit.facilityName,
      facilityAddress: unit.facilityAddress,
      zoneCode: unit.zoneCode,
      floor: unit.floor,
      sizeM2: unit.sizeM2,
      accessType: unit.accessType,
      startDate,
      endDate,
      durationMonths,
      depositAmount,
      monthlyRate,
      baseRent,
      totalRent: baseRent,
      policyVersion: 'v3',
      accessCode: null,
      status: 'PENDING_PAYMENT',
    }

    mockReservationsList.push(resDto)
    return HttpResponse.json(resDto, { status: 201 })
  }),

  http.get('/api/v1/reservations/my', () => {
    return HttpResponse.json(mockReservationsList, { status: 200 })
  }),

  http.get('/api/v1/reservations/:id', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const res = mockReservationsList.find((r) => r.id === id)
    if (!res) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Reservation ${id} not found`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(res, { status: 200 })
  }),

  // ------------------------------------------------------------ Payments (2.4, 2.6)
  http.post('/api/v1/payments/create', async ({ request }) => {
    const body = (await request.json()) as CreatePaymentRequest
    const reservationId = body.reservationId
    const res = mockReservationsList.find((r) => r.id === reservationId)

    const amount = body.amount || (res ? res.depositAmount : 103500)
    const maxId = mockPaymentsList.reduce((max, p) => Math.max(max, p.id), 100)
    const paymentId = maxId + 1
    const orderCode = 1727932800000 + paymentId

    const newPayment: PaymentDto = {
      id: paymentId,
      receiptCode: `RC-${orderCode}`,
      payerId: 1,
      reservationId,
      orderCode,
      purpose: body.purpose || 'DEPOSIT',
      method: body.method || 'PAYOS',
      amount,
      status: body.method === 'CASH' ? 'PENDING_CASH' : 'PENDING',
      createdAt: new Date().toISOString(),
      newEndDate: body.newEndDate || null,
    }

    mockPaymentsList.push(newPayment)

    if (body.method === 'CASH' && body.purpose === 'EXTENSION_FEE' && res) {
      const existingTask = mockTasksList.find(
        (t) => t.type === 'CONTRACT' && t.refCode?.startsWith('CT-')
      )
      if (!existingTask) {
        mockTasksList.push({
          id: mockTasksList.length + 1,
          type: 'CONTRACT',
          refCode: `CT-${res.code}-A1`,
          assignedStaffId: 2,
          assignedStaffName: 'Minh Tran',
          workDate: new Date().toISOString().split('T')[0],
          status: 'TODO',
          unitCode: res.unitCode,
          customerName: res.customerName,
          dueDate: new Date(Date.now() + 7 * 86400000).toISOString().split('T')[0],
          timeSlot: 'Morning',
          title: `Contract Signature CT-${res.code}-A1`,
          description: `Collect extension cash (${amount} VND) and sign addendum`,
        })
      }
    }

    const response: PaymentResponseDto = {
      paymentId: newPayment.id,
      orderCode,
      amount,
      status: newPayment.status,
      method: newPayment.method,
      purpose: newPayment.purpose,
      checkoutUrl: `https://pay.payos.vn/web/${orderCode}`,
      qrCode: `00020101021238540010A000000727012400069704150110${orderCode}5303704540${amount}5802VN62150811StorageHub6304`,
      expiresAt: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
    }

    return HttpResponse.json(response, { status: 201 })
  }),

  http.get('/api/v1/payments/:id', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const payment = mockPaymentsList.find((p) => p.id === id)
    if (!payment) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Payment ${id} not found`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(payment, { status: 200 })
  }),

  http.post('/api/v1/payments/:id/cancel', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const payment = mockPaymentsList.find((p) => p.id === id)
    if (payment) {
      payment.status = 'EXPIRED'
    }
    return HttpResponse.json({ success: true }, { status: 200 })
  }),

  http.post('/api/v1/payments/:id/confirm-cash', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const payment = mockPaymentsList.find((p) => p.id === id)
    if (!payment) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Payment ${id} not found`,
        },
        { status: 404 }
      )
    }

    payment.status = 'SUCCEEDED'

    if (payment.reservationId) {
      const res = mockReservationsList.find((r) => r.id === payment.reservationId)
      if (res) {
        if (payment.purpose === 'EXTENSION_FEE' && payment.newEndDate) {
          res.endDate = payment.newEndDate
          res.depositAmount = (res.depositAmount || 207000) + 71300
          res.totalRent = (res.totalRent || 2070000) + 713000
          if (!res.payments) res.payments = []
          if (!res.payments.find((x) => x.id === payment.id)) {
            res.payments.push(payment)
          }

          // Draft addendum in mockContractsList
          const addendumCode = `CT-${res.code}-A1`
          if (!mockContractsList.find((c) => c.code === addendumCode)) {
            mockContractsList.push({
              id: mockContractsList.length + 1,
              code: addendumCode,
              reservationId: res.id,
              reservationCode: res.code,
              policyId: 1,
              policyVersion: 'v3',
              contentSnapshot: JSON.stringify({
                code: addendumCode,
                reservationCode: res.code,
                unitCode: res.unitCode,
                newEndDate: payment.newEndDate,
                monthlyRate: res.monthlyRate,
                totalRent: res.totalRent,
                depositAmount: res.depositAmount,
              }),
              status: 'AWAITING_SIGNATURE',
              supersedesContractId: 2,
              isLatest: 1,
            })
          }
        } else {
          res.status = 'RESERVED'
        }
      }
    }

    return HttpResponse.json(payment, { status: 200 })
  }),

  // ------------------------------------------------------------ Contracts (3.1)
  http.get('/api/v1/contracts/:id', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const contract = mockContractsList.find((c) => c.id === id)
    if (!contract) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Contract ${id} not found`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(contract, { status: 200 })
  }),

  http.get('/api/v1/contracts/reservation/:reservationId', ({ params }) => {
    const reservationId = parseInt(params.reservationId as string, 10)
    const contract = mockContractsList.find(
      (c) => c.reservationId === reservationId && c.isLatest === 1
    ) || mockContractsList.find((c) => c.reservationId === reservationId)
    if (!contract) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `No contract found for reservation ${reservationId}`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(contract, { status: 200 })
  }),

  http.post('/api/v1/contracts/:id/re-draft', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const prevContract = mockContractsList.find((c) => c.id === id)
    if (!prevContract) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Contract ${id} not found`,
        },
        { status: 404 }
      )
    }

    if (prevContract.status !== 'DRAFT') {
      return HttpResponse.json(
        {
          code: 'INVALID_CONTRACT_STATUS',
          message: 'Only DRAFT contracts can be re-drafted',
        },
        { status: 409 }
      )
    }

    // Supersede old contract
    prevContract.status = 'SUPERSEDED'
    prevContract.isLatest = 0

    // Create new draft
    const newId = Math.max(...mockContractsList.map((c) => c.id), 0) + 1
    const newCode = `${prevContract.code.replace(/-R\d+$/, '')}-R${newId}`
    const newContract: ContractDto = {
      ...prevContract,
      id: newId,
      code: newCode,
      status: 'DRAFT',
      supersedesContractId: prevContract.id,
      isLatest: 1,
    }
    mockContractsList.push(newContract)

    return HttpResponse.json(newContract, { status: 200 })
  }),

  http.post('/api/v1/contracts/:id/print', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const contract = mockContractsList.find((c) => c.id === id)
    if (!contract) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Contract ${id} not found` }, { status: 404 })
    }
    if (contract.status === 'DRAFT') {
      contract.status = 'PRINTED'
    }
    return HttpResponse.json(contract, { status: 200 })
  }),

  http.post('/api/v1/contracts/:id/sign', async ({ params, request }) => {
    const id = parseInt(params.id as string, 10)
    const contract = mockContractsList.find((c) => c.id === id)
    if (!contract) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Contract ${id} not found` }, { status: 404 })
    }

    const body = (await request.json()) as SignContractRequest
    if (!body || !body.signedPhotoUrl || !body.signedPhotoUrl.trim()) {
      return HttpResponse.json({ code: 'VALIDATION_FAILED', message: 'Signed photo URL is required' }, { status: 400 })
    }

    contract.signedPhotoUrl = body.signedPhotoUrl.trim()
    contract.status = 'SIGNED'

    return HttpResponse.json(contract, { status: 200 })
  }),

  http.post('/api/v1/contracts/:id/expire', async ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const contract = mockContractsList.find((c) => c.id === id)
    if (!contract) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Contract ${id} not found` }, { status: 404 })
    }
    contract.status = 'EXPIRED'
    return HttpResponse.json(contract, { status: 200 })
  }),

  http.post('/api/v1/contracts/:id/void', async ({ params, request }) => {
    const id = parseInt(params.id as string, 10)
    const contract = mockContractsList.find((c) => c.id === id)
    if (!contract) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Contract ${id} not found` }, { status: 404 })
    }
    const body = (await request.json().catch(() => ({}))) as { reason?: string }
    if (!body?.reason || !body.reason.trim()) {
      return HttpResponse.json({ code: 'REASON_REQUIRED', message: 'Void reason is mandatory' }, { status: 400 })
    }
    contract.status = 'VOIDED'
    return HttpResponse.json(contract, { status: 200 })
  }),

  http.get('/api/v1/contracts/reservation/:reservationId/chain', ({ params }) => {
    const reservationId = parseInt(params.reservationId as string, 10)
    const contracts = mockContractsList
      .filter((c) => c.reservationId === reservationId)
      .sort((a, b) => a.id - b.id)

    return HttpResponse.json(contracts, { status: 200 })
  }),

  // ------------------------------------------------------------ Attachments (AD-10)
  http.post('/api/v1/attachments', async () => {
    const mockFileUrl = `/api/v1/attachments/signed-contract-${Date.now()}.jpg`
    return HttpResponse.json(
      {
        fileUrl: mockFileUrl,
        fileName: `signed-contract-${Date.now()}.jpg`,
        size: 204800,
        contentType: 'image/jpeg',
      },
      { status: 201 }
    )
  }),

  http.get('/api/v1/attachments/:filename', () => {
    return new HttpResponse(new Uint8Array([0xff, 0xd8, 0xff, 0xe0]), {
      status: 200,
      headers: { 'Content-Type': 'image/jpeg' },
    })
  }),

  // ------------------------------------------------------------ Tasks (3.2)
  http.get('/api/v1/tasks', ({ request }) => {
    const url = new URL(request.url)
    const workDate = url.searchParams.get('workDate')
    const type = url.searchParams.get('type')
    const status = url.searchParams.get('status')
    const assignedStaffIdStr = url.searchParams.get('assignedStaffId')
    const assignedStaffId = assignedStaffIdStr ? parseInt(assignedStaffIdStr, 10) : null

    let result = [...mockTasksList]

    if (workDate) {
      result = result.filter((t) => t.workDate === workDate)
    }
    if (type) {
      result = result.filter((t) => t.type === type)
    }
    if (status) {
      result = result.filter((t) => t.status === status)
    }
    if (assignedStaffId) {
      result = result.filter((t) => t.assignedStaffId === assignedStaffId)
    }

    return HttpResponse.json(result, { status: 200 })
  }),

  http.get('/api/v1/tasks/:id', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const task = mockTasksList.find((t) => t.id === id)
    if (!task) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Task ${id} not found`,
        },
        { status: 404 }
      )
    }
    return HttpResponse.json(task, { status: 200 })
  }),

  http.patch('/api/v1/tasks/:id/status', async ({ params, request }) => {
    const id = parseInt(params.id as string, 10)
    const task = mockTasksList.find((t) => t.id === id)
    if (!task) {
      return HttpResponse.json(
        {
          code: 'NOT_FOUND',
          message: `Task ${id} not found`,
        },
        { status: 404 }
      )
    }

    const body = (await request.json()) as UpdateTaskStatusRequest
    if (!body || !body.status) {
      return HttpResponse.json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Status is required',
          fieldErrors: [{ field: 'status', message: 'Status must not be null' }],
        },
        { status: 400 }
      )
    }

    if (body.status === 'DONE' && task.type === 'CHECK_IN') {
      const resCode = task.refCode
      const reservation = mockReservationsList.find((r) => r.code === resCode || (resCode === 'BK-1042' && r.id === 1))
      if (reservation) {
        const rentPayment = mockPaymentsList.find(
          (p) => p.reservationId === reservation.id && p.purpose === 'RENT' && p.status === 'SUCCEEDED'
        )
        if (!rentPayment) {
          return HttpResponse.json(
            {
              code: 'CLOSING_STEP_MISSING',
              message: 'Rent payment is still pending on this check-in.',
              missingStep: 'RENT_PAYMENT_PENDING',
              stepLabel: 'Rent payment is still pending on this check-in.',
            },
            { status: 409 }
          )
        }

        const contract = mockContractsList.find(
          (c) => (c.reservationId === reservation.id || c.reservationCode === reservation.code) && (c.status === 'SIGNED' || c.status === 'ACTIVE')
        )
        if (!contract) {
          return HttpResponse.json(
            {
              code: 'CLOSING_STEP_MISSING',
              message: 'Contract signature is required before completing check-in.',
              missingStep: 'CONTRACT_UNSIGNED',
              stepLabel: 'Contract signature is required before completing check-in.',
            },
            { status: 409 }
          )
        }

        if (reservation.status !== 'CHECKED_IN') {
          return HttpResponse.json(
            {
              code: 'CLOSING_STEP_MISSING',
              message: 'Access code handover and check-in activation are required before completing check-in.',
              missingStep: 'CHECKIN_NOT_ACTIVATED',
              stepLabel: 'Access code handover and check-in activation are required before completing check-in.',
            },
            { status: 409 }
          )
        }
      }
    }

    task.status = body.status
    return HttpResponse.json(task, { status: 200 })
  }),

  http.post('/api/v1/tasks', async ({ request }) => {
    const body = (await request.json()) as CreateTaskRequest
    const newId = Math.max(...mockTasksList.map((t) => t.id), 0) + 1

    const newTask: TaskDto = {
      id: newId,
      type: body.type,
      refCode: body.refCode || null,
      assignedStaffId: body.assignedStaffId || 2,
      assignedStaffName: 'Minh Tran',
      workDate: body.workDate,
      status: body.status || 'TODO',
      unitCode: body.unitCode || null,
      customerName: body.customerName || null,
      dueDate: body.dueDate || body.workDate,
      timeSlot: body.timeSlot || 'Morning',
      title: body.title || `${body.type} Task`,
      description: body.description || null,
    }

    mockTasksList.push(newTask)
    return HttpResponse.json(newTask, { status: 201 })
  }),

  // Check-in task validation (3.3)
  http.post('/api/v1/tasks/:id/validate-reservation', async ({ params, request }) => {
    const id = parseInt(params.id as string, 10)
    const task = mockTasksList.find((t) => t.id === id)
    if (!task) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: `Task ${id} not found` },
        { status: 404 }
      )
    }

    const body = (await request.json()) as ValidateCheckInRequest
    if (!body || !body.reservationCode || !body.reservationCode.trim()) {
      return HttpResponse.json(
        { code: 'VALIDATION_FAILED', message: 'Reservation code is required' },
        { status: 400 }
      )
    }

    const code = body.reservationCode.trim()
    if (code === 'BK-UNPAID') {
      return HttpResponse.json(
        { code: 'DEPOSIT_UNPAID', message: `Cannot proceed to check-in because deposit is not paid for reservation ${code}` },
        { status: 409 }
      )
    }

    const reservation = mockReservationsList.find((r) => r.code === code || (code === 'BK-1042' && r.id === 1))
    if (!reservation) {
      return HttpResponse.json(
        { code: 'NOT_FOUND', message: `Reservation ${code} not found` },
        { status: 404 }
      )
    }

    if (reservation.status === 'PENDING_PAYMENT') {
      return HttpResponse.json(
        { code: 'DEPOSIT_UNPAID', message: `Deposit has not been paid for reservation ${code}` },
        { status: 409 }
      )
    }

    if (reservation.status === 'EXPIRED') {
      return HttpResponse.json(
        { code: 'RESERVATION_EXPIRED', message: `Reservation ${code} has expired` },
        { status: 409 }
      )
    }

    if (reservation.status !== 'RESERVED') {
      return HttpResponse.json(
        { code: 'INVALID_RESERVATION_STATUS', message: `Reservation ${code} is in ${reservation.status} status and cannot be checked in` },
        { status: 409 }
      )
    }

    const depositPayment = mockPaymentsList.find(
      (p) => p.reservationId === reservation.id && p.purpose === 'DEPOSIT' && p.status === 'SUCCEEDED'
    )

    if (!depositPayment) {
      return HttpResponse.json(
        { code: 'DEPOSIT_UNPAID', message: `Deposit has not been paid for reservation ${code}` },
        { status: 409 }
      )
    }

    const rentPayment = mockPaymentsList.find(
      (p) => p.reservationId === reservation.id && p.purpose === 'RENT' && p.status === 'SUCCEEDED'
    )

    const response: CheckInValidationDto = {
      valid: true,
      errorCode: null,
      errorMessage: null,
      taskId: id,
      reservationId: reservation.id,
      reservationCode: reservation.code,
      customerName: reservation.customerName,
      unitCode: reservation.unitCode,
      depositAmountPaid: depositPayment.amount,
      totalRentDue: reservation.totalRent || (depositPayment.amount * 10),
      depositReceiptCode: depositPayment.receiptCode,
      rentPaid: Boolean(rentPayment),
      rentReceiptCode: rentPayment ? rentPayment.receiptCode : null,
      status: reservation.status,
    }

    return HttpResponse.json(response, { status: 200 })
  }),

  // Activate check-in & reveal access code (Story 3.4)
  http.post('/api/v1/tasks/:id/activate-checkin', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const task = mockTasksList.find((t) => t.id === id)
    if (!task) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Task ${id} not found` }, { status: 404 })
    }

    const code = task.refCode || 'BK-1042'
    const res = mockReservationsList.find((r) => r.code === code || (code === 'BK-1042' && r.id === 1))
    if (!res) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Reservation ${code} not found` }, { status: 404 })
    }

    const accessCode = res.accessCode || '482913'
    res.accessCode = accessCode
    res.status = 'CHECKED_IN'

    const contract = mockContractsList.find((c) => c.reservationId === res.id && c.isLatest === 1)
    if (contract) {
      contract.status = 'ACTIVE'
    }

    task.status = 'DONE'

    const response: CheckInActivationDto = {
      accessCode,
      reservationCode: res.code,
      reservationId: res.id,
      unitCode: res.unitCode,
      reservationStatus: 'CHECKED_IN',
      unitStatus: 'RENTED',
      taskStatus: 'DONE',
    }

    return HttpResponse.json(response, { status: 200 })
  }),

  http.get('/api/v1/reservations/:id/access-code', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const res = mockReservationsList.find((r) => r.id === id)
    if (!res) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Reservation ${id} not found` }, { status: 404 })
    }

    const response: AccessCodeResponseDto = {
      accessCode: res.accessCode || '482913',
      accessType: res.accessType || 'PIN',
      unitCode: res.unitCode,
    }

    return HttpResponse.json(response, { status: 200 })
  }),

  // Extension Boundary & Quote (Story 4.1)
  http.get('/api/v1/reservations/:id/extension-boundary', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const res = mockReservationsList.find((r) => r.id === id)
    if (!res) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Reservation ${id} not found` }, { status: 404 })
    }

    // Check conflict against mock upcoming reservations on same unit
    let latestPossibleCheckoutDate: string | null = null
    let conflictStartDate: string | null = null
    let conflictReservationCode: string | null = null

    // For Unit M-5 or custom tests: if there's a conflict scheduled
    if (res.unitCode === 'M-5' || res.unitCode === 'S-3-CONFLICT') {
      conflictStartDate = '2027-01-15'
      conflictReservationCode = 'BK-UPCOMING-99'
      const d = new Date(conflictStartDate)
      d.setDate(d.getDate() - 1)
      latestPossibleCheckoutDate = d.toISOString().split('T')[0]
    }

    const response: ExtensionBoundaryDto = {
      rentalId: res.id,
      unitCode: res.unitCode,
      currentEndDate: res.endDate,
      latestPossibleCheckoutDate,
      conflictStartDate,
      conflictReservationCode,
      isExtendable: res.status === 'CHECKED_IN',
      message: latestPossibleCheckoutDate
        ? `Upcoming reservation starts ${conflictStartDate}. Latest checkout is ${latestPossibleCheckoutDate}.`
        : 'No upcoming reservation conflict for this unit.',
    }

    return HttpResponse.json(response, { status: 200 })
  }),

  http.post('/api/v1/reservations/:id/extension-quote', async ({ params, request }) => {
    const id = parseInt(params.id as string, 10)
    const res = mockReservationsList.find((r) => r.id === id)
    if (!res) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Reservation ${id} not found` }, { status: 404 })
    }

    const body = (await request.json()) as ExtensionQuoteRequest
    const newEndDate = body.newEndDate

    // Check conflict boundary
    if (
      (res.unitCode === 'M-5' || res.unitCode === 'S-3-CONFLICT') &&
      newEndDate > '2027-01-14'
    ) {
      return HttpResponse.json(
        {
          code: 'EXTENSION_DATE_CONFLICT',
          message: `Can't extend to ${newEndDate} — ${res.unitCode} has a reservation starting 2027-01-15. Latest possible checkout is 2027-01-14. Pick another date.`,
        },
        { status: 409 }
      )
    }

    const currentEnd = new Date(res.endDate)
    const newEnd = new Date(newEndDate)
    const diffTime = newEnd.getTime() - currentEnd.getTime()
    const additionalDays = Math.max(1, Math.round(diffTime / (1000 * 60 * 60 * 24)))
    const monthlyRate = res.monthlyRate || 690000
    const additionalRent = Math.round((monthlyRate / 30) * additionalDays)
    const currentHeldDeposit = res.depositAmount || 207000
    const totalRentAfter = (res.baseRent || 2070000) + additionalRent
    const newTotalDepositRequired = Math.round(totalRentAfter * 0.1)
    const depositTopUp = Math.max(0, newTotalDepositRequired - currentHeldDeposit)
    const totalFee = additionalRent + depositTopUp

    const quote: ExtensionQuoteDto = {
      rentalId: res.id,
      unitCode: res.unitCode,
      currentEndDate: res.endDate,
      newEndDate,
      additionalDays,
      additionalMonths: Math.ceil(additionalDays / 30),
      monthlyRate,
      additionalRent,
      currentHeldDeposit,
      newTotalDepositRequired,
      depositTopUp,
      totalFee,
      currency: 'VND',
      policyVersion: res.policyVersion || 'v3',
    }

    return HttpResponse.json(quote, { status: 200 })
  }),

  // ------------------------------------------------------------ Support (Story 5.1)
  http.get('/api/v1/support-tickets', ({ request }) => {
    const url = new URL(request.url)
    const status = url.searchParams.get('status')
    const unitCode = url.searchParams.get('unitCode')

    let list = [...mockSupportTicketsList]
    if (status) {
      list = list.filter((t) => t.status === status)
    }
    if (unitCode) {
      list = list.filter((t) => t.unitCode === unitCode)
    }
    return HttpResponse.json(list, { status: 200 })
  }),

  http.get('/api/v1/support-tickets/:id', ({ params }) => {
    const id = parseInt(params.id as string, 10)
    const ticket = mockSupportTicketsList.find((t) => t.id === id)
    if (!ticket) {
      return HttpResponse.json({ code: 'NOT_FOUND', message: `Support ticket ${id} not found` }, { status: 404 })
    }
    return HttpResponse.json(ticket, { status: 200 })
  }),

  http.post('/api/v1/support-tickets', async ({ request }) => {
    const body = (await request.json()) as CreateSupportTicketRequest

    if (!body.description || !body.description.trim()) {
      return HttpResponse.json(
        {
          code: 'VALIDATION_FAILED',
          message: 'Validation failed',
          fieldErrors: [{ field: 'description', message: 'description must not be blank' }],
        },
        { status: 400 }
      )
    }

    const unit = MOCK_UNITS[body.unitId === 2 ? 'M-2' : body.unitId === 3 ? 'M-5' : 'S-3']
    const code = `SR-${String(mockSupportTicketsList.length + 34).padStart(4, '0')}`

    const newTicket: SupportTicketDto = {
      id: mockSupportTicketsList.length + 10,
      code,
      customerId: 1,
      customerName: 'Lan Nguyen',
      unitId: body.unitId,
      unitCode: unit ? unit.code : 'S-3',
      reservationId: 1,
      reservationCode: 'BK-1042',
      incidentType: body.incidentType,
      status: 'OPEN',
      description: body.description.trim(),
      assignedStaffId: 2,
      assignedStaffName: 'Minh Tran',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    }

    mockSupportTicketsList.unshift(newTicket)

    // Also push to mockTasksList
    mockTasksList.push({
      id: mockTasksList.length + 1,
      type: 'SUPPORT',
      refCode: code,
      assignedStaffId: 2,
      assignedStaffName: 'Minh Tran',
      workDate: new Date().toISOString().split('T')[0],
      status: 'TODO',
      unitCode: newTicket.unitCode,
      customerName: 'Lan Nguyen',
      dueDate: new Date().toISOString().split('T')[0],
      timeSlot: 'Morning',
      title: `Support Ticket ${code}`,
      description: `${body.incidentType}: ${body.description.trim()}`,
    })

    return HttpResponse.json(newTicket, { status: 201 })
  }),
]

export const INITIAL_SUPPORT_TICKETS: SupportTicketDto[] = [
  {
    id: 1,
    code: 'SR-0032',
    customerId: 1,
    customerName: 'Lan Nguyen',
    unitId: 1,
    unitCode: 'S-3',
    reservationId: 1,
    reservationCode: 'BK-1042',
    incidentType: 'DEVICE_ISSUE',
    status: 'RESOLVED',
    description: 'Sticky door latch repaired during morning shift',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    createdAt: '2026-10-06T08:30:00Z',
    updatedAt: '2026-10-06T10:00:00Z',
  },
  {
    id: 2,
    code: 'SR-0033',
    customerId: 1,
    customerName: 'Lan Nguyen',
    unitId: 2,
    unitCode: 'M-2',
    reservationId: null,
    reservationCode: null,
    incidentType: 'OTHER',
    status: 'ESCALATED',
    description: 'Water ingress from ceiling joint in unit M-2',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    createdAt: '2026-10-18T09:00:00Z',
    updatedAt: '2026-10-18T09:30:00Z',
  },
]

let mockSupportTicketsList: SupportTicketDto[] = JSON.parse(JSON.stringify(INITIAL_SUPPORT_TICKETS))

export function resetMockSupportTickets(custom?: SupportTicketDto[]) {
  mockSupportTicketsList = custom ? [...custom] : JSON.parse(JSON.stringify(INITIAL_SUPPORT_TICKETS))
}

export const INITIAL_TASKS: TaskDto[] = [
  {
    id: 1,
    type: 'CHECK_IN',
    refCode: 'BK-1042',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-03',
    status: 'DONE',
    unitCode: 'S-3',
    customerName: 'Lan Nguyen',
    dueDate: '2026-10-03',
    timeSlot: 'Morning',
    title: 'Check-in BK-1042',
    description: 'Customer Lan Nguyen checking into Unit S-3',
  },
  {
    id: 2,
    type: 'CONTRACT',
    refCode: 'CT-1042-A1',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-16',
    status: 'DONE',
    unitCode: 'S-3',
    customerName: 'Lan Nguyen',
    dueDate: '2026-10-16',
    timeSlot: 'Morning',
    title: 'Contract Signature CT-1042-A1',
    description: 'Contract and addendum signing at front desk',
  },
  {
    id: 3,
    type: 'CHECKOUT',
    refCode: 'RT-0871',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-18',
    status: 'DONE',
    unitCode: 'S-3',
    customerName: 'Lan Nguyen',
    dueDate: '2026-10-18',
    timeSlot: 'Morning',
    title: 'Checkout RT-0871',
    description: 'Unit inspection and key return',
  },
  {
    id: 4,
    type: 'CLEANING',
    refCode: 'S-3',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-19',
    status: 'TODO',
    unitCode: 'S-3',
    customerName: null,
    dueDate: '2026-10-19',
    timeSlot: 'Morning',
    title: 'Cleaning S-3',
    description: 'Turnover buffer cleaning and inspection for unit S-3',
  },
  {
    id: 5,
    type: 'SUPPORT',
    refCode: 'SR-0032',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-06',
    status: 'DONE',
    unitCode: 'S-3',
    customerName: 'Lan Nguyen',
    dueDate: '2026-10-06',
    timeSlot: 'Morning',
    title: 'Support Ticket SR-0032',
    description: 'Door hinge inspection and repair',
  },
  {
    id: 6,
    type: 'SUPPORT',
    refCode: 'SR-0033',
    assignedStaffId: 2,
    assignedStaffName: 'Minh Tran',
    workDate: '2026-10-18',
    status: 'IN_PROGRESS',
    unitCode: 'M-2',
    customerName: 'Lan Nguyen',
    dueDate: '2026-10-18',
    timeSlot: 'Morning',
    title: 'Support Ticket SR-0033',
    description: 'Ceiling joint inspection for unit M-2',
  },
]

let mockTasksList: TaskDto[] = JSON.parse(JSON.stringify(INITIAL_TASKS))

export function resetMockTasks(custom?: TaskDto[]) {
  mockTasksList = custom ? [...custom] : JSON.parse(JSON.stringify(INITIAL_TASKS))
}


export const INITIAL_CONTRACTS: ContractDto[] = [
  {
    id: 1,
    code: 'CT-1042',
    reservationId: 1,
    reservationCode: 'BK-1042',
    policyId: 1,
    policyVersion: 'v3',
    contentSnapshot: JSON.stringify({
      code: 'CT-1042',
      reservationCode: 'BK-1042',
      unitCode: 'S-3',
      monthlyRate: 345000,
      baseRent: 1035000,
      totalRent: 1035000,
      depositAmount: 103500,
      depositRate: 10,
      durationMonths: 3,
      policyVersion: 'v3',
      currency: 'VND',
      startDate: '2026-10-05',
      endDate: '2027-01-05',
      customerName: 'Lan Nguyen',
      customerEmail: 'lan@storagehub.dev',
      customerPhone: '0901234567',
      facilityName: 'Tan Binh Depot',
      facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
      zoneCode: 'A',
      floor: 1,
      sizeM2: 5.0,
    }),
    snapshot: {
      code: 'CT-1042',
      reservationCode: 'BK-1042',
      unitCode: 'S-3',
      monthlyRate: 345000,
      baseRent: 1035000,
      totalRent: 1035000,
      depositAmount: 103500,
      depositRate: 10,
      durationMonths: 3,
      policyVersion: 'v3',
      currency: 'VND',
      startDate: '2026-10-05',
      endDate: '2027-01-05',
      customerName: 'Lan Nguyen',
      customerEmail: 'lan@storagehub.dev',
      customerPhone: '0901234567',
      facilityName: 'Tan Binh Depot',
      facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
      zoneCode: 'A',
      floor: 1,
      sizeM2: 5.0,
    },
    signedPhotoUrl: null,
    status: 'DRAFT',
    supersedesContractId: null,
    isLatest: 1,
  },
  {
    id: 2,
    code: 'CT-BK-2026-0002',
    reservationId: 2,
    reservationCode: 'BK-2026-0002',
    policyId: 1,
    policyVersion: 'v3',
    contentSnapshot: JSON.stringify({
      code: 'CT-BK-2026-0002',
      reservationCode: 'BK-2026-0002',
      unitCode: 'M-5',
      monthlyRate: 690000,
      baseRent: 2070000,
      totalRent: 2070000,
      depositAmount: 207000,
      depositRate: 10,
      durationMonths: 3,
      policyVersion: 'v3',
      currency: 'VND',
      startDate: '2026-09-01',
      endDate: '2026-12-01',
      customerName: 'Lan Nguyen',
    }),
    signedPhotoUrl: '/api/v1/attachments/signed-base-2.jpg',
    status: 'SIGNED',
    supersedesContractId: null,
    isLatest: 0,
  },
  {
    id: 3,
    code: 'CT-1042-A1',
    reservationId: 2,
    reservationCode: 'BK-2026-0002',
    policyId: 1,
    policyVersion: 'v3',
    contentSnapshot: JSON.stringify({
      code: 'CT-1042-A1',
      baseContractCode: 'CT-BK-2026-0002',
      reservationCode: 'BK-2026-0002',
      unitCode: 'M-5',
      monthlyRate: 690000,
      totalRent: 2783000,
      depositAmount: 278300,
      signingDeadline: '2026-10-10',
    }),
    signedPhotoUrl: null,
    status: 'AWAITING_SIGNATURE',
    supersedesContractId: 2,
    isLatest: 1,
  },
]

let mockContractsList: ContractDto[] = JSON.parse(JSON.stringify(INITIAL_CONTRACTS))

export function resetMockContracts(custom?: ContractDto[]) {
  mockContractsList = custom ? [...custom] : JSON.parse(JSON.stringify(INITIAL_CONTRACTS))
}

export const INITIAL_PAYMENTS: PaymentDto[] = [
  {
    id: 101,
    receiptCode: 'RC-101',
    payerId: 1,
    reservationId: 1,
    orderCode: 1727932800101,
    purpose: 'DEPOSIT',
    method: 'PAYOS',
    amount: 103500,
    status: 'SUCCEEDED',
    createdAt: '2026-10-02T10:00:00Z',
  },
  {
    id: 102,
    receiptCode: 'RC-102',
    payerId: 1,
    reservationId: 2,
    orderCode: 1727932800102,
    purpose: 'DEPOSIT',
    method: 'PAYOS',
    amount: 207000,
    status: 'SUCCEEDED',
    createdAt: '2026-09-01T08:00:00Z',
  },
  {
    id: 103,
    receiptCode: 'RC-103',
    payerId: 1,
    reservationId: 2,
    orderCode: 1727932800103,
    purpose: 'RENT',
    method: 'CASH',
    amount: 2070000,
    status: 'SUCCEEDED',
    createdAt: '2026-09-01T09:00:00Z',
  },
]

let mockPaymentsList: PaymentDto[] = JSON.parse(JSON.stringify(INITIAL_PAYMENTS))

export function resetMockPayments(custom?: PaymentDto[]) {
  mockPaymentsList = custom ? [...custom] : JSON.parse(JSON.stringify(INITIAL_PAYMENTS))
}

export function setMockPaymentStatus(
  paymentIdOrStatus: number | PaymentStatus,
  maybeStatus?: PaymentStatus
) {
  let targetPayment: PaymentDto | undefined
  let targetStatus: PaymentStatus

  if (typeof paymentIdOrStatus === 'string') {
    targetStatus = paymentIdOrStatus
    targetPayment = mockPaymentsList[mockPaymentsList.length - 1]
  } else {
    targetStatus = maybeStatus || 'SUCCEEDED'
    targetPayment = mockPaymentsList.find((item) => item.id === paymentIdOrStatus)
  }

  if (targetPayment) {
    targetPayment.status = targetStatus
    if (targetStatus === 'SUCCEEDED' && targetPayment.reservationId) {
      const res = mockReservationsList.find((r) => r.id === targetPayment.reservationId)
      if (res) {
        if (targetPayment.purpose === 'EXTENSION_FEE' && targetPayment.newEndDate) {
          res.endDate = targetPayment.newEndDate
          res.depositAmount = (res.depositAmount || 207000) + 71300
          res.totalRent = (res.totalRent || 2070000) + 713000
          const addendumCode = `CT-${res.code}-A1`
          if (!mockContractsList.find((c) => c.code === addendumCode)) {
            mockContractsList.push({
              id: mockContractsList.length + 1,
              code: addendumCode,
              reservationId: res.id,
              reservationCode: res.code,
              policyId: 1,
              policyVersion: 'v3',
              contentSnapshot: JSON.stringify({
                code: addendumCode,
                reservationCode: res.code,
                unitCode: res.unitCode,
                newEndDate: targetPayment.newEndDate,
                monthlyRate: res.monthlyRate,
                totalRent: res.totalRent,
                depositAmount: res.depositAmount,
              }),
              status: 'AWAITING_SIGNATURE',
              supersedesContractId: 2,
              isLatest: 1,
            })
          }
        } else {
          res.status = 'RESERVED'
        }
        if (!res.payments) res.payments = []
        if (!res.payments.find((x) => x.id === targetPayment.id)) {
          res.payments.push(targetPayment)
        }
      }
    }
  }
}

export const INITIAL_RESERVATIONS: ReservationDto[] = [
  {
    id: 1,
    code: 'BK-2026-0001',
    customerId: 1,
    customerName: 'Lan Nguyen',
    unitId: 1,
    unitCode: 'S-3',
    unitTypeName: 'S',
    facilityName: 'Tan Binh Depot',
    facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
    zoneCode: 'A',
    floor: 1,
    sizeM2: 5.0,
    accessType: 'PIN',
    startDate: '2026-10-05',
    endDate: '2027-01-05',
    durationMonths: 3,
    depositAmount: 103500,
    monthlyRate: 345000,
    baseRent: 1035000,
    totalRent: 1035000,
    policyVersion: 'v3',
    accessCode: null,
    status: 'RESERVED',
    payments: [
      {
        id: 101,
        receiptCode: 'RC-101',
        payerId: 1,
        reservationId: 1,
        orderCode: 1727932800101,
        purpose: 'DEPOSIT',
        method: 'PAYOS',
        amount: 103500,
        status: 'SUCCEEDED',
        createdAt: '2026-10-02T10:00:00Z',
      },
    ],
  },
  {
    id: 2,
    code: 'BK-2026-0002',
    customerId: 1,
    customerName: 'Lan Nguyen',
    unitId: 3,
    unitCode: 'M-5',
    unitTypeName: 'M',
    facilityName: 'Tan Binh Depot',
    facilityAddress: '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City',
    zoneCode: 'B',
    floor: 2,
    sizeM2: 8.0,
    accessType: 'QR',
    startDate: '2026-09-01',
    endDate: '2026-12-01',
    durationMonths: 3,
    depositAmount: 207000,
    monthlyRate: 690000,
    baseRent: 2070000,
    totalRent: 2070000,
    policyVersion: 'v3',
    accessCode: '482913',
    status: 'CHECKED_IN',
    payments: [
      {
        id: 102,
        receiptCode: 'RC-102',
        payerId: 1,
        reservationId: 2,
        orderCode: 1727932800102,
        purpose: 'DEPOSIT',
        method: 'PAYOS',
        amount: 207000,
        status: 'SUCCEEDED',
        createdAt: '2026-09-01T08:00:00Z',
      },
      {
        id: 103,
        receiptCode: 'RC-103',
        payerId: 1,
        reservationId: 2,
        orderCode: 1727932800103,
        purpose: 'RENT',
        method: 'CASH',
        amount: 2070000,
        status: 'SUCCEEDED',
        createdAt: '2026-09-01T09:00:00Z',
      },
    ],
  },
]

let mockReservationsList: ReservationDto[] = JSON.parse(JSON.stringify(INITIAL_RESERVATIONS))

export function resetMockReservations(custom?: ReservationDto[]) {
  mockReservationsList = custom ? [...custom] : JSON.parse(JSON.stringify(INITIAL_RESERVATIONS))
}

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

export const MOCK_BROWSE_UNITS: import('../types/unit').BrowseUnitDto[] = [
  {
    id: 1,
    code: 'S-3',
    typeName: 'S',
    typeDescription: 'Small unit around 5 m2',
    zoneCode: 'A',
    facilityName: 'Tan Binh Depot',
    floor: 1,
    sizeM2: 5.0,
    accessType: 'PIN',
    status: 'PREPARING',
    imageUrl: '/units/S-3.jpg',
    monthlyRate: 345000,
    depositRate: 10,
    availabilityStatus: 'Available Oct 5 · cleaning buffer',
    availableFromDate: '2026-10-05',
    isImmediatelyAvailable: false,
    isInCleaningBuffer: true,
  },
  {
    id: 3,
    code: 'M-5',
    typeName: 'M',
    typeDescription: 'Medium unit around 8 m2',
    zoneCode: 'B',
    facilityName: 'Tan Binh Depot',
    floor: 2,
    sizeM2: 8.0,
    accessType: 'QR',
    status: 'AVAILABLE',
    imageUrl: '/units/M-5.jpg',
    monthlyRate: 690000,
    depositRate: 10,
    availabilityStatus: 'Available now',
    availableFromDate: '2026-10-02',
    isImmediatelyAvailable: true,
    isInCleaningBuffer: false,
  },
]



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

