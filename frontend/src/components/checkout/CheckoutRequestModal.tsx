import React, { useState } from 'react'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { requestCheckout } from '../../api/checkout'
import { useToast } from '../../hooks/useToast'
import { formatUnitCode } from '../../lib/format'
import type { ReservationDto } from '../../types/reservation'

interface CheckoutRequestModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess?: () => void
  reservation: ReservationDto
}

function useSafeToast() {
  try {
    return useToast()
  } catch {
    return {
      showToast: () => '',
      dismissToast: () => {},
      clearAllToasts: () => {},
      toasts: [],
    }
  }
}

export const CheckoutRequestModal: React.FC<CheckoutRequestModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  reservation,
}) => {
  const { showToast } = useSafeToast()

  // Default to rental end date or today + 7 days
  const todayStr = new Date().toISOString().split('T')[0]
  const defaultDate = reservation.endDate && reservation.endDate >= todayStr ? reservation.endDate : todayStr

  const [requestedDate, setRequestedDate] = useState<string>(defaultDate)
  const [notes, setNotes] = useState<string>('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)

    if (!requestedDate) {
      setErrorMsg('Please select a checkout date.')
      return
    }

    if (requestedDate < todayStr) {
      setErrorMsg('Checkout date cannot be in the past.')
      return
    }

    try {
      setIsSubmitting(true)
      await requestCheckout(reservation.id, {
        requestedDate,
        notes: notes.trim() || undefined,
      })

      showToast({
        title: `Checkout Scheduled: Unit ${reservation.unitCode} on ${requestedDate}`,
        tone: 'success',
      })

      onSuccess?.()
      onClose()
    } catch (err: any) {
      const msg =
        err.response?.data?.message ||
        'Failed to submit checkout request. Please check date availability.'
      setErrorMsg(msg)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <Modal
      open={isOpen}
      onOpenChange={(open) => {
        if (!open) onClose()
      }}
      title="Request Unit Checkout"
    >
      <form onSubmit={handleSubmit} className="space-y-5" data-testid="checkout-request-modal">
        <p className="text-xs text-sh-muted">
          Schedule your move-out inspection and key handover for Unit{' '}
          <strong className="text-sh-ink font-mono">{formatUnitCode(reservation.unitCode)}</strong>.
        </p>

        {errorMsg && (
          <div
            className="p-3 bg-red-50 border border-red-200 text-xs text-red-700 rounded-sh-sm leading-relaxed"
            data-testid="checkout-error-banner"
          >
            {errorMsg}
          </div>
        )}

        {/* Date Selector */}
        <div>
          <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
            Target Checkout Date <span className="text-red-500">*</span>
          </label>
          <input
            type="date"
            min={todayStr}
            value={requestedDate}
            onChange={(e) => {
              setRequestedDate(e.target.value)
              setErrorMsg(null)
            }}
            className="w-full px-3 py-2 text-xs border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
            data-testid="checkout-date-input"
            required
          />
          <p className="text-[11px] text-sh-faint mt-1">
            Rental End Date: <span className="font-mono font-semibold">{reservation.endDate}</span>
          </p>
        </div>

        {/* Transparent 3-Case Settlement Logic Guide */}
        <div className="p-4 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-2.5 text-xs text-sh-ink">
          <div className="font-bold text-sh-ink flex items-center gap-1.5">
            <span>🛡️</span>
            <span>Security Deposit Settlement Guide (Held: {Number(reservation.depositAmount || 0).toLocaleString()} ₫)</span>
          </div>

          <div className="space-y-2 text-[11px] text-sh-ink-secondary">
            <div className="flex items-start gap-2 bg-green-50/60 p-2 rounded border border-green-100">
              <span className="font-bold text-green-700">1. Full Refund:</span>
              <span>
                Unit cleaned, empty, and original padlock & key returned in good order → <strong>100% deposit refunded</strong>.
              </span>
            </div>

            <div className="flex items-start gap-2 bg-amber-50/60 p-2 rounded border border-amber-100">
              <span className="font-bold text-amber-700">2. Deductions:</span>
              <span>
                Damage or late move-out fees will be itemized on the spot and deducted directly from your deposit.
              </span>
            </div>

            <div className="flex items-start gap-2 bg-blue-50/60 p-2 rounded border border-blue-100">
              <span className="font-bold text-blue-700">3. Excess Charges:</span>
              <span>
                If damage charges exceed the held deposit, the difference is settled at the desk via PayOS QR or cash.
              </span>
            </div>
          </div>
        </div>

        {/* Notes input */}
        <div>
          <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
            Notes / Estimated Arrival Time (Optional)
          </label>
          <textarea
            rows={2}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="e.g. Returning keys in the morning around 10:00 AM."
            className="w-full px-3 py-2 text-xs border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
            data-testid="checkout-notes-input"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex items-center justify-end gap-2 pt-2 border-t border-sh-divider">
          <Button type="button" variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            type="submit"
            variant="primary"
            loading={isSubmitting}
            data-testid="submit-checkout-request-btn"
          >
            Confirm & Submit Request →
          </Button>
        </div>
      </form>
    </Modal>
  )
}
