import React, { useState, useEffect } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import { getExtensionBoundary, getExtensionQuote } from '../../api/rental'
import type { ExtensionQuoteDto } from '../../types/extension'
import type { ReservationDto } from '../../types/rental'
import {
  ModalRoot,
  ModalContent,
  ModalHeader,
  ModalTitle,
  ModalDescription,
  ModalFooter,
} from '../ui/Modal'
import { Button } from '../ui/Button'
import { formatMoney, formatUnitCode } from '../../lib/format'

export interface ExtensionModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  reservation: ReservationDto
  onProceedToPayment?: (quote: ExtensionQuoteDto) => void
}

export function ExtensionModal({
  open,
  onOpenChange,
  reservation,
  onProceedToPayment,
}: ExtensionModalProps) {
  const [selectedDate, setSelectedDate] = useState<string>('')
  const [conflictBannerError, setConflictBannerError] = useState<string | null>(null)

  // Fetch conflict boundary on open
  const { data: boundary, isLoading: isBoundaryLoading } = useQuery({
    queryKey: ['extension-boundary', reservation.id],
    queryFn: () => getExtensionBoundary(reservation.id),
    enabled: open && Boolean(reservation.id),
  })

  // Calculate default min date = current end date + 1 day
  const minDate = React.useMemo(() => {
    if (!reservation.endDate) return ''
    const d = new Date(reservation.endDate)
    d.setDate(d.getDate() + 1)
    return d.toISOString().split('T')[0]
  }, [reservation.endDate])

  // Reset selected date when modal opens
  useEffect(() => {
    if (open) {
      setConflictBannerError(null)
      // Default to 1 month from current end date or latestPossibleCheckoutDate if shorter
      if (reservation.endDate) {
        const d = new Date(reservation.endDate)
        d.setMonth(d.getMonth() + 1)
        const defStr = d.toISOString().split('T')[0]
        if (boundary?.latestPossibleCheckoutDate && defStr > boundary.latestPossibleCheckoutDate) {
          setSelectedDate(boundary.latestPossibleCheckoutDate)
        } else {
          setSelectedDate(defStr)
        }
      }
    }
  }, [open, reservation.endDate, boundary?.latestPossibleCheckoutDate])

  // Quote mutation to calculate pricing breakdown
  const quoteMutation = useMutation({
    mutationFn: (newEndDate: string) =>
      getExtensionQuote(reservation.id, { newEndDate }),
    onSuccess: () => {
      setConflictBannerError(null)
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message ||
        err?.message ||
        'Selected date conflicts with an upcoming reservation.'
      setConflictBannerError(msg)
    },
  })

  // Automatically recalculate quote when selectedDate changes and is valid
  useEffect(() => {
    if (selectedDate && selectedDate >= minDate) {
      if (
        boundary?.latestPossibleCheckoutDate &&
        selectedDate > boundary.latestPossibleCheckoutDate
      ) {
        const unit = formatUnitCode(reservation.unitCode)
        const conflictDate = boundary.conflictStartDate || 'an upcoming date'
        const latestDate = boundary.latestPossibleCheckoutDate
        setConflictBannerError(
          `Can't extend to ${selectedDate} — ${unit} has a reservation starting ${conflictDate}. Latest possible checkout is ${latestDate}. Pick another date.`
        )
      } else {
        setConflictBannerError(null)
        quoteMutation.mutate(selectedDate)
      }
    }
  }, [selectedDate, minDate, boundary, reservation.unitCode])

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value
    setSelectedDate(val)
  }

  const quote = quoteMutation.data
  const hasConflict = Boolean(conflictBannerError)

  return (
    <ModalRoot open={open} onOpenChange={onOpenChange}>
      <ModalContent className="max-w-[500px]" data-testid="extension-modal">
        <ModalHeader>
          <div className="flex items-center justify-between pr-4">
            <ModalTitle>Extend Rental Period</ModalTitle>
            <span
              className="font-mono text-xs font-bold px-2 py-0.5 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-sh-sm"
              data-testid="extension-unit-badge"
            >
              {formatUnitCode(reservation.unitCode)}
            </span>
          </div>
          <ModalDescription>
            Extend your stay safely within upcoming reservation boundaries.
          </ModalDescription>
        </ModalHeader>

        <div className="space-y-4">
          {/* Conflict Banner Error at top of form (NFR-8) */}
          {hasConflict && (
            <div
              className="p-3.5 bg-amber-50 border-l-4 border-l-amber-500 border-amber-200 rounded-sh-sm text-xs text-amber-950 font-medium space-y-1"
              data-testid="extension-conflict-banner"
            >
              <div className="font-bold text-amber-900 flex items-center gap-1.5">
                <span>⚠️</span>
                <span>Date Conflict Detected</span>
              </div>
              <p>{conflictBannerError}</p>
            </div>
          )}

          {/* Current Schedule Summary */}
          <div className="p-3.5 bg-sh-surface-subtle border border-sh-border rounded-sh-md grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-sh-muted block">Current Checkout:</span>
              <span className="font-mono font-semibold text-sh-ink">
                {reservation.endDate}
              </span>
            </div>
            <div>
              <span className="text-sh-muted block">Max Allowed Checkout:</span>
              <span className="font-mono font-semibold text-indigo-700">
                {boundary?.latestPossibleCheckoutDate || 'No restriction'}
              </span>
            </div>
          </div>

          {/* New End Date Input */}
          <div className="space-y-1.5">
            <label
              htmlFor="extension-end-date"
              className="text-xs font-bold uppercase tracking-wider text-sh-ink block"
            >
              New Checkout Date
            </label>
            <input
              id="extension-end-date"
              type="date"
              data-testid="extension-date-input"
              value={selectedDate}
              min={minDate}
              max={boundary?.latestPossibleCheckoutDate || undefined}
              onChange={handleDateChange}
              className={`w-full px-3 py-2 text-sm font-mono border rounded-sh-sm focus:outline-none focus:ring-2 focus:ring-sh-primary ${
                hasConflict
                  ? 'border-amber-500 bg-amber-50/30'
                  : 'border-sh-border bg-sh-surface'
              }`}
            />
            {boundary?.message && (
              <p className="text-[11px] text-sh-muted">{boundary.message}</p>
            )}
          </div>

          {/* 2-Line Financial Breakdown */}
          {quote && !hasConflict && (
            <div
              className="p-4 bg-sh-surface border border-sh-border rounded-sh-md space-y-3"
              data-testid="extension-breakdown-card"
            >
              <div className="text-xs font-bold uppercase tracking-wider text-sh-muted border-b border-sh-divider pb-1.5">
                Extension Fee Breakdown
              </div>

              <div className="space-y-2 text-xs">
                {/* Line 1: Additional Rent */}
                <div className="flex items-center justify-between">
                  <div>
                    <span className="font-semibold text-sh-ink block">
                      Additional Rent ({quote.additionalDays} days)
                    </span>
                    <span className="text-[11px] text-sh-muted">
                      Rate: {formatMoney(quote.monthlyRate)} / month
                    </span>
                  </div>
                  <span className="font-mono font-bold text-sh-ink">
                    {formatMoney(quote.additionalRent)}
                  </span>
                </div>

                {/* Line 2: Deposit Top-up */}
                <div className="flex items-center justify-between pt-1 border-t border-sh-divider">
                  <div>
                    <span className="font-semibold text-sh-ink block">
                      Deposit Top-up
                    </span>
                    <span className="text-[11px] text-sh-muted">
                      Held: {formatMoney(quote.currentHeldDeposit)} → New requirement:{' '}
                      {formatMoney(quote.newTotalDepositRequired)}
                    </span>
                  </div>
                  <span className="font-mono font-bold text-emerald-700">
                    +{formatMoney(quote.depositTopUp)}
                  </span>
                </div>

                {/* Total */}
                <div className="flex items-center justify-between pt-2 border-t-2 border-sh-divider">
                  <span className="font-bold text-sh-ink">Total Due Now</span>
                  <span
                    className="font-mono text-base font-bold text-sh-primary"
                    data-testid="extension-total-amount"
                  >
                    {formatMoney(quote.totalFee)}
                  </span>
                </div>
              </div>
            </div>
          )}
        </div>

        <ModalFooter>
          <Button variant="secondary" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            variant="primary"
            disabled={hasConflict || !quote || quoteMutation.isPending || isBoundaryLoading}
            isLoading={quoteMutation.isPending}
            onClick={() => quote && onProceedToPayment?.(quote)}
            data-testid="proceed-to-payment-btn"
            className="font-bold"
          >
            {quote
              ? `Pay Fee (${formatMoney(quote.totalFee)})`
              : 'Proceed to Payment'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </ModalRoot>
  )
}
