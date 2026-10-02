import { useState, useEffect, useCallback, useMemo, useContext } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ModalRoot, ModalContent, ModalHeader, ModalTitle, ModalDescription, ModalFooter } from '../ui/Modal'
import { Button } from '../ui/Button'
import { ToastContext } from '../../context/toast-context-base'
import { createPaymentLink, getPaymentStatus, cancelPayment } from '../../api/payment'
import type {
  PaymentModalProps,
  PaymentModalState,
  PaymentMethod,
  PaymentResponseDto,
  PaymentPurpose,
} from '../../types/payment'
import { formatMoney, formatUnitCode } from '../../lib/format'

function formatCountdown(seconds: number): string {
  const mins = Math.floor(seconds / 60)
  const secs = seconds % 60
  return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
}

export function PaymentModal({
  open,
  onOpenChange,
  reservationId,
  unitCode,
  amount,
  purpose = 'DEPOSIT',
  allowedMethods,
  onSuccess,
}: PaymentModalProps) {
  const navigate = useNavigate()
  const toastContext = useContext(ToastContext)
  const queryClient = useQueryClient()

  const showToast = useCallback(
    (title: string, tone: 'info' | 'success' | 'warning' | 'error' = 'info') => {
      if (toastContext) {
        toastContext.showToast(title, tone)
      } else if (typeof window !== 'undefined') {
        window.dispatchEvent(
          new CustomEvent('storagehub:toast', {
            detail: {
              id: `toast-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
              title,
              tone,
            },
          })
        )
      }
    },
    [toastContext]
  )

  const [modalState, setModalState] = useState<PaymentModalState>('METHOD_SELECT')
  const [selectedMethod, setSelectedMethod] = useState<PaymentMethod>('PAYOS')
  const [currentPayment, setCurrentPayment] = useState<PaymentResponseDto | null>(null)
  const [remainingSeconds, setRemainingSeconds] = useState<number>(0)
  const [failureCount, setFailureCount] = useState<number>(0)

  const isDeposit = purpose === 'DEPOSIT'

  // Determine available methods. For DEPOSIT, strictly PAYOS only.
  const methods: PaymentMethod[] = useMemo(() => {
    if (isDeposit) {
      return ['PAYOS']
    }
    return allowedMethods || ['PAYOS', 'CASH']
  }, [isDeposit, allowedMethods])

  // Reset state on open
  useEffect(() => {
    if (open) {
      setModalState('METHOD_SELECT')
      setSelectedMethod('PAYOS')
      setCurrentPayment(null)
      setRemainingSeconds(0)
      setFailureCount(0)
    }
  }, [open])

  // Countdown timer derived strictly from expiresAt
  useEffect(() => {
    if (modalState !== 'AWAITING') {
      return
    }

    const updateTimer = () => {
      const expiresTime = currentPayment?.expiresAt
        ? new Date(currentPayment.expiresAt).getTime()
        : Date.now() + 900000
      const diff = Math.max(0, Math.floor((expiresTime - Date.now()) / 1000))
      setRemainingSeconds(diff)

      if (diff <= 0) {
        setModalState((prev) => {
          if (prev !== 'EXPIRED') {
            setFailureCount((fc) => fc + 1)
          }
          return 'EXPIRED'
        })
      }
    }

    updateTimer()
    const timerId = setInterval(updateTimer, 1000)
    return () => clearInterval(timerId)
  }, [modalState, currentPayment?.expiresAt])

  // Polling payment status every 2 seconds while awaiting
  const { data: polledPayment } = useQuery({
    queryKey: ['payment-status', currentPayment?.paymentId],
    queryFn: () => getPaymentStatus(currentPayment!.paymentId),
    refetchInterval: modalState === 'AWAITING' && currentPayment?.paymentId ? 2000 : false,
    enabled: modalState === 'AWAITING' && Boolean(currentPayment?.paymentId),
  })

  // Watch polled status changes
  useEffect(() => {
    if (modalState !== 'AWAITING' || !polledPayment) return

    if (polledPayment.status === 'SUCCEEDED') {
      setModalState('SUCCESS')
      showToast(
        `Payment of ${formatMoney(amount)} confirmed! Unit ${formatUnitCode(unitCode)} is reserved.`,
        'success'
      )
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: ['my-reservations'] })
      queryClient.invalidateQueries({ queryKey: ['rental-detail', String(reservationId)] })
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      queryClient.invalidateQueries({ queryKey: ['unread-notifications-count'] })

      onSuccess?.(polledPayment)
    } else if (polledPayment.status === 'FAILED') {
      setFailureCount((prev) => prev + 1)
      setModalState('FAILED')
    } else if (polledPayment.status === 'EXPIRED') {
      setModalState((prev) => {
        if (prev !== 'EXPIRED') {
          setFailureCount((fc) => fc + 1)
        }
        return 'EXPIRED'
      })
    }
  }, [polledPayment, modalState, amount, unitCode, reservationId, showToast, queryClient, onSuccess])

  // Mutation to create payment link
  const createPaymentMutation = useMutation({
    mutationFn: (methodToUse: PaymentMethod) =>
      createPaymentLink({
        reservationId,
        purpose,
        method: methodToUse,
        amount,
      }),
    onSuccess: (data) => {
      setCurrentPayment(data)
      if (data.status === 'SUCCEEDED') {
        setModalState('SUCCESS')
        showToast(
          `Payment of ${formatMoney(amount)} confirmed! Unit ${formatUnitCode(unitCode)} is reserved.`,
          'success'
        )
        onSuccess?.(data)
      } else {
        setModalState('AWAITING')
      }
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.message || 'Failed to create payment link. Please try again.'
      showToast(msg, 'error')
      setFailureCount((prev) => prev + 1)
      setModalState('FAILED')
    },
  })

  const handleStartPayment = () => {
    createPaymentMutation.mutate(selectedMethod)
  }

  const handleCancelPayment = useCallback(async () => {
    if (currentPayment?.paymentId) {
      await cancelPayment(currentPayment.paymentId)
    }
    setCurrentPayment(null)
    setModalState('METHOD_SELECT')
  }, [currentPayment])

  const handleRetry = () => {
    setModalState('METHOD_SELECT')
    handleStartPayment()
  }

  const handleViewRental = () => {
    onOpenChange(false)
    navigate(`/rentals/${reservationId}`)
  }

  const handleClose = () => {
    if (modalState === 'AWAITING') return // Prevent closing while awaiting
    onOpenChange(false)
  }

  const isAwaiting = modalState === 'AWAITING'

  const purposeTitleMap: Record<PaymentPurpose, string> = {
    DEPOSIT: 'Deposit Payment',
    RENT: 'Rent Payment',
    EXTENSION_FEE: 'Extension Fee Payment',
    DAMAGE_FEE: 'Damage Fee Settlement',
    EXTRA_FEE: 'Extra Fee Settlement',
  }

  return (
    <ModalRoot open={open} onOpenChange={isAwaiting ? () => {} : onOpenChange}>
      <ModalContent
        className="max-w-[480px]"
        data-testid="payment-modal"
        onPointerDownOutside={(e) => {
          if (isAwaiting) e.preventDefault()
        }}
        onEscapeKeyDown={(e) => {
          if (isAwaiting) e.preventDefault()
        }}
      >
        {/* State 1: METHOD_SELECT */}
        {modalState === 'METHOD_SELECT' && (
          <>
            <ModalHeader showClose={!isAwaiting}>
              <div className="flex items-center justify-between pr-4">
                <ModalTitle>{purposeTitleMap[purpose] || 'Payment'}</ModalTitle>
                <span
                  className="font-mono text-xs font-bold px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-sh-sm"
                  data-testid="payment-unit-badge"
                >
                  {formatUnitCode(unitCode)}
                </span>
              </div>
              <ModalDescription>
                Select your payment method to complete the transaction.
              </ModalDescription>
            </ModalHeader>

            <div className="space-y-4">
              {/* Amount Summary Card */}
              <div
                className="p-4 bg-sh-surface-muted border border-sh-border rounded-sh-md flex items-center justify-between"
                data-testid="modal-amount-display"
              >
                <div>
                  <span className="text-xs text-sh-muted uppercase tracking-wider block">
                    Amount Due
                  </span>
                  <span className="text-xs text-sh-ink-secondary">
                    {purpose === 'DEPOSIT' ? '10% Refundable Deposit' : 'Payment Total'}
                  </span>
                </div>
                <div className="text-right">
                  <span className="font-mono text-2xl font-bold text-sh-ink tabular-nums">
                    {formatMoney(amount)}
                  </span>
                </div>
              </div>

              {/* Payment Methods Selection */}
              <div className="space-y-2">
                <span className="text-xs font-semibold text-sh-ink uppercase tracking-wider block">
                  Payment Method
                </span>

                {methods.map((method) => {
                  const isPayOS = method === 'PAYOS'
                  const isSelected = selectedMethod === method

                  return (
                    <div
                      key={method}
                      data-testid={`payment-method-${method.toLowerCase()}`}
                      onClick={() => setSelectedMethod(method)}
                      className={`p-3.5 rounded-sh-md border transition-all cursor-pointer flex items-center justify-between ${
                        isSelected
                          ? 'border-sh-primary bg-indigo-50/50 ring-1 ring-sh-primary'
                          : 'border-sh-border bg-sh-surface hover:border-sh-ink-secondary/30'
                      }`}
                    >
                      <div className="flex items-center gap-3">
                        <div
                          className={`w-4 h-4 rounded-full border flex items-center justify-center ${
                            isSelected
                              ? 'border-sh-primary bg-sh-primary'
                              : 'border-sh-border bg-sh-surface'
                          }`}
                        >
                          {isSelected && <div className="w-1.5 h-1.5 rounded-full bg-white" />}
                        </div>
                        <div>
                          <div className="text-sm font-semibold text-sh-ink flex items-center gap-2">
                            {isPayOS ? 'PayOS QR Payment' : 'Cash at Counter'}
                            {isPayOS && (
                              <span className="text-[10px] font-bold px-1.5 py-0.2 bg-emerald-100 text-emerald-800 rounded">
                                Instant
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-sh-muted">
                            {isPayOS
                              ? 'Scan QR with any Vietnamese banking app (VietQR)'
                              : 'Pay with staff at facility reception desk'}
                          </div>
                        </div>
                      </div>

                      {isPayOS && (
                        <span className="font-mono text-xs font-bold text-indigo-600 bg-indigo-100/70 px-2 py-0.5 rounded">
                          PayOS
                        </span>
                      )}
                    </div>
                  )
                })}
              </div>
            </div>

            <ModalFooter>
              <Button variant="secondary" onClick={handleClose}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleStartPayment}
                isLoading={createPaymentMutation.isPending}
                data-testid="pay-with-qr-btn"
                className="font-bold"
              >
                Pay with QR ({formatMoney(amount)})
              </Button>
            </ModalFooter>
          </>
        )}

        {/* State 2: AWAITING (QR & Polling) */}
        {modalState === 'AWAITING' && (
          <>
            <ModalHeader showClose={false}>
              <div className="flex items-center justify-between pr-2">
                <ModalTitle>Scan QR to Pay</ModalTitle>
                <div
                  className="flex items-center gap-1.5 text-xs font-mono font-semibold text-amber-700 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-sh-sm"
                  data-testid="countdown-timer"
                >
                  <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  <span>{formatCountdown(remainingSeconds)}</span>
                </div>
              </div>
              <ModalDescription>
                Open your banking app, scan the VietQR code, and confirm transfer.
              </ModalDescription>
            </ModalHeader>

            <div className="space-y-4 text-center">
              {/* QR Code Container */}
              <div
                className="p-4 bg-white border border-sh-border rounded-sh-md inline-block mx-auto shadow-sm"
                data-testid="payos-qr-code"
              >
                {currentPayment?.qrCode?.startsWith('data:image') || currentPayment?.qrCode?.startsWith('http') ? (
                  <img
                    src={currentPayment.qrCode}
                    alt="PayOS QR Code"
                    className="w-52 h-52 object-contain mx-auto"
                  />
                ) : (
                  /* Crisp styled SVG VietQR Code visual representation */
                  <div className="relative w-52 h-52 bg-white flex flex-col items-center justify-center p-2 border-2 border-indigo-600 rounded">
                    <svg viewBox="0 0 100 100" className="w-44 h-44 text-sh-ink">
                      {/* Standard QR alignment patterns */}
                      <rect x="5" y="5" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="3" />
                      <rect x="11" y="11" width="16" height="16" fill="currentColor" />
                      <rect x="67" y="5" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="3" />
                      <rect x="73" y="11" width="16" height="16" fill="currentColor" />
                      <rect x="5" y="67" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="3" />
                      <rect x="11" y="73" width="16" height="16" fill="currentColor" />
                      {/* Matrix data dots */}
                      <rect x="38" y="10" width="8" height="8" fill="currentColor" />
                      <rect x="50" y="10" width="8" height="8" fill="currentColor" />
                      <rect x="38" y="22" width="8" height="8" fill="currentColor" />
                      <rect x="50" y="22" width="8" height="8" fill="currentColor" />
                      <rect x="10" y="38" width="8" height="8" fill="currentColor" />
                      <rect x="22" y="38" width="8" height="8" fill="currentColor" />
                      <rect x="38" y="38" width="24" height="24" fill="currentColor" />
                      <rect x="67" y="38" width="8" height="8" fill="currentColor" />
                      <rect x="79" y="38" width="8" height="8" fill="currentColor" />
                      <rect x="67" y="50" width="8" height="8" fill="currentColor" />
                      <rect x="79" y="50" width="8" height="8" fill="currentColor" />
                      <rect x="38" y="67" width="8" height="8" fill="currentColor" />
                      <rect x="50" y="67" width="8" height="8" fill="currentColor" />
                      <rect x="38" y="79" width="8" height="8" fill="currentColor" />
                      <rect x="50" y="79" width="8" height="8" fill="currentColor" />
                      <rect x="67" y="67" width="8" height="8" fill="currentColor" />
                      <rect x="79" y="79" width="8" height="8" fill="currentColor" />
                    </svg>
                    <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                      <span className="bg-indigo-600 text-white font-bold text-[9px] px-1.5 py-0.5 rounded shadow">
                        PayOS
                      </span>
                    </div>
                  </div>
                )}
              </div>

              {/* Transaction details banner */}
              <div className="p-3 bg-sh-surface-muted border border-sh-border rounded-sh-md text-xs space-y-1.5 text-left">
                <div className="flex justify-between">
                  <span className="text-sh-muted">Order Code:</span>
                  <span className="font-mono font-semibold text-sh-ink">
                    #{currentPayment?.orderCode || reservationId}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sh-muted">Amount:</span>
                  <span className="font-mono font-bold text-sh-ink tabular-nums">
                    {formatMoney(amount)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sh-muted">Unit:</span>
                  <span className="font-mono font-semibold text-sh-ink">
                    {formatUnitCode(unitCode)}
                  </span>
                </div>
              </div>

              {/* Status Polling Indicator */}
              <div className="flex items-center justify-center gap-2 text-xs text-sh-muted">
                <span className="relative flex h-2.5 w-2.5">
                  <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-indigo-400 opacity-75"></span>
                  <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-indigo-600"></span>
                </span>
                <span>Awaiting payment confirmation (checking every 2s)...</span>
              </div>

              {/* Direct Checkout Link fallback */}
              {currentPayment?.checkoutUrl && (
                <div className="text-center pt-1">
                  <a
                    href={currentPayment.checkoutUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-xs text-sh-primary hover:underline inline-flex items-center gap-1"
                  >
                    <span>Or open in PayOS checkout page</span>
                    <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                    </svg>
                  </a>
                </div>
              )}
            </div>

            <ModalFooter>
              <Button
                variant="secondary"
                onClick={handleCancelPayment}
                data-testid="cancel-payment-btn"
              >
                Cancel Payment
              </Button>
            </ModalFooter>
          </>
        )}

        {/* State 3: SUCCESS */}
        {modalState === 'SUCCESS' && (
          <>
            <ModalHeader showClose={false}>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
                <ModalTitle className="text-emerald-950">Payment Successful</ModalTitle>
              </div>
            </ModalHeader>

            <div className="space-y-4" data-testid="payment-success-tile">
              {/* Success Tile */}
              <div className="p-5 bg-emerald-50 border border-emerald-200 rounded-sh-md text-center space-y-2">
                <span className="text-xs font-semibold text-emerald-800 uppercase tracking-wider block">
                  Amount Paid
                </span>
                <div className="font-mono text-3xl font-bold text-emerald-950 tabular-nums">
                  {formatMoney(amount)}
                </div>
                <p className="text-sm font-medium text-emerald-900 pt-1">
                  Unit {formatUnitCode(unitCode)} is reserved for you until check-in.
                </p>
              </div>

              {/* Receipt Summary */}
              <div className="p-3 bg-sh-surface-muted border border-sh-border rounded-sh-md text-xs space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-sh-muted">Payment Purpose:</span>
                  <span className="font-medium text-sh-ink capitalize">
                    {purpose.toLowerCase().replace('_', ' ')}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sh-muted">Unit Reserved:</span>
                  <span className="font-mono font-semibold text-sh-ink">
                    {formatUnitCode(unitCode)}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span className="text-sh-muted">Status:</span>
                  <span className="font-semibold text-emerald-700">SUCCEEDED</span>
                </div>
              </div>
            </div>

            <ModalFooter>
              <Button
                variant="primary"
                onClick={handleViewRental}
                data-testid="view-rental-btn"
                className="w-full sm:w-auto font-bold"
              >
                View Rental
              </Button>
            </ModalFooter>
          </>
        )}

        {/* State 4: FAILED */}
        {modalState === 'FAILED' && (
          <>
            <ModalHeader showClose={!isAwaiting}>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-red-100 text-red-600 flex items-center justify-center">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </div>
                <ModalTitle className="text-red-950">Payment Failed</ModalTitle>
              </div>
            </ModalHeader>

            <div className="space-y-4" data-testid="payment-failed-tile">
              <div className="p-4 bg-red-50 border border-red-200 rounded-sh-md text-center space-y-1">
                <p className="text-sm font-semibold text-red-900">No money was taken.</p>
                <p className="text-xs text-red-700">
                  The payment could not be processed or was rejected by your bank.
                </p>
              </div>

              {failureCount >= 2 && (
                <div
                  className="p-3.5 bg-amber-50 border border-amber-200 rounded-sh-md text-xs text-amber-900 leading-relaxed"
                  data-testid="reception-counter-hint"
                >
                  <span className="font-semibold text-amber-950 block mb-0.5">Need Assistance?</span>
                  Having trouble? You may also complete your booking with staff at our reception counter.
                </div>
              )}
            </div>

            <ModalFooter>
              <Button variant="secondary" onClick={handleClose}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleRetry}
                data-testid="retry-payment-btn"
                className="font-bold"
              >
                Retry Payment
              </Button>
            </ModalFooter>
          </>
        )}

        {/* State 5: EXPIRED */}
        {modalState === 'EXPIRED' && (
          <>
            <ModalHeader showClose={!isAwaiting}>
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center">
                  <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                </div>
                <ModalTitle className="text-amber-950">Payment Link Expired</ModalTitle>
              </div>
            </ModalHeader>

            <div className="space-y-4" data-testid="payment-expired-tile">
              <div className="p-4 bg-amber-50 border border-amber-200 rounded-sh-md text-center space-y-1">
                <p className="text-sm font-semibold text-amber-900">No money was taken.</p>
                <p className="text-xs text-amber-700">
                  Payment link expired. Please generate a new QR code to proceed.
                </p>
              </div>

              {failureCount >= 2 && (
                <div
                  className="p-3.5 bg-amber-50 border border-amber-200 rounded-sh-md text-xs text-amber-900 leading-relaxed"
                  data-testid="reception-counter-hint"
                >
                  <span className="font-semibold text-amber-950 block mb-0.5">Need Assistance?</span>
                  Having trouble? You may also complete your booking with staff at our reception counter.
                </div>
              )}
            </div>

            <ModalFooter>
              <Button variant="secondary" onClick={handleClose}>
                Cancel
              </Button>
              <Button
                variant="primary"
                onClick={handleRetry}
                data-testid="retry-payment-btn"
                className="font-bold"
              >
                Generate New QR
              </Button>
            </ModalFooter>
          </>
        )}
      </ModalContent>
    </ModalRoot>
  )
}

export default PaymentModal
