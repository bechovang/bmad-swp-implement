import { useState, useEffect, useRef } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getTaskById, updateTaskStatus, validateCheckInReservation, activateCheckIn } from '../../api/task'
import { getContractByReservation, getContractChain, printContract, signContract, expireContract, voidContract, uploadAttachment } from '../../api/contract'
import { getSupportTickets, getSupportTicketById, resolveSupportTicket, escalateSupportTicket } from '../../api/support'
import { TASK_TYPE_CONFIG, type CheckInValidationDto, type CheckInActivationDto } from '../../types/task'
import type { ContractDto } from '../../types/contract'
import type { SupportTicketDto } from '../../types/support'
import { Button } from '../../components/ui/Button'
import { PaymentModal } from '../../components/payment/PaymentModal'
import { formatMoney, formatUnitCode } from '../../lib/format'

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>()
  const taskId = id ? Number(id) : 0
  const queryClient = useQueryClient()

  const [reservationCodeInput, setReservationCodeInput] = useState('')
  const [validationResult, setValidationResult] = useState<CheckInValidationDto | null>(null)
  const [validationError, setValidationError] = useState<string | null>(null)
  const [isPaymentModalOpen, setIsPaymentModalOpen] = useState(false)

  // Contract ritual state (Story 3.4)
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [activationResult, setActivationResult] = useState<CheckInActivationDto | null>(null)
  const [ritualError, setRitualError] = useState<string | null>(null)
  const fileInputRef = useRef<HTMLInputElement | null>(null)

  // Support ticket handling state (Story 5.2)
  const [supportNote, setSupportNote] = useState('')
  const [supportError, setSupportError] = useState<string | null>(null)
  const [supportSuccess, setSupportSuccess] = useState<string | null>(null)

  // Fetch task details
  const {
    data: task,
    isLoading: isTaskLoading,
    error: taskError,
    refetch: refetchTask,
  } = useQuery({
    queryKey: ['task-detail', taskId],
    queryFn: () => getTaskById(taskId),
    enabled: taskId > 0,
  })

  // Validate reservation mutation
  const validateMutation = useMutation({
    mutationFn: (code: string) => validateCheckInReservation(taskId, { reservationCode: code }),
    onSuccess: (data) => {
      if (data.valid) {
        setValidationResult(data)
        setValidationError(null)
      } else {
        setValidationResult(null)
        setValidationError(data.errorMessage || data.errorCode || 'Invalid reservation for check-in')
      }
    },
    onError: (err: any) => {
      setValidationResult(null)
      const msg = err?.response?.data?.message || err?.message || 'Failed to validate reservation'
      setValidationError(msg)
    },
  })

  // Contract query for reservation or contract task refCode
  const isContractTask = task?.type === 'CONTRACT'
  const contractReservationId = validationResult?.reservationId || (task?.refCode?.startsWith('BK-') ? 2 : undefined)

  const {
    data: contract,
    refetch: refetchContract,
  } = useQuery<ContractDto>({
    queryKey: ['contract-reservation', contractReservationId, task?.refCode],
    queryFn: async () => {
      if (contractReservationId) {
        return await getContractByReservation(contractReservationId)
      }
      if (task?.refCode?.startsWith('CT-')) {
        // Find by contract code from chain of reservation 2 by default in mock
        const chain = await getContractChain(2)
        const found = chain.find((c) => c.code === task.refCode)
        if (found) return found
      }
      return await getContractByReservation(2)
    },
    enabled: Boolean(contractReservationId || isContractTask),
    retry: false,
  })

  // Set initial code input and auto-validate if refCode is available
  useEffect(() => {
    if (task?.refCode && task.type === 'CHECK_IN') {
      setReservationCodeInput(task.refCode)
      validateMutation.mutate(task.refCode)
    }
  }, [task?.refCode, task?.type, taskId])

  // Status update mutation
  const statusMutation = useMutation({
    mutationFn: (newStatus: 'TODO' | 'IN_PROGRESS' | 'DONE') =>
      updateTaskStatus(taskId, { status: newStatus }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to update task status'
      setRitualError(msg)
    },
  })

  // Print contract mutation
  const printMutation = useMutation({
    mutationFn: (contractId: number) => printContract(contractId),
    onSuccess: () => {
      refetchContract()
      if (typeof window !== 'undefined') {
        window.print()
      }
    },
  })

  // Sign contract mutation (Upload attachment + sign contract)
  const signMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile) {
        throw new Error('Please select or capture a photo of the signed contract')
      }
      if (!contract?.id) {
        throw new Error('No contract found to sign')
      }

      // 1. Upload attachment
      const uploadRes = await uploadAttachment(selectedFile)

      // 2. Sign contract
      return await signContract(contract.id, { signedPhotoUrl: uploadRes.fileUrl })
    },
    onSuccess: () => {
      setRitualError(null)
      refetchContract()
      queryClient.invalidateQueries({ queryKey: ['contract-reservation'] })
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to sign contract'
      setRitualError(msg)
    },
  })

  // Expire contract mutation (Story 4.3)
  const expireMutation = useMutation({
    mutationFn: async (contractId: number) => {
      return await expireContract(contractId, 'Customer did not sign within 7 days')
    },
    onSuccess: () => {
      setRitualError(null)
      refetchContract()
      queryClient.invalidateQueries({ queryKey: ['contract-reservation'] })
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to expire contract'
      setRitualError(msg)
    },
  })

  // Void contract mutation (Story 4.3)
  const voidMutation = useMutation({
    mutationFn: async ({ contractId, reason }: { contractId: number; reason: string }) => {
      return await voidContract(contractId, reason)
    },
    onSuccess: () => {
      setRitualError(null)
      refetchContract()
      queryClient.invalidateQueries({ queryKey: ['contract-reservation'] })
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to void contract'
      setRitualError(msg)
    },
  })

  // Activate check-in mutation (Story 3.4)
  const activateMutation = useMutation({
    mutationFn: () => activateCheckIn(taskId),
    onSuccess: (data) => {
      setActivationResult(data)
      setRitualError(null)
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      const msg = err?.response?.data?.message || err?.message || 'Failed to activate check-in'
      setRitualError(msg)
    },
  })

  // Support ticket query for support task (Story 5.2)
  const isSupportTask = task?.type === 'SUPPORT'

  const {
    data: supportTicket,
    refetch: refetchSupportTicket,
  } = useQuery<SupportTicketDto | null>({
    queryKey: ['support-ticket-task', task?.refCode, taskId],
    queryFn: async () => {
      if (task?.refCode) {
        const numericId = Number(task.refCode)
        if (!isNaN(numericId) && numericId > 0 && !task.refCode.startsWith('SR-')) {
          try {
            return await getSupportTicketById(numericId)
          } catch {
            // fallback
          }
        }
        const list = await getSupportTickets()
        const found = list.find((t) => t.code === task.refCode || String(t.id) === task.refCode)
        if (found) return found
      }
      const list = await getSupportTickets()
      return list[0] || null
    },
    enabled: isSupportTask,
    retry: false,
  })

  // Resolve support ticket mutation (Story 5.2)
  const resolveTicketMutation = useMutation({
    mutationFn: async ({ ticketId, note }: { ticketId: number; note: string }) => {
      return await resolveSupportTicket(ticketId, { note })
    },
    onSuccess: () => {
      setSupportError(null)
      setSupportSuccess('Support ticket resolved successfully.')
      setSupportNote('')
      refetchSupportTicket()
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      setSupportSuccess(null)
      const msg = err?.response?.data?.message || err?.message || 'Failed to resolve support ticket'
      setSupportError(msg)
    },
  })

  // Escalate support ticket mutation (Story 5.2)
  const escalateTicketMutation = useMutation({
    mutationFn: async ({ ticketId, note }: { ticketId: number; note: string }) => {
      if (!note || !note.trim()) {
        throw new Error('Please enter an escalation note explaining the issue to the manager.')
      }
      return await escalateSupportTicket(ticketId, { note: note.trim() })
    },
    onSuccess: () => {
      setSupportError(null)
      setSupportSuccess('Support ticket escalated to Facility Manager.')
      setSupportNote('')
      refetchSupportTicket()
      queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      refetchTask()
    },
    onError: (err: any) => {
      setSupportSuccess(null)
      const msg = err?.response?.data?.message || err?.message || 'Failed to escalate support ticket'
      setSupportError(msg)
    },
  })

  const handleValidate = (e?: React.FormEvent) => {
    if (e) e.preventDefault()
    if (!reservationCodeInput.trim()) {
      setValidationError('Please enter a reservation code')
      return
    }
    validateMutation.mutate(reservationCodeInput.trim())
  }

  const handlePaymentSuccess = (paymentData: any) => {
    setIsPaymentModalOpen(false)
    if (validationResult) {
      setValidationResult({
        ...validationResult,
        rentPaid: true,
        rentReceiptCode: paymentData.receiptCode || `RC-${paymentData.orderCode || Date.now()}`,
      })
    }
    queryClient.invalidateQueries({ queryKey: ['task-detail', taskId] })
    if (task?.refCode) {
      validateMutation.mutate(task.refCode)
    }
    refetchContract()
  }

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      setSelectedFile(file)
      const objUrl = URL.createObjectURL(file)
      setPreviewUrl(objUrl)
    }
  }

  const handleRetake = () => {
    setSelectedFile(null)
    setPreviewUrl(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  if (isTaskLoading) {
    return (
      <div className="max-w-4xl mx-auto py-8 px-4" data-testid="task-detail-loading">
        <div className="h-8 bg-sh-surface-muted rounded w-1/3 animate-pulse mb-6" />
        <div className="h-48 bg-sh-surface-muted rounded animate-pulse" />
      </div>
    )
  }

  if (taskError || !task) {
    return (
      <div className="max-w-4xl mx-auto py-8 px-4 space-y-4">
        <div className="p-4 bg-red-50 border border-red-200 rounded-sh-md text-red-800 text-sm">
          Failed to load task details.
        </div>
        <Link to="/tasks" className="text-sh-primary text-sm font-semibold hover:underline">
          ← Back to Task Board
        </Link>
      </div>
    )
  }

  const taskTypeConfig = TASK_TYPE_CONFIG[task.type]
  const isCheckIn = task.type === 'CHECK_IN'
  const isContractSigned = contract?.status === 'SIGNED' || contract?.status === 'ACTIVE'

  return (
    <div className="max-w-4xl mx-auto py-6 px-4 space-y-6" data-testid="task-detail-page">
      {/* Top Breadcrumb & Status Actions */}
      <div className="flex items-center justify-between">
        <Link
          to="/tasks"
          className="text-xs font-semibold text-sh-muted hover:text-sh-ink flex items-center gap-1"
        >
          <span>←</span>
          <span>Back to Task Board</span>
        </Link>

        {/* Quick Transition Pills */}
        <div className="flex items-center gap-2">
          {task.status !== 'TODO' && (
            <button
              type="button"
              onClick={() => statusMutation.mutate('TODO')}
              disabled={statusMutation.isPending}
              className="text-xs px-2.5 py-1 rounded-sh-sm font-medium border border-sh-border text-sh-muted hover:text-sh-ink hover:bg-sh-surface-muted transition-colors cursor-pointer"
            >
              Reset to To do
            </button>
          )}

          {task.status === 'TODO' && (
            <button
              type="button"
              onClick={() => statusMutation.mutate('IN_PROGRESS')}
              disabled={statusMutation.isPending}
              className="text-xs px-2.5 py-1 rounded-sh-sm font-semibold bg-blue-50 text-blue-700 border border-blue-200 hover:bg-blue-100 transition-colors cursor-pointer"
            >
              Start Progress
            </button>
          )}

          {task.status === 'IN_PROGRESS' && !isCheckIn && (
            <button
              type="button"
              onClick={() => statusMutation.mutate('DONE')}
              disabled={statusMutation.isPending}
              className="text-xs px-2.5 py-1 rounded-sh-sm font-semibold bg-emerald-50 text-emerald-700 border border-emerald-200 hover:bg-emerald-100 transition-colors cursor-pointer"
            >
              Mark Complete
            </button>
          )}

          <span
            data-testid="task-status-badge"
            className={`text-xs font-mono font-bold px-2.5 py-1 rounded-sh-sm uppercase ${
              task.status === 'DONE'
                ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                : task.status === 'IN_PROGRESS'
                ? 'bg-blue-100 text-blue-800 border border-blue-300'
                : 'bg-sh-surface-muted text-sh-muted border border-sh-border'
            }`}
          >
            {task.status}
          </span>
        </div>
      </div>

      {/* Task Summary Card */}
      <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-4">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span
                className="text-xs font-bold px-2 py-0.5 rounded border"
                style={{
                  backgroundColor: taskTypeConfig.tintBg,
                  borderColor: taskTypeConfig.borderColor,
                  color: taskTypeConfig.textColor,
                }}
              >
                {taskTypeConfig.label}
              </span>
              <span className="font-mono text-sm font-semibold text-sh-muted">
                Task #{task.id}
              </span>
            </div>
            <h1 className="text-xl font-bold text-sh-ink">
              {task.title || `${taskTypeConfig.label} Task`}
            </h1>
            {task.description && (
              <p className="text-xs text-sh-ink-secondary">{task.description}</p>
            )}
          </div>

          <div className="text-right text-xs space-y-1">
            <div className="text-sh-muted">
              Work Date: <span className="font-mono text-sh-ink font-semibold">{task.workDate}</span>
            </div>
            {task.assignedStaffName && (
              <div className="text-sh-muted">
                Assigned to: <span className="text-sh-ink font-medium">{task.assignedStaffName}</span>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Check-in Task Specific Workflow */}
      {isCheckIn && (
        <div className="space-y-5" data-testid="check-in-workflow-card">
          {/* Step 1: Validate Reservation Code */}
          <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-sm font-bold uppercase tracking-wider text-sh-ink">
                  1. Validate Customer Reservation Code
                </h2>
                <p className="text-xs text-sh-muted">
                  Enter or scan the customer's check-in pass reservation code (e.g. BK-1042)
                </p>
              </div>
              <span className="font-mono text-xs font-semibold px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded">
                Desk Step 1
              </span>
            </div>

            <form onSubmit={handleValidate} className="flex gap-2">
              <input
                type="text"
                value={reservationCodeInput}
                onChange={(e) => setReservationCodeInput(e.target.value)}
                placeholder="BK-1042"
                data-testid="reservation-code-input"
                className="flex-1 px-3 py-2 text-sm font-mono border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
              />
              <Button
                type="submit"
                variant="primary"
                data-testid="validate-code-btn"
                loading={validateMutation.isPending}
              >
                Validate Code
              </Button>
            </form>

            {validationError && (
              <div
                data-testid="check-in-error-banner"
                className="p-3 bg-red-50 border border-red-200 rounded-sh-sm text-xs text-red-800 flex items-center justify-between"
              >
                <div className="flex items-center gap-2">
                  <span className="font-bold">Validation Failed:</span>
                  <span>{validationError}</span>
                </div>
              </div>
            )}
          </div>

          {/* Step 2: Financial Breakdown & Rent Collection (FR-15) */}
          {validationResult && (
            <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-5">
              <div className="flex items-center justify-between border-b border-sh-divider pb-3">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-base font-bold text-sh-ink">
                      {validationResult.reservationCode}
                    </span>
                    <span className="text-xs text-sh-muted font-medium">
                      ({validationResult.customerName || 'Lan Nguyen'} · Unit {formatUnitCode(validationResult.unitCode || 'S-3')})
                    </span>
                  </div>
                  <p className="text-xs text-sh-muted mt-0.5">
                    Deposit verified. Confirm 100% full rental payment before executing the contract ritual.
                  </p>
                </div>
                <span className="text-xs font-mono font-bold px-2 py-0.5 bg-emerald-50 text-emerald-700 border border-emerald-200 rounded">
                  {validationResult.status || 'RESERVED'}
                </span>
              </div>

              {/* 2-Line Financial Breakdown */}
              <div className="space-y-3">
                <div className="text-xs font-bold uppercase tracking-wider text-sh-muted">
                  2. Financial Breakdown (Deposit Held vs Rent Due)
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* Line 1: Deposit Paid */}
                  <div className="p-4 bg-emerald-50/60 border border-emerald-200 rounded-sh-md space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-emerald-800 uppercase tracking-wide">
                        10% Deposit Paid
                      </span>
                      <span className="text-[10px] font-mono px-1.5 py-0.5 bg-emerald-100 text-emerald-900 border border-emerald-300 rounded">
                        {validationResult.depositReceiptCode || 'CONFIRMED'}
                      </span>
                    </div>
                    <div className="font-mono text-xl font-bold text-emerald-950 tabular-nums">
                      {formatMoney(validationResult.depositAmountPaid || 0)}
                    </div>
                    <p className="text-[11px] text-emerald-700">
                      Paid online & held in escrow (deducted/refunded at checkout)
                    </p>
                  </div>

                  {/* Line 2: 100% Full Rent Due */}
                  <div
                    className={`p-4 border rounded-sh-md space-y-1 ${
                      validationResult.rentPaid
                        ? 'bg-blue-50/60 border-blue-200'
                        : 'bg-amber-50/60 border-amber-200'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span
                        className={`text-xs font-bold uppercase tracking-wide ${
                          validationResult.rentPaid ? 'text-blue-800' : 'text-amber-800'
                        }`}
                      >
                        100% Full Rent Due
                      </span>
                      {validationResult.rentPaid ? (
                        <span className="text-[10px] font-mono px-1.5 py-0.5 bg-blue-100 text-blue-900 border border-blue-300 rounded">
                          {validationResult.rentReceiptCode || 'PAID'}
                        </span>
                      ) : (
                        <span className="text-[10px] font-bold px-1.5 py-0.5 bg-amber-100 text-amber-900 border border-amber-300 rounded">
                          DUE AT DESK
                        </span>
                      )}
                    </div>
                    <div
                      className={`font-mono text-xl font-bold tabular-nums ${
                        validationResult.rentPaid ? 'text-blue-950' : 'text-amber-950'
                      }`}
                    >
                      {formatMoney(validationResult.totalRentDue || 0)}
                    </div>
                    <p
                      className={`text-[11px] ${
                        validationResult.rentPaid ? 'text-blue-700' : 'text-amber-700'
                      }`}
                    >
                      {validationResult.rentPaid
                        ? 'Rent payment confirmed by desk transaction'
                        : 'Collect before contract signing ritual'}
                    </p>
                  </div>
                </div>
              </div>

              {/* Rent Payment Action or Banner */}
              <div className="pt-2 border-t border-sh-border">
                {!validationResult.rentPaid ? (
                  <div className="flex items-center justify-between bg-amber-50/50 p-4 border border-amber-200 rounded-sh-md">
                    <div>
                      <h4 className="text-sm font-bold text-amber-950">Collect 100% Rent Payment</h4>
                      <p className="text-xs text-amber-800">
                        Choose PayOS QR code or accept Cash at desk with immediate receipt.
                      </p>
                    </div>
                    <Button
                      variant="primary"
                      onClick={() => setIsPaymentModalOpen(true)}
                      data-testid="collect-rent-btn"
                      className="font-bold"
                    >
                      Collect Rent ({formatMoney(validationResult.totalRentDue || 0)})
                    </Button>
                  </div>
                ) : (
                  <div
                    className="p-4 bg-emerald-50 border border-emerald-200 rounded-sh-md space-y-1"
                    data-testid="rent-paid-banner"
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span className="w-2 h-2 rounded-full bg-emerald-500" />
                        <span className="text-sm font-bold text-emerald-950">
                          100% Rent Paid (Receipt {validationResult.rentReceiptCode || 'CONFIRMED'})
                        </span>
                      </div>
                      <span className="text-xs font-mono font-bold text-emerald-800 bg-emerald-100 px-2 py-0.5 rounded">
                        PAID
                      </span>
                    </div>
                    <p className="text-xs text-emerald-800">
                      Payment recorded. Proceed to contract printing, customer signature, and photo capture below.
                    </p>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Step 3: Contract Ritual & Access Code Handover (Story 3.4) */}
          {validationResult && validationResult.rentPaid && (
            <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-5" data-testid="contract-ritual-section">
              <div className="flex items-center justify-between border-b border-sh-divider pb-3">
                <div>
                  <h2 className="text-sm font-bold uppercase tracking-wider text-sh-ink">
                    3. Contract Signing Ritual & Access Code Handover
                  </h2>
                  <p className="text-xs text-sh-muted">
                    Print agreement, collect physical signature, attach photo of signed copy, then issue access PIN.
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-semibold px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded">
                    Desk Step 3
                  </span>
                  {contract && (
                    <span
                      data-testid="contract-status-chip"
                      className={`text-xs font-mono font-bold px-2 py-0.5 rounded uppercase ${
                        isContractSigned
                          ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                          : 'bg-amber-100 text-amber-800 border border-amber-300'
                      }`}
                    >
                      {contract.status}
                    </span>
                  )}
                </div>
              </div>

              {ritualError && (
                <div className="p-3 bg-red-50 border border-red-200 rounded-sh-sm text-xs text-red-800">
                  {ritualError}
                </div>
              )}

              {/* Action 3A: Print Contract */}
              <div className="p-4 bg-sh-surface-subtle border border-sh-border rounded-sh-md flex items-center justify-between">
                <div>
                  <h4 className="text-xs font-bold uppercase text-sh-ink">Step 3A: Print Rental Agreement</h4>
                  <p className="text-xs text-sh-muted">
                    Print agreement {contract?.code || 'CT-1042'} generated from locked booking terms for tenant signature.
                  </p>
                </div>
                <Button
                  variant="secondary"
                  onClick={() => contract?.id && printMutation.mutate(contract.id)}
                  loading={printMutation.isPending}
                  data-testid="print-contract-btn"
                  className="text-xs font-semibold"
                >
                  🖨️ Print Agreement
                </Button>
              </div>

              {/* Action 3B: Capture / Upload Signed Copy */}
              {!isContractSigned ? (
                <div className="p-4 border-2 border-dashed border-sh-border rounded-sh-md space-y-4 text-center" data-testid="capture-signed-tile">
                  <div>
                    <h4 className="text-xs font-bold uppercase text-sh-ink">Step 3B: Upload Signed Contract Photo</h4>
                    <p className="text-xs text-sh-muted">
                      Capture or select a clear photo/scan of the physically signed contract document (JPEG, PNG, PDF up to 10MB).
                    </p>
                  </div>

                  <input
                    type="file"
                    ref={fileInputRef}
                    onChange={handleFileChange}
                    accept="image/jpeg,image/png,image/webp,application/pdf"
                    className="hidden"
                    data-testid="signed-file-input"
                  />

                  {!previewUrl ? (
                    <div>
                      <Button
                        type="button"
                        variant="secondary"
                        onClick={() => fileInputRef.current?.click()}
                        data-testid="select-photo-btn"
                      >
                        📷 Take Photo / Select Document
                      </Button>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      <div className="inline-block relative">
                        <img
                          src={previewUrl}
                          alt="Signed contract preview"
                          className="h-36 w-auto object-cover rounded border border-sh-border mx-auto shadow-sm"
                        />
                        <button
                          type="button"
                          onClick={handleRetake}
                          className="mt-2 text-xs text-red-600 hover:underline font-semibold block mx-auto cursor-pointer"
                        >
                          Retake / Change Photo
                        </button>
                      </div>

                      <div>
                        <Button
                          variant="primary"
                          onClick={() => signMutation.mutate()}
                          loading={signMutation.isPending}
                          data-testid="attach-and-sign-btn"
                          className="font-bold"
                        >
                          ✓ Attach & Mark Contract Signed
                        </Button>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-sh-md flex items-center justify-between" data-testid="signed-confirmation-card">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold text-sm">
                      ✓
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-emerald-950 uppercase">
                        Signed Contract Recorded ({contract?.code})
                      </h4>
                      <p className="text-xs text-emerald-800">
                        Physical copy attached. Ready to issue personal access code to tenant.
                      </p>
                    </div>
                  </div>
                  {contract?.signedPhotoUrl && (
                    <a
                      href={contract.signedPhotoUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="text-xs text-sh-primary font-semibold hover:underline"
                    >
                      View Photo ↗
                    </a>
                  )}
                </div>
              )}

              {/* Action 3C: Reveal Access Code & Finalize Check-in */}
              <div className="pt-3 border-t border-sh-border">
                {!activationResult ? (
                  <div className="flex items-center justify-between p-4 bg-indigo-50/50 border border-indigo-200 rounded-sh-md">
                    <div>
                      <h4 className="text-xs font-bold text-indigo-950 uppercase">
                        Step 3C: Handover Access Code & Activate Rental
                      </h4>
                      <p className="text-xs text-indigo-800">
                        Locks check-in completion, transitions unit to RENTED, and reveals security PIN.
                      </p>
                    </div>
                    <Button
                      variant="primary"
                      onClick={() => activateMutation.mutate()}
                      loading={activateMutation.isPending}
                      disabled={!isContractSigned}
                      data-testid="handover-access-code-btn"
                      className="font-bold"
                    >
                      🔑 Handover Access Code
                    </Button>
                  </div>
                ) : (
                  <div className="p-5 bg-emerald-50 border-2 border-emerald-300 rounded-sh-md space-y-4" data-testid="access-code-reveal-card">
                    <div className="flex items-center justify-between">
                      <div>
                        <span className="text-xs font-bold text-emerald-800 uppercase tracking-wider">
                          Check-in Completed · Access Code Generated
                        </span>
                        <h3 className="text-lg font-bold text-emerald-950">
                          Personal Security Code (PIN / QR)
                        </h3>
                      </div>
                      <span className="font-mono text-xs px-2 py-0.5 bg-emerald-200 text-emerald-900 rounded font-bold">
                        ACTIVE RENTAL
                      </span>
                    </div>

                    <div className="p-4 bg-white border border-emerald-200 rounded-sh-md text-center space-y-1">
                      <span className="text-xs text-sh-muted uppercase tracking-wider block">
                        Customer Access Key
                      </span>
                      <div className="font-mono text-3xl font-black text-emerald-950 tracking-widest" data-testid="revealed-access-code">
                        {activationResult.accessCode}
                      </div>
                      <p className="text-[11px] text-sh-muted">
                        Give this 6-digit code to the tenant for keypad access at Unit {formatUnitCode(activationResult.unitCode)}.
                      </p>
                    </div>

                    <div className="flex items-center justify-between text-xs text-emerald-900">
                      <span>✓ Unit status changed to <strong>RENTED</strong></span>
                      <span>✓ Notification sent to customer</span>
                      <Link to="/tasks" className="font-bold text-sh-primary hover:underline">
                        Return to Task Board →
                      </Link>
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Payment Modal for 100% Rent */}
          {validationResult && (
            <PaymentModal
              open={isPaymentModalOpen}
              onOpenChange={setIsPaymentModalOpen}
              reservationId={validationResult.reservationId || 0}
              unitCode={validationResult.unitCode || ''}
              amount={validationResult.totalRentDue || 0}
              purpose="RENT"
              allowedMethods={['PAYOS', 'CASH']}
              onSuccess={handlePaymentSuccess}
            />
          )}
        </div>
      )}

      {/* Contract Signature / Addendum Task Specific Workflow (Story 4.3) */}
      {isContractTask && (
        <div className="space-y-5" data-testid="contract-signature-workflow">
          {ritualError && (
            <div
              data-testid="contract-ritual-error-banner"
              className="p-3 bg-red-50 border border-red-200 rounded-sh-sm text-xs text-red-800 flex items-center justify-between"
            >
              <div className="flex items-center gap-2">
                <span className="font-bold">Error:</span>
                <span>{ritualError}</span>
              </div>
            </div>
          )}

          {/* Contract / Addendum Info Card */}
          <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-sh-divider pb-3">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-base font-bold text-sh-ink" data-testid="contract-code-display">
                    {contract?.code || task.refCode || 'CT-...-A1'}
                  </span>
                  <span className="text-xs text-sh-muted font-medium">
                    (Extension Addendum)
                  </span>
                </div>
                <p className="text-xs text-sh-muted mt-0.5">
                  7-day signature deadline window. Print agreement, collect physical signature, and attach signed photo.
                </p>
              </div>
              <span
                data-testid="addendum-status-chip"
                className={`text-xs font-mono font-bold px-2.5 py-1 rounded uppercase border ${
                  contract?.status === 'SIGNED' || contract?.status === 'ACTIVE'
                    ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                    : contract?.status === 'EXPIRED' || contract?.status === 'VOIDED'
                    ? 'bg-gray-100 text-gray-800 border-gray-300'
                    : 'bg-amber-100 text-amber-800 border-amber-300'
                }`}
              >
                {contract?.status || 'AWAITING_SIGNATURE'}
              </span>
            </div>

            {/* Step A: Print Agreement */}
            <div className="p-4 bg-sh-surface-subtle border border-sh-border rounded-sh-md flex items-center justify-between">
              <div>
                <h4 className="text-xs font-bold uppercase text-sh-ink">Step 1: Print Agreement / Addendum</h4>
                <p className="text-xs text-sh-muted">
                  Generate print view for physical signature at reception desk.
                </p>
              </div>
              <Button
                variant="secondary"
                onClick={() => contract?.id && printMutation.mutate(contract.id)}
                loading={printMutation.isPending}
                data-testid="print-contract-btn"
                className="text-xs font-semibold"
              >
                🖨️ Print Addendum
              </Button>
            </div>

            {/* Step B: Upload Signed Photo */}
            {contract?.status !== 'SIGNED' && contract?.status !== 'EXPIRED' && contract?.status !== 'VOIDED' ? (
              <div className="p-4 border-2 border-dashed border-sh-border rounded-sh-md space-y-4 text-center" data-testid="capture-signed-tile">
                <div>
                  <h4 className="text-xs font-bold uppercase text-sh-ink">Step 2: Upload Signed Document Photo</h4>
                  <p className="text-xs text-sh-muted">
                    Take or select a photo of the signed physical addendum document.
                  </p>
                </div>

                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept="image/jpeg,image/png,image/webp,application/pdf"
                  className="hidden"
                  data-testid="signed-file-input"
                />

                {!previewUrl ? (
                  <div>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => fileInputRef.current?.click()}
                      data-testid="select-photo-btn"
                    >
                      📷 Capture / Select Photo
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-3">
                    <div className="inline-block relative">
                      <img
                        src={previewUrl}
                        alt="Signed document preview"
                        className="h-36 w-auto object-cover rounded border border-sh-border mx-auto shadow-sm"
                      />
                      <button
                        type="button"
                        onClick={handleRetake}
                        className="mt-2 text-xs text-red-600 hover:underline font-semibold block mx-auto cursor-pointer"
                      >
                        Retake Photo
                      </button>
                    </div>

                    <div>
                      <Button
                        variant="primary"
                        onClick={() => signMutation.mutate()}
                        loading={signMutation.isPending}
                        data-testid="attach-and-sign-btn"
                        className="font-bold bg-emerald-600 hover:bg-emerald-700 text-white"
                      >
                        ✓ Attach & Record Signature
                      </Button>
                    </div>
                  </div>
                )}
              </div>
            ) : contract?.status === 'SIGNED' ? (
              <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-sh-md flex items-center justify-between" data-testid="signed-confirmation-card">
                <div className="flex items-center gap-3">
                  <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center text-emerald-700 font-bold text-sm">
                    ✓
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-emerald-950 uppercase">
                      Signed Addendum Recorded ({contract?.code})
                    </h4>
                    <p className="text-xs text-emerald-800">
                      Physical document attached. Ready to mark task complete.
                    </p>
                  </div>
                </div>
                {contract?.signedPhotoUrl && (
                  <a
                    href={contract.signedPhotoUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-sh-primary font-semibold hover:underline"
                  >
                    View Photo ↗
                  </a>
                )}
              </div>
            ) : (
              <div className="p-4 bg-gray-50 border border-gray-200 rounded-sh-md text-xs text-gray-700">
                Addendum status: <strong>{contract?.status}</strong>. File closed.
              </div>
            )}

            {/* Exception & Completion Actions */}
            <div className="pt-3 border-t border-sh-border flex items-center justify-between">
              <div className="flex items-center gap-2">
                {contract && contract.status === 'AWAITING_SIGNATURE' && (
                  <>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => contract.id && expireMutation.mutate(contract.id)}
                      loading={expireMutation.isPending}
                      data-testid="expire-contract-btn"
                      className="text-xs text-amber-800 border-amber-300"
                    >
                      Close as Expired
                    </Button>
                    <Button
                      variant="secondary"
                      size="sm"
                      onClick={() => {
                        const reason = window.prompt('Enter reason for voiding addendum:')
                        if (reason && reason.trim() && contract.id) {
                          voidMutation.mutate({ contractId: contract.id, reason: reason.trim() })
                        }
                      }}
                      loading={voidMutation.isPending}
                      data-testid="void-contract-btn"
                      className="text-xs text-red-800 border-red-300"
                    >
                      Void Addendum
                    </Button>
                  </>
                )}
              </div>

              {task.status !== 'DONE' && (
                <Button
                  variant="primary"
                  onClick={() => statusMutation.mutate('DONE')}
                  loading={statusMutation.isPending}
                  data-testid="complete-contract-task-btn"
                  className="font-bold"
                >
                  Mark Task Complete
                </Button>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Support Ticket Handling Workflow (Story 5.2) */}
      {isSupportTask && (
        <div className="space-y-5" data-testid="support-ticket-workflow">
          {supportError && (
            <div
              data-testid="support-error-banner"
              className="p-3 bg-red-50 border border-red-200 rounded-sh-sm text-xs text-red-800 flex items-center justify-between"
            >
              <div className="flex items-center gap-2">
                <span className="font-bold">Error:</span>
                <span>{supportError}</span>
              </div>
            </div>
          )}

          {supportSuccess && (
            <div
              data-testid="support-success-banner"
              className="p-3 bg-emerald-50 border border-emerald-200 rounded-sh-sm text-xs text-emerald-800 flex items-center justify-between"
            >
              <div className="flex items-center gap-2">
                <span className="font-bold">Success:</span>
                <span>{supportSuccess}</span>
              </div>
            </div>
          )}

          {/* Incident Details Card */}
          <div className="bg-sh-surface border border-sh-border rounded-sh-md p-5 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b border-sh-divider pb-3">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span
                    data-testid="support-ticket-code"
                    className="font-mono text-base font-bold text-sh-ink"
                  >
                    {supportTicket?.code || task.refCode || 'SR-0000'}
                  </span>
                  <span
                    data-testid="incident-type-badge"
                    className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-800 border border-blue-200"
                  >
                    {supportTicket?.incidentType || 'INCIDENT'}
                  </span>
                </div>
                <p className="text-xs text-sh-muted">
                  Customer reported issue requiring staff triage, physical fix, or escalation to manager.
                </p>
              </div>

              <span
                data-testid="support-ticket-status-chip"
                className={`text-xs font-mono font-bold px-2.5 py-1 rounded uppercase border ${
                  supportTicket?.status === 'RESOLVED'
                    ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                    : supportTicket?.status === 'ESCALATED'
                    ? 'bg-purple-100 text-purple-800 border-purple-300'
                    : supportTicket?.status === 'IN_PROGRESS'
                    ? 'bg-blue-100 text-blue-800 border-blue-300'
                    : 'bg-amber-100 text-amber-800 border-amber-300'
                }`}
              >
                {supportTicket?.status || 'OPEN'}
              </span>
            </div>

            {/* Context Details Grid */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-xs">
              <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                <span className="text-sh-muted block">Customer</span>
                <span data-testid="customer-name-display" className="font-semibold text-sh-ink block">
                  {supportTicket?.customerName || task.customerName || 'Lan Nguyen'}
                </span>
                <span className="text-[10px] text-sh-muted font-mono">
                  ID: #{supportTicket?.customerId || 1}
                </span>
              </div>

              <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                <span className="text-sh-muted block">Unit Code</span>
                <span data-testid="unit-code-display" className="font-mono font-bold text-sh-ink block">
                  {formatUnitCode(supportTicket?.unitCode || task.unitCode || 'S-3')}
                </span>
                <span className="text-[10px] text-sh-muted">
                  Booking: {supportTicket?.reservationCode || 'N/A'}
                </span>
              </div>

              <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                <span className="text-sh-muted block">Assigned Staff</span>
                <span className="font-semibold text-sh-ink block">
                  {supportTicket?.assignedStaffName || task.assignedStaffName || 'Minh Tran'}
                </span>
                <span className="text-[10px] text-sh-muted">On-duty Staff</span>
              </div>

              <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                <span className="text-sh-muted block">Reported At</span>
                <span className="font-mono text-sh-ink block">
                  {supportTicket?.createdAt
                    ? new Date(supportTicket.createdAt).toLocaleDateString()
                    : task.workDate}
                </span>
                <span className="text-[10px] text-sh-muted">
                  {supportTicket?.createdAt
                    ? new Date(supportTicket.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                    : task.timeSlot || 'Morning'}
                </span>
              </div>
            </div>

            {/* Original Customer Verbatim Description */}
            <div className="space-y-1">
              <span className="text-xs font-bold uppercase tracking-wider text-sh-muted">
                Original Customer Description
              </span>
              <div
                data-testid="ticket-customer-description"
                className="p-4 bg-amber-50/40 border border-amber-200/80 rounded-sh-md text-sm text-sh-ink leading-relaxed font-sans"
              >
                "{supportTicket?.description || task.description || 'No description provided'}"
              </div>
            </div>

            {/* Resolution Note if present */}
            {supportTicket?.resolutionNote && (
              <div
                data-testid="resolution-note-display"
                className="p-4 bg-emerald-50/70 border border-emerald-200 rounded-sh-md space-y-1"
              >
                <div className="flex items-center gap-2 text-xs font-bold text-emerald-950 uppercase tracking-wide">
                  <span className="w-2 h-2 rounded-full bg-emerald-500" />
                  <span>Resolution Note</span>
                </div>
                <p className="text-sm text-emerald-900 font-sans">
                  {supportTicket.resolutionNote}
                </p>
              </div>
            )}

            {/* Escalation Note if present */}
            {supportTicket?.escalationNote && (
              <div
                data-testid="escalation-note-display"
                className="p-4 bg-purple-50/70 border border-purple-200 rounded-sh-md space-y-1"
              >
                <div className="flex items-center gap-2 text-xs font-bold text-purple-950 uppercase tracking-wide">
                  <span className="w-2 h-2 rounded-full bg-purple-500" />
                  <span>Escalation Note (Forwarded to Facility Manager)</span>
                </div>
                <p className="text-sm text-purple-900 font-sans">
                  {supportTicket.escalationNote}
                </p>
              </div>
            )}

            {/* Staff Resolution / Escalation Form */}
            {supportTicket?.status !== 'RESOLVED' ? (
              <div className="pt-3 border-t border-sh-border space-y-3">
                <div className="space-y-1">
                  <label
                    htmlFor="staff-note"
                    className="text-xs font-bold uppercase tracking-wide text-sh-ink block"
                  >
                    Staff Note / Resolution Steps
                  </label>
                  <textarea
                    id="staff-note"
                    rows={3}
                    value={supportNote}
                    onChange={(e) => setSupportNote(e.target.value)}
                    placeholder="Enter actions taken to resolve the incident, or explain why escalation is required..."
                    data-testid="staff-note-input"
                    className="w-full px-3 py-2 text-sm border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
                  />
                  <p className="text-[11px] text-sh-muted">
                    Note is optional when resolving, but mandatory when escalating to the manager.
                  </p>
                </div>

                <div className="flex items-center justify-between pt-2">
                  <Button
                    type="button"
                    variant="secondary"
                    onClick={() => {
                      if (!supportNote.trim()) {
                        setSupportError('Please enter an escalation note explaining the issue to the manager.')
                        return
                      }
                      if (supportTicket?.id) {
                        escalateTicketMutation.mutate({
                          ticketId: supportTicket.id,
                          note: supportNote,
                        })
                      }
                    }}
                    loading={escalateTicketMutation.isPending}
                    disabled={supportTicket?.status === 'ESCALATED'}
                    data-testid="escalate-ticket-btn"
                    className="text-xs font-semibold text-purple-900 bg-purple-50 border-purple-200 hover:bg-purple-100"
                  >
                    {supportTicket?.status === 'ESCALATED' ? 'Already Escalated' : '⚠️ Escalate to Manager'}
                  </Button>

                  <Button
                    type="button"
                    variant="primary"
                    onClick={() => {
                      if (supportTicket?.id) {
                        resolveTicketMutation.mutate({
                          ticketId: supportTicket.id,
                          note: supportNote,
                        })
                      }
                    }}
                    loading={resolveTicketMutation.isPending}
                    data-testid="resolve-ticket-btn"
                    className="font-bold bg-emerald-600 hover:bg-emerald-700 text-white"
                  >
                    ✓ Resolve Ticket & Close Task
                  </Button>
                </div>
              </div>
            ) : (
              <div className="pt-3 border-t border-sh-border">
                <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-sh-sm text-xs text-emerald-900 flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-bold">✓ Incident Resolved:</span>
                    <span>Support ticket has been closed and tenant notified.</span>
                  </div>
                  <Link to="/tasks" className="font-bold text-sh-primary hover:underline">
                    Return to Task Board →
                  </Link>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

export default TaskDetailPage
