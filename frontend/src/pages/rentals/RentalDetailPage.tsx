import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getRentalDetail } from '../../api/rental'
import type { ReservationStatus, PaymentStatus } from '../../types/rental'
import { Card } from '../../components/ui/Card'
import { Badge, type BadgeVariant } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Skeleton } from '../../components/ui/Skeleton'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '../../components/ui/Table'
import { CheckInPassModal } from '../../components/rentals/CheckInPassModal'
import { PaymentModal } from '../../components/payment/PaymentModal'
import { formatMoney, formatUnitCode } from '../../lib/format'

function getStatusBadgeProps(status: ReservationStatus): { variant: BadgeVariant; label: string } {
  switch (status) {
    case 'PENDING_PAYMENT':
      return { variant: 'warning', label: 'Awaiting Deposit' }
    case 'RESERVED':
      return { variant: 'reserved', label: 'Reserved' }
    case 'CHECKED_IN':
      return { variant: 'success', label: 'Active Rental' }
    case 'CHECKOUT_REQUESTED':
      return { variant: 'warning', label: 'Checkout Requested' }
    case 'CLOSED':
      return { variant: 'neutral', label: 'Completed' }
    case 'EXPIRED':
      return { variant: 'neutral', label: 'Expired' }
    case 'CANCELLED':
      return { variant: 'neutral', label: 'Cancelled' }
    default:
      return { variant: 'neutral', label: status }
  }
}

function getDepositBadgeProps(status: ReservationStatus): { variant: BadgeVariant; label: string; subtext: string } {
  switch (status) {
    case 'PENDING_PAYMENT':
      return { variant: 'warning', label: 'Pending Payment', subtext: '10% Refundable Deposit Due' }
    case 'RESERVED':
    case 'CHECKED_IN':
    case 'CHECKOUT_REQUESTED':
      return { variant: 'success', label: 'Held', subtext: '10% Refundable Deposit Held' }
    case 'EXPIRED':
      return { variant: 'error', label: 'Forfeited', subtext: 'Deposit Forfeited (No-Show)' }
    case 'CLOSED':
      return { variant: 'neutral', label: 'Settled', subtext: 'Deposit Settled / Refunded' }
    case 'CANCELLED':
      return { variant: 'neutral', label: 'Cancelled', subtext: 'Deposit Cancelled' }
    default:
      return { variant: 'neutral', label: status, subtext: 'Deposit' }
  }
}

function getPaymentStatusBadge(status: PaymentStatus): { variant: BadgeVariant; label: string } {
  switch (status) {
    case 'SUCCEEDED':
      return { variant: 'success', label: 'Paid' }
    case 'PENDING':
    case 'PENDING_CASH':
    case 'PROCESSING':
      return { variant: 'warning', label: 'Pending' }
    case 'FAILED':
    case 'EXPIRED':
      return { variant: 'error', label: status }
    default:
      return { variant: 'neutral', label: status }
  }
}

