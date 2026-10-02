import { useState } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getUnitDetail } from '../../api/unit'
import { calculatePricing } from '../../api/pricing'
import { createReservation } from '../../api/reservation'
import { formatMoney, formatUnitCode } from '../../lib/format'
import { useToast } from '../../hooks/useToast'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Skeleton } from '../../components/ui/Skeleton'

function calculateEndDateString(startDateStr: string, durationMonths: number): string {
  try {
    const d = new Date(startDateStr)
    if (isNaN(d.getTime())) return ''
    d.setMonth(d.getMonth() + durationMonths)
    return d.toISOString().split('T')[0]
  } catch {
    return ''
  }
}

function formatDateDisplay(dateStr: string): string {
  if (!dateStr) return ''
  try {
    const d = new Date(dateStr)
    if (isNaN(d.getTime())) return dateStr
    return d.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  } catch {
    return dateStr
  }
}

export function BookingSummaryPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { showToast } = useToast()

  const rawUnitCode = searchParams.get('unit') || ''
  const unitCode = formatUnitCode(rawUnitCode)
  const durationMonths = Math.max(1, parseInt(searchParams.get('duration') || '1', 10))
  const startDate = searchParams.get('startDate') || new Date().toISOString().split('T')[0]
  const endDate = calculateEndDateString(startDate, durationMonths)

  const [isSubmitting, setIsSubmitting] = useState(false)

  // Fetch unit details
  const {
    data: unit,
    isLoading: unitLoading,
    isError: unitError,
    error: unitErrorObj,
  } = useQuery({
    queryKey: ['unit', unitCode],
    queryFn: () => getUnitDetail(unitCode),
    enabled: Boolean(unitCode),
  })

  // Fetch verified pricing snapshot from PricingEngine
  const {
    data: pricing,
    isLoading: pricingLoading,
    isError: pricingError,
  } = useQuery({
    queryKey: ['pricing', unitCode, durationMonths, startDate],
    queryFn: () =>
      calculatePricing({
        unitCode,
        durationMonths,
        startDate,
      }),
    enabled: Boolean(unitCode && durationMonths >= 1),
  })

  // Reservation mutation
  const reservationMutation = useMutation({
    mutationFn: () =>
      createReservation({
        unitCode,
        startDate,
        durationMonths,
      }),
    onSuccess: (res) => {
      setIsSubmitting(false)
      showToast('Reservation created successfully! Proceeding to payment.', 'success')
      // Navigate to rental detail or payment modal touchpoint
      navigate(`/booking/summary?unit=${encodeURIComponent(unitCode)}&reserved=${res.code}`)
    },
    onError: (err: any) => {
      setIsSubmitting(false)
      const status = err?.response?.status
      const errorCode = err?.response?.data?.code
      const errorMessage =
        err?.response?.data?.message || `${unitCode} was just reserved. Similar units still available.`

      if (status === 409 || errorCode === 'UNIT_UNAVAILABLE') {
        showToast(errorMessage, 'error')
        navigate('/units')
      } else {
        showToast(errorMessage || 'Failed to create reservation', 'error')
      }
    },
  })

  const handleConfirm = () => {
    setIsSubmitting(true)
    reservationMutation.mutate()
  }

  if (unitLoading || pricingLoading) {
    return (
      <div className="max-w-[680px] mx-auto py-8 px-4 space-y-6" data-testid="booking-summary-loading">
        <Skeleton className="h-5 w-32" />
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-44 w-full rounded-sh-md" />
        <Skeleton className="h-32 w-full rounded-sh-md" />
        <Skeleton className="h-64 w-full rounded-sh-md" />
        <Skeleton className="h-24 w-full rounded-sh-md" />
      </div>
    )
  }

  if (unitError || pricingError || !unit || !pricing) {
    return (
      <div className="max-w-[680px] mx-auto py-8 px-4 space-y-6" data-testid="booking-summary-error">
        <Link
          to="/units"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-sh-primary hover:underline"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Browse Units
        </Link>
        <Card className="p-8 text-center flex flex-col items-center justify-center min-h-[300px]">
          <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mb-3">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 className="typography-headline text-sh-ink font-semibold">Unable to Load Booking Summary</h3>
          <p className="typography-body text-sh-muted max-w-[420px] mt-1 mb-6">
            {(unitErrorObj as any)?.response?.data?.message || `Unit "${unitCode}" pricing or terms could not be retrieved.`}
          </p>
          <div className="flex gap-3">
            <Button variant="primary" onClick={() => navigate('/units')}>
              Back to Available Units
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  return (
    <div className="max-w-[680px] mx-auto py-8 px-4 space-y-6" data-testid="booking-summary-page">
      {/* Top Navigation */}
      <div>
        <Link
          to={`/units/${encodeURIComponent(unit.code)}`}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-sh-ink-secondary hover:text-sh-primary transition-colors"
          data-testid="back-to-unit-link"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Unit Detail
        </Link>
      </div>

      {/* Header */}
      <div className="flex items-center justify-between border-b border-sh-border pb-4">
        <div>
          <h1 className="text-2xl font-bold text-sh-ink">Review Booking Terms</h1>
          <p className="text-sm text-sh-muted mt-0.5">
            Verify your storage space details and itemized pricing snapshot before proceeding.
          </p>
        </div>
        <span
          className="font-mono text-base font-bold px-3 py-1 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-sh-sm"
          data-testid="unit-code-badge"
        >
          {unit.code}
        </span>
      </div>

      {/* Section 1: Storage Unit & Facility */}
      <Card className="p-5 space-y-4" data-testid="storage-unit-facility-section">
        <h2 className="text-sm font-semibold text-sh-ink uppercase tracking-wider text-sh-muted border-b border-sh-divider pb-2">
          Storage Space & Facility
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 text-sm">
          <div>
            <span className="text-xs text-sh-muted block">Unit Code</span>
            <span className="font-mono font-semibold text-sh-ink" data-testid="summary-unit-code">
              {unit.code}
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Unit Type</span>
            <span className="font-medium text-sh-ink" data-testid="summary-unit-type">
              {unit.typeName} {unit.typeDescription ? `(${unit.typeDescription})` : ''}
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Floor / Zone</span>
            <span className="font-medium text-sh-ink" data-testid="summary-unit-location">
              Floor {unit.floor} · Zone {unit.zoneCode}
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Dimensions</span>
            <span className="font-medium text-sh-ink" data-testid="summary-unit-size">
              {unit.sizeM2} m²
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Access Method</span>
            <span className="font-medium text-sh-ink" data-testid="summary-unit-access">
              {unit.accessType} Keyless Access
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Facility Depot</span>
            <span className="font-medium text-sh-ink" data-testid="summary-facility-name">
              {unit.facilityName}
            </span>
          </div>
        </div>
        <div className="pt-2 border-t border-sh-divider text-xs text-sh-muted">
          <span className="font-semibold text-sh-ink-secondary">Address: </span>
          <span data-testid="summary-facility-address">{unit.facilityAddress}</span>
        </div>
      </Card>

      {/* Section 2: Rental Timeline */}
      <Card className="p-5 space-y-4" data-testid="rental-timeline-section">
        <h2 className="text-sm font-semibold text-sh-ink uppercase tracking-wider text-sh-muted border-b border-sh-divider pb-2">
          Rental Timeline
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
          <div>
            <span className="text-xs text-sh-muted block">Move-in Start Date</span>
            <span className="font-semibold text-sh-ink" data-testid="summary-start-date">
              {formatDateDisplay(startDate)}
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Estimated End Date</span>
            <span className="font-semibold text-sh-ink" data-testid="summary-end-date">
              {formatDateDisplay(endDate)}
            </span>
          </div>
          <div>
            <span className="text-xs text-sh-muted block">Committed Duration</span>
            <span className="font-semibold text-sh-ink" data-testid="summary-duration">
              {durationMonths} {durationMonths === 1 ? 'Month' : 'Months'}
            </span>
          </div>
        </div>
      </Card>

      {/* Section 3: Itemized Financial Terms */}
      <Card className="p-5 space-y-4" data-testid="financial-terms-section">
        <div className="flex items-center justify-between border-b border-sh-divider pb-2">
          <h2 className="text-sm font-semibold text-sh-ink uppercase tracking-wider text-sh-muted">
            Itemized Financial Terms
          </h2>
          <span className="text-xs text-sh-muted font-mono" data-testid="policy-version-badge">
            Policy {pricing.policyVersion}
          </span>
        </div>

        <div className="space-y-2.5 text-sm">
          <div className="flex justify-between items-center text-sh-ink-secondary">
            <span>Monthly Base Rent</span>
            <span className="font-mono tabular-nums font-medium" data-testid="item-monthly-rate">
              {formatMoney(pricing.monthlyRate)} / month
            </span>
          </div>

          <div className="flex justify-between items-center text-sh-ink-secondary">
            <span>
              Base Rent for Term ({durationMonths} {durationMonths === 1 ? 'month' : 'months'})
            </span>
            <span className="font-mono tabular-nums font-medium" data-testid="item-base-rent">
              {formatMoney(pricing.baseRent)}
            </span>
          </div>

          {pricing.surcharges && pricing.surcharges.length > 0 && (
            <div className="space-y-1.5 pl-3 border-l-2 border-sh-border py-1">
              {pricing.surcharges.map((s, idx) => (
                <div key={idx} className="flex justify-between text-xs text-sh-muted">
                  <span>{s.name}</span>
                  <span className="font-mono tabular-nums">{formatMoney(s.amount)}</span>
                </div>
              ))}
            </div>
          )}

          <div className="flex justify-between items-center text-sh-ink-secondary pt-1 border-t border-sh-divider">
            <span className="flex items-center gap-1.5">
              Refundable Deposit ({pricing.depositRate}% rate)
              <span className="inline-block w-2 h-2 rounded-full bg-emerald-500" title="100% refundable upon move-out" />
            </span>
            <span className="font-mono tabular-nums font-semibold text-emerald-700" data-testid="item-deposit-amount">
              {formatMoney(pricing.depositAmount)}
            </span>
          </div>

          <div className="flex justify-between items-center font-bold text-sh-ink pt-2 border-t border-sh-border">
            <span>Total Contract Rent</span>
            <span className="font-mono tabular-nums text-base" data-testid="item-total-rent">
              {formatMoney(pricing.totalRent)}
            </span>
          </div>
        </div>

        {/* Deposit Due Now Highlight Card */}
        <div
          className="p-4 bg-indigo-50/80 border border-indigo-200 rounded-sh-md flex items-center justify-between mt-4"
          data-testid="deposit-due-now-highlight"
        >
          <div>
            <span className="text-xs font-bold text-indigo-950 uppercase tracking-wide block">
              Refundable Deposit Due Now
            </span>
            <span className="text-xs text-indigo-700 block mt-0.5">
              Required to lock and hold this unit exclusively
            </span>
          </div>
          <div className="text-right">
            <span className="font-mono font-bold text-xl text-indigo-950 block tabular-nums" data-testid="deposit-due-now-value">
              {formatMoney(pricing.totalDueNow || pricing.depositAmount)}
            </span>
            <span className="text-[10px] text-emerald-700 font-semibold bg-emerald-100/90 px-2 py-0.5 rounded-full inline-block mt-0.5">
              100% Refundable
            </span>
          </div>
        </div>
      </Card>

      {/* Section 4: Legal & Check-in Notice */}
      <div
        className="p-4 bg-amber-50/70 border border-amber-200 rounded-sh-md flex gap-3 text-xs text-amber-900 leading-relaxed"
        data-testid="legal-contract-notice"
      >
        <svg className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <div>
          <p className="font-semibold text-amber-950">Contractual & Check-in Notice</p>
          <p className="mt-0.5 text-amber-900">
            Your rental agreement will be auto-drafted from these exact terms and signed during check-in. Deposit is 100% refundable upon move-out settlement.
          </p>
        </div>
      </div>

      {/* Actions */}
      <div className="flex flex-col sm:flex-row items-center justify-end gap-3 pt-4 border-t border-sh-border">
        <Button
          variant="secondary"
          size="page"
          className="w-full sm:w-auto"
          onClick={() => navigate(`/units/${encodeURIComponent(unit.code)}`)}
          disabled={isSubmitting}
          data-testid="summary-back-btn"
        >
          Back
        </Button>
        <Button
          variant="primary"
          size="page"
          className="w-full sm:w-auto font-bold shadow-md"
          onClick={handleConfirm}
          isLoading={isSubmitting}
          data-testid="confirm-booking-btn"
        >
          Confirm & Proceed to Payment ({formatMoney(pricing.totalDueNow || pricing.depositAmount)})
        </Button>
      </div>
    </div>
  )
}

export default BookingSummaryPage