export function RentalDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [showPassModal, setShowPassModal] = useState(false)
  const [showPaymentModal, setShowPaymentModal] = useState(false)

  const { data: reservation, isLoading, isError, refetch } = useQuery({
    queryKey: ['rental-detail', id],
    queryFn: () => getRentalDetail(id || ''),
    enabled: Boolean(id),
  })

  if (isLoading) {
    return (
      <div className="space-y-6 max-w-5xl mx-auto">
        <Skeleton className="h-8 w-48 rounded-sh-sm" />
        <Skeleton className="h-40 w-full rounded-sh-md" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-48 rounded-sh-md" />
          <Skeleton className="h-48 rounded-sh-md" />
        </div>
      </div>
    )
  }

  if (isError || !reservation) {
    return (
      <div className="max-w-5xl mx-auto">
        <Card className="p-8 text-center bg-sh-surface">
          <h3 className="typography-headline text-sh-ink font-semibold mb-2">
            Rental Details Not Found
          </h3>
          <p className="typography-body text-sh-muted mb-4">
            The requested rental record could not be loaded or you do not have permission to view it.
          </p>
          <div className="flex justify-center gap-3">
            <Button variant="secondary" onClick={() => navigate('/rentals')}>
              Back to My Rentals
            </Button>
            <Button variant="primary" onClick={() => refetch()}>
              Retry
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  const statusBadge = getStatusBadgeProps(reservation.status)
  const depositBadge = getDepositBadgeProps(reservation.status)
  const isReserved = reservation.status === 'RESERVED'
  const isCheckedIn = reservation.status === 'CHECKED_IN'
  const isPendingPayment = reservation.status === 'PENDING_PAYMENT'
  const payments = reservation.payments || []

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12">
      {/* Back Link & Header */}
      <div>
        <button
          type="button"
          onClick={() => navigate('/rentals')}
          aria-label="Back to My Rentals"
          className="text-xs font-medium text-sh-muted hover:text-sh-ink transition-colors mb-3 inline-flex items-center gap-1.5 cursor-pointer"
        >
          <span>←</span> Back to My Rentals
        </button>

        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <div className="flex items-center gap-3">
              <h1 className="typography-display text-sh-ink font-bold">
                {`Unit ${formatUnitCode(reservation.unitCode)}`}
              </h1>
              <Badge status={statusBadge.variant} showDot>
                {statusBadge.label}
              </Badge>
            </div>
            <p className="typography-meta text-sh-muted mt-1">
              Reservation <span className="font-mono text-sh-ink font-semibold">{reservation.code}</span> · {reservation.facilityName || 'Tan Binh Depot'}
            </p>
          </div>

          {isReserved && (
            <Button variant="primary" size="sm" onClick={() => setShowPassModal(true)}>
              View Check-in Pass
            </Button>
          )}
          {isPendingPayment && (
            <Button variant="primary" size="sm" onClick={() => setShowPaymentModal(true)}>
              Pay Deposit Now
            </Button>
          )}
        </div>
      </div>

      {/* Pending Payment Banner */}
      {isPendingPayment && (
        <Card className="p-5 bg-sh-warning-tint border-l-4 border-l-sh-warning border-sh-status-buffer-border rounded-sh-md shadow-sm">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="space-y-1">
              <div className="text-xs uppercase tracking-wider text-sh-warning font-semibold">
                Deposit Payment Required
              </div>
              <p className="text-xs text-sh-ink-secondary">
                Please complete the {formatMoney(reservation.depositAmount)} refundable deposit to confirm and lock this unit reservation.
              </p>
            </div>
            <Button variant="primary" size="sm" onClick={() => setShowPaymentModal(true)}>
              Pay Deposit Now
            </Button>
          </div>
        </Card>
      )}

      {/* Check-in Banner / Notice (if RESERVED) */}
      {isReserved && (
        <Card className="p-5 bg-sh-surface-subtle border-l-4 border-l-sh-primary border-sh-border rounded-sh-md shadow-sm">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div className="space-y-1">
              <div className="text-xs uppercase tracking-wider text-sh-muted font-semibold">
                Counter Check-in Pass
              </div>
              <div className="font-mono text-xl font-bold text-sh-ink">
                {reservation.code}
              </div>
              <p className="text-xs text-sh-ink-secondary">
                Please visit the front desk with your National ID to sign the rental agreement and pay the remaining balance.
              </p>
            </div>
            <Button variant="secondary" size="sm" onClick={() => setShowPassModal(true)}>
              Open Pass
            </Button>
          </div>
        </Card>
      )}

      {/* Grid of Modular Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Card 1: Unit & Facility Specifications */}
        <Card className="p-5 space-y-4 bg-sh-surface border border-sh-border rounded-sh-md shadow-sm">
          <h3 className="typography-headline text-sm font-semibold text-sh-ink uppercase tracking-wider border-b border-sh-divider pb-2">
            Unit & Facility Specifications
          </h3>
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-sh-muted block">Unit Code:</span>
              <span className="font-mono font-semibold text-sh-ink">
                {formatUnitCode(reservation.unitCode)}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Type:</span>
              <span className="font-medium text-sh-ink">
                {reservation.unitTypeName ? `${reservation.unitTypeName} Type` : 'Standard'}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Floor / Zone:</span>
              <span className="font-medium text-sh-ink">
                Floor {reservation.floor ?? 1} · Zone {reservation.zoneCode ?? 'A'}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Size:</span>
              <span className="font-medium text-sh-ink">
                {reservation.sizeM2 ? `${reservation.sizeM2} m²` : 'N/A'}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Access Mechanism:</span>
              <span className="font-medium text-sh-ink">
                {reservation.accessType ? `${reservation.accessType} Keypad` : 'PIN Keypad'}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Facility:</span>
              <span className="font-medium text-sh-ink">
                {reservation.facilityName || 'Tan Binh Depot'}
              </span>
            </div>
            <div className="col-span-2">
              <span className="text-sh-muted block">Facility Address:</span>
              <span className="font-medium text-sh-ink">
                {reservation.facilityAddress || '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City'}
              </span>
            </div>
          </div>
        </Card>

        {/* Card 2: Rental Timeline & Schedule */}
        <Card className="p-5 space-y-4 bg-sh-surface border border-sh-border rounded-sh-md shadow-sm">
          <h3 className="typography-headline text-sm font-semibold text-sh-ink uppercase tracking-wider border-b border-sh-divider pb-2">
            Rental Schedule & Timeline
          </h3>
          <div className="grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-sh-muted block">Start Date:</span>
              <span className="font-semibold text-sh-ink">{reservation.startDate}</span>
            </div>
            <div>
              <span className="text-sh-muted block">End Date:</span>
              <span className="font-semibold text-sh-ink">{reservation.endDate}</span>
            </div>
            <div>
              <span className="text-sh-muted block">Duration:</span>
              <span className="font-medium text-sh-ink">
                {reservation.durationMonths} {reservation.durationMonths > 1 ? 'months' : 'month'}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Status:</span>
              <span className="font-medium text-sh-ink">{statusBadge.label}</span>
            </div>
          </div>

          {isCheckedIn && (
            <div className="p-3 bg-sh-surface-muted border border-sh-border rounded-sh-md">
              <span className="text-xs text-sh-muted block mb-1">Access PIN / Code:</span>
              <span className="font-mono text-lg font-bold text-sh-ink tracking-wider">
                {reservation.accessCode || 'Active on Keypad'}
              </span>
            </div>
          )}
        </Card>

        {/* Card 3: Financial Terms & Deposit Status */}
        <Card className="p-5 space-y-4 bg-sh-surface border border-sh-border rounded-sh-md shadow-sm md:col-span-2">
          <h3 className="typography-headline text-sm font-semibold text-sh-ink uppercase tracking-wider border-b border-sh-divider pb-2">
            Financial Terms & Deposit Status
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4 text-xs">
            <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
              <span className="text-sh-muted block">Monthly Rate</span>
              <span className="font-mono text-base font-semibold text-sh-ink">
                {formatMoney(reservation.monthlyRate)}
              </span>
              <span className="text-[11px] text-sh-muted block">per month</span>
            </div>
            <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
              <span className="text-sh-muted block">Base Rent</span>
              <span className="font-mono text-base font-semibold text-sh-ink">
                {formatMoney(reservation.baseRent)}
              </span>
              <span className="text-[11px] text-sh-muted block">for {reservation.durationMonths} {reservation.durationMonths > 1 ? 'mos' : 'mo'}</span>
            </div>
            <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
              <span className="text-sh-muted block">Total Rent</span>
              <span className="font-mono text-base font-semibold text-sh-ink">
                {formatMoney(reservation.totalRent)}
              </span>
              <span className="text-[11px] text-sh-muted block">locked terms ({reservation.policyVersion})</span>
            </div>
            <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
              <div className="flex items-center justify-between">
                <span className="text-sh-muted block">Deposit Status</span>
                <Badge status={depositBadge.variant} showDot>
                  {depositBadge.label}
                </Badge>
              </div>
              <span className="font-mono text-base font-semibold text-sh-ink">
                {formatMoney(reservation.depositAmount)}
              </span>
              <span className="text-[11px] text-sh-muted block">{depositBadge.subtext}</span>
            </div>
          </div>
        </Card>

        {/* Card 4: Payment Receipts Ledger */}
        <Card className="p-5 space-y-4 bg-sh-surface border border-sh-border rounded-sh-md shadow-sm md:col-span-2">
          <div className="flex items-center justify-between border-b border-sh-divider pb-2">
            <h3 className="typography-headline text-sm font-semibold text-sh-ink uppercase tracking-wider">
              Payment Receipts Ledger
            </h3>
            <span className="text-xs text-sh-muted font-medium">
              {payments.length} {payments.length === 1 ? 'Receipt' : 'Receipts'}
            </span>
          </div>

          {payments.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Receipt Code</TableHead>
                  <TableHead>Purpose</TableHead>
                  <TableHead>Method</TableHead>
                  <TableHead numeric>Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {payments.map((p) => {
                  const pBadge = getPaymentStatusBadge(p.status)
                  const methodLabel = p.method === 'PAYOS' ? 'PAYOS QR' : p.method
                  return (
                    <TableRow key={p.id}>
                      <TableCell className="font-mono font-medium">{p.receiptCode}</TableCell>
                      <TableCell className="capitalize">{p.purpose.toLowerCase().replace('_', ' ')}</TableCell>
                      <TableCell>
                        <span className="font-medium text-sh-ink">{methodLabel}</span>
                      </TableCell>
                      <TableCell numeric className="font-mono font-medium">
                        {formatMoney(p.amount)}
                      </TableCell>
                      <TableCell>
                        <Badge status={pBadge.variant} showDot={false}>
                          {pBadge.label}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          ) : (
            <div className="p-6 text-center text-xs text-sh-muted border border-dashed border-sh-border rounded-sh-md">
              No payment receipts recorded for this reservation yet.
            </div>
          )}
        </Card>
      </div>

      {/* Check-in Pass Modal */}
      <CheckInPassModal
        open={showPassModal}
        onOpenChange={setShowPassModal}
        reservation={reservation}
      />

      {/* Payment Modal */}
      {reservation && (
        <PaymentModal
          open={showPaymentModal}
          onOpenChange={setShowPaymentModal}
          reservationId={reservation.id}
          unitCode={reservation.unitCode}
          amount={reservation.depositAmount}
          purpose="DEPOSIT"
          onSuccess={() => {
            refetch()
          }}
        />
      )}
    </div>
  )
}
