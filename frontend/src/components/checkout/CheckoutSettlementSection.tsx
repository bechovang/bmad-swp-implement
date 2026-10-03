import React, { useState, useEffect } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getSettlementPreview, finalizeSettlement } from '../../api/settlement'
import type { SettlementPreviewDto, SettlementReceiptDto } from '../../types/settlement'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'
import { formatMoney } from '../../lib/format'
import { useToast } from '../../hooks/useToast'

interface CheckoutSettlementSectionProps {
  reservationId: number
  reservationCode: string
  unitCode?: string
  customerName?: string
  depositHeld?: number
  hasMajorDamage?: boolean
  majorItems?: string[]
  onSettlementCompleted?: (receipt: SettlementReceiptDto) => void
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

export const CheckoutSettlementSection: React.FC<CheckoutSettlementSectionProps> = ({
  reservationId,
  reservationCode,
  unitCode,
  customerName,
  depositHeld: initialDeposit = 172500,
  hasMajorDamage = false,
  majorItems = [],
  onSettlementCompleted,
}) => {
  const { showToast } = useSafeToast()
  const queryClient = useQueryClient()

  const [damageFee, setDamageFee] = useState<number>(0)
  const [damageReason, setDamageReason] = useState<string>('')
  const [waiverAmount, setWaiverAmount] = useState<number>(0)
  const [waiverReason, setWaiverReason] = useState<string>('')
  const [cashReceived, setCashReceived] = useState<boolean>(false)
  const [notes, setNotes] = useState<string>('')
  const [finalizedReceipt, setFinalizedReceipt] = useState<SettlementReceiptDto | null>(null)

  // Auto-fill damage reason hint if major items were flagged
  useEffect(() => {
    if (hasMajorDamage && majorItems.length > 0 && !damageReason) {
      const itemsList = majorItems.map((i) => i.replace('_', ' ').toLowerCase()).join(', ')
      setDamageReason(`Inspection major defect found on: ${itemsList}`)
    }
  }, [hasMajorDamage, majorItems])

  // Fetch real-time settlement preview with waiver
  const { data: preview } = useQuery<SettlementPreviewDto>({
    queryKey: ['settlement-preview', reservationId, damageFee, damageReason, waiverAmount, waiverReason],
    queryFn: () =>
      getSettlementPreview(reservationId, {
        damageFee,
        damageReason: damageReason || undefined,
        waiverAmount,
        waiverReason: waiverReason || undefined,
      }),
    enabled: Boolean(reservationId),
  })

  // Finalize settlement mutation
  const finalizeMutation = useMutation({
    mutationFn: async (isCashAtDesk: boolean = false) => {
      return await finalizeSettlement(reservationId, {
        damageFee,
        damageReason: damageReason || null,
        waiverAmount,
        waiverReason: waiverReason || null,
        paymentMethod: isCashAtDesk ? 'CASH' : 'PAYOS',
        cashReceived: isCashAtDesk,
        notes: notes || null,
      })
    },
    onSuccess: (receipt) => {
      setFinalizedReceipt(receipt)
      showToast({
        title: `Settlement Finalized — Receipt ${receipt.receiptCode}`,
        tone: 'success',
      })
      queryClient.invalidateQueries({ queryKey: ['rental', reservationId] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['task-detail'] })
      if (onSettlementCompleted) {
        onSettlementCompleted(receipt)
      }
    },
    onError: (err: any) => {
      const msg =
        err?.response?.data?.message || err?.message || 'Failed to finalize settlement'
      showToast({
        title: msg,
        tone: 'error',
      })
    },
  })

  const depositAmount = preview?.depositHeld ?? initialDeposit
  const calculatedDamage = preview?.damageFee ?? damageFee
  const calculatedLateFee = preview?.lateFee ?? 0
  const waiverCap = preview?.waiverCap ?? 50000
  const policyVersionName = preview?.policyVersion ?? 'Rental Policy v3'

  const isWaiverExceeded = preview?.waiverExceeded ?? (waiverAmount > waiverCap)
  const isWaiverReasonMissing = waiverAmount > 0 && (!waiverReason || waiverReason.trim() === '')
  const isReasonMissing = damageFee > 0 && (!damageReason || damageReason.trim() === '')

  const grossCharges = calculatedDamage + calculatedLateFee
  const netCharges = preview?.totalCharges ?? Math.max(0, grossCharges - waiverAmount)
  const refundAmount = preview?.refundAmount ?? Math.max(0, depositAmount - netCharges)
  const extraFeeAmount = preview?.extraFeeAmount ?? Math.max(0, netCharges - depositAmount)
  const isExtraFeeDue = extraFeeAmount > 0

  const canConfirm =
    !isReasonMissing &&
    !isWaiverExceeded &&
    !isWaiverReasonMissing &&
    (!isExtraFeeDue || cashReceived || preview?.extraFeePaid)

  if (finalizedReceipt) {
    return (
      <Card
        className="p-5 bg-emerald-50/60 border border-emerald-300 rounded-sh-sm space-y-4"
        data-testid="settlement-receipt-card"
      >
        <div className="flex items-center justify-between border-b border-emerald-200 pb-3">
          <div className="flex items-center gap-2">
            <span className="text-xl">🧾</span>
            <div>
              <h3 className="text-sm font-bold text-emerald-950 uppercase tracking-wide">
                Permanent Settlement Receipt
              </h3>
              <p className="text-xs text-emerald-800">
                Receipt Code: <span className="font-mono font-bold" data-testid="receipt-code">{finalizedReceipt.receiptCode}</span>
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 text-xs font-bold uppercase rounded bg-emerald-600 text-white shadow-xs">
            Rental Closed
          </span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 text-xs">
          <div className="p-2.5 bg-white rounded border border-emerald-100">
            <span className="text-sh-muted block">Deposit Held</span>
            <span className="font-bold text-sh-ink">{formatMoney(finalizedReceipt.depositHeld)}</span>
          </div>
          <div className="p-2.5 bg-white rounded border border-emerald-100">
            <span className="text-sh-muted block">Damage Deductions</span>
            <span className="font-bold text-rose-600">
              {finalizedReceipt.damageFee > 0 ? `-${formatMoney(finalizedReceipt.damageFee)}` : '0 ₫'}
            </span>
          </div>
          <div className="p-2.5 bg-white rounded border border-emerald-100">
            <span className="text-sh-muted block">Late Return Fee</span>
            <span className="font-bold text-rose-600">
              {finalizedReceipt.lateFee > 0 ? `-${formatMoney(finalizedReceipt.lateFee)}` : '0 ₫'}
            </span>
          </div>
          <div className="p-2.5 bg-emerald-50 rounded border border-emerald-200">
            <span className="text-emerald-800 font-medium block">Fee Waiver / Discount</span>
            <span className="font-bold text-emerald-700 font-mono" data-testid="receipt-waiver-amount">
              {finalizedReceipt.waiverAmount > 0 ? `-${formatMoney(finalizedReceipt.waiverAmount)}` : '0 ₫'}
            </span>
          </div>
          <div className="p-2.5 bg-emerald-100/70 rounded border border-emerald-300">
            <span className="text-emerald-900 font-semibold block">
              {finalizedReceipt.refundAmount > 0 ? 'Refund Processed' : 'Extra Fee Paid'}
            </span>
            <span className="font-bold text-emerald-900 text-sm font-mono" data-testid="receipt-final-amount">
              {finalizedReceipt.refundAmount > 0
                ? formatMoney(finalizedReceipt.refundAmount)
                : formatMoney(finalizedReceipt.extraFeeAmount)}
            </span>
          </div>
        </div>

        {finalizedReceipt.damageReason && (
          <div className="p-2.5 bg-white rounded border border-emerald-100 text-xs space-y-1">
            <span className="font-semibold text-sh-ink">Damage Justification:</span>
            <p className="text-sh-muted">{finalizedReceipt.damageReason}</p>
          </div>
        )}

        {finalizedReceipt.waiverAmount > 0 && finalizedReceipt.waiverReason && (
          <div className="p-2.5 bg-emerald-50/70 rounded border border-emerald-200 text-xs space-y-1" data-testid="receipt-waiver-reason-box">
            <span className="font-semibold text-emerald-900">Fee Waiver Reason (Policy Cap {formatMoney(waiverCap)}):</span>
            <p className="text-emerald-800">{finalizedReceipt.waiverReason}</p>
          </div>
        )}

        <div className="text-[11px] text-emerald-800 flex items-center justify-between pt-2 border-t border-emerald-200">
          <span>Processed by: <strong>{finalizedReceipt.staffName || 'Staff Desk'}</strong></span>
          <span>Status: <strong>CLOSED & UNIT IN TURNOVER</strong></span>
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-4" data-testid="checkout-settlement-section">
      <div className="flex items-center justify-between border-b border-sh-border pb-2">
        <div className="flex items-center gap-2">
          <span className="text-base">💰</span>
          <h3 className="text-xs font-bold uppercase tracking-wider text-sh-ink">
            Step 2: Deposit Settlement & Rental Closure
          </h3>
        </div>
        <span className="text-[11px] font-mono text-sh-muted">
          {reservationCode} · Unit {unitCode || 'N/A'} {customerName ? `· ${customerName}` : ''}
        </span>
      </div>

      {/* Deposit Baseline Info */}
      <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm flex items-center justify-between">
        <div>
          <span className="text-xs font-semibold text-sh-ink block">Original Held Deposit</span>
          <span className="text-[11px] text-sh-muted">Held securely during rental period</span>
        </div>
        <span className="text-sm font-bold font-mono text-sh-ink" data-testid="deposit-held-display">
          {formatMoney(depositAmount)}
        </span>
      </div>

      {/* Itemized Deductions Form */}
      <div className="space-y-3.5 p-3.5 bg-white border border-sh-border rounded-sh-sm">
        <h4 className="text-xs font-bold uppercase tracking-wider text-sh-ink">
          Itemized Settlement Charges & Fee Adjustments
        </h4>

        {/* 1. Damage Fee Row */}
        <div className="space-y-1.5">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <label className="text-xs font-semibold text-sh-ink">
              1. Damage / Replacement Fee (VND)
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min="0"
                step="1000"
                value={damageFee || ''}
                onChange={(e) => setDamageFee(Math.max(0, Number(e.target.value) || 0))}
                placeholder="0"
                data-testid="input-damage-fee"
                className="w-36 px-2.5 py-1 text-xs text-right font-mono font-bold border border-sh-border rounded focus:outline-none focus:ring-1 focus:ring-sh-primary"
              />
              <span className="text-xs text-sh-muted">₫</span>
            </div>
          </div>

          {/* Mandatory Reason Input */}
          <div>
            <input
              type="text"
              value={damageReason}
              onChange={(e) => setDamageReason(e.target.value)}
              placeholder="Mandatory justification reason for damage deduction (e.g. Scratched door, Lost key)..."
              data-testid="input-damage-reason"
              className={`w-full px-2.5 py-1.5 text-xs rounded border transition-colors ${
                isReasonMissing
                  ? 'border-rose-400 bg-rose-50/50 focus:ring-rose-500'
                  : 'border-sh-border bg-white focus:ring-sh-primary'
              }`}
            />
            {isReasonMissing && (
              <p className="text-[11px] text-rose-600 mt-1 font-medium" data-testid="error-damage-reason-required">
                ⚠️ A specific damage reason is mandatory when assessing a damage fee.
              </p>
            )}
          </div>
        </div>

        {/* 2. Late Fee Row */}
        {calculatedLateFee > 0 && (
          <div className="flex items-center justify-between p-2 bg-amber-50 border border-amber-200 rounded text-xs">
            <div>
              <span className="font-semibold text-amber-900 block">2. Late Return Fee (LATE_FEE)</span>
              <span className="text-[11px] text-amber-700">
                Auto-calculated for {preview?.daysLate ?? 1} day(s) overdue
              </span>
            </div>
            <span className="font-bold font-mono text-amber-900" data-testid="late-fee-display">
              +{formatMoney(calculatedLateFee)}
            </span>
          </div>
        )}

        {/* 3. Settlement Waiver Row (Story 6.4) */}
        <div className="space-y-1.5 pt-1 border-t border-sh-divider">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
            <div>
              <label className="text-xs font-semibold text-sh-ink block">
                3. Staff Fee Waiver / Discount (WAIVER_CAP: {formatMoney(waiverCap)})
              </label>
              <span className="text-[11px] text-sh-muted">
                Max {formatMoney(waiverCap)} waiver under {policyVersionName} (requires mandatory reason)
              </span>
            </div>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min="0"
                step="1000"
                value={waiverAmount || ''}
                onChange={(e) => setWaiverAmount(Math.max(0, Number(e.target.value) || 0))}
                placeholder="0"
                data-testid="input-waiver-amount"
                className={`w-36 px-2.5 py-1 text-xs text-right font-mono font-bold border rounded focus:outline-none focus:ring-1 ${
                  isWaiverExceeded
                    ? 'border-rose-400 bg-rose-50 text-rose-700 focus:ring-rose-500'
                    : 'border-sh-border focus:ring-sh-primary'
                }`}
              />
              <span className="text-xs text-sh-muted">₫</span>
            </div>
          </div>

          {/* Waiver Reason Input */}
          <div>
            <input
              type="text"
              value={waiverReason}
              onChange={(e) => setWaiverReason(e.target.value)}
              placeholder="Mandatory reason for applying fee waiver (e.g. Long-term customer goodwill)..."
              data-testid="input-waiver-reason"
              className={`w-full px-2.5 py-1.5 text-xs rounded border transition-colors ${
                isWaiverReasonMissing
                  ? 'border-rose-400 bg-rose-50/50 focus:ring-rose-500'
                  : 'border-sh-border bg-white focus:ring-sh-primary'
              }`}
            />
            {isWaiverReasonMissing && (
              <p className="text-[11px] text-rose-600 mt-1 font-medium" data-testid="error-waiver-reason-required">
                ⚠️ A specific waiver reason is mandatory when applying a fee waiver.
              </p>
            )}
            {isWaiverExceeded && (
              <p className="text-[11px] text-rose-600 mt-1 font-medium" data-testid="error-waiver-exceeded">
                ⚠️ Waiver exceeds the {formatMoney(waiverCap)} cap in {policyVersionName}
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Live Financial Arithmetic Breakdown */}
      <div className="p-3.5 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-2">
        <h4 className="text-xs font-bold uppercase tracking-wider text-sh-ink">
          Settlement Balance Preview
        </h4>

        <div className="space-y-1 text-xs">
          <div className="flex justify-between py-0.5 text-sh-muted">
            <span>Deposit Held:</span>
            <span className="font-mono text-sh-ink">{formatMoney(depositAmount)}</span>
          </div>
          <div className="flex justify-between py-0.5 text-sh-muted">
            <span>Gross Deductions (Damage + Late Fee):</span>
            <span className="font-mono text-rose-600">
              {grossCharges > 0 ? `-${formatMoney(grossCharges)}` : '0 ₫'}
            </span>
          </div>
          {waiverAmount > 0 && (
            <div className="flex justify-between py-0.5 text-emerald-800">
              <span>Waiver Discount (Within Cap):</span>
              <span className="font-mono font-semibold text-emerald-700" data-testid="preview-waiver-amount">
                +{formatMoney(waiverAmount)}
              </span>
            </div>
          )}
          <div className="flex justify-between py-0.5 text-sh-muted">
            <span>Net Settlement Charges:</span>
            <span className="font-mono font-semibold text-sh-ink">
              {netCharges > 0 ? `-${formatMoney(netCharges)}` : '0 ₫'}
            </span>
          </div>

          <div className="border-t border-sh-border pt-1.5 flex justify-between items-center font-bold">
            <span className="text-xs text-sh-ink">
              {isExtraFeeDue ? 'Outstanding Extra Fee Due:' : 'Net Deposit Refund to Customer:'}
            </span>
            <span
              className={`text-sm font-mono ${
                isExtraFeeDue ? 'text-rose-600' : 'text-emerald-700'
              }`}
              data-testid="settlement-net-balance"
            >
              {isExtraFeeDue ? `+${formatMoney(extraFeeAmount)}` : formatMoney(refundAmount)}
            </span>
          </div>
        </div>

        {/* Extra Fee Payment Callout */}
        {isExtraFeeDue && (
          <div
            className="mt-2 p-3 bg-rose-50 border border-rose-300 rounded text-xs space-y-2"
            data-testid="extra-fee-notice"
          >
            <div className="flex items-center gap-1.5 font-bold text-rose-900">
              <span>⚠️ Extra Fee Required</span>
              <span>({formatMoney(extraFeeAmount)})</span>
            </div>
            <p className="text-[11px] text-rose-800">
              Deductions exceed the held deposit. The customer must pay the remaining balance before closing the rental.
            </p>

            <div className="flex items-center gap-2 pt-1">
              <label className="flex items-center gap-2 cursor-pointer bg-white px-3 py-1.5 rounded border border-rose-200">
                <input
                  type="checkbox"
                  checked={cashReceived}
                  onChange={(e) => setCashReceived(e.target.checked)}
                  data-testid="checkbox-cash-received"
                  className="rounded text-rose-600 focus:ring-rose-500"
                />
                <span className="text-xs font-bold text-rose-900">
                  Cash received at front desk ({formatMoney(extraFeeAmount)})
                </span>
              </label>
            </div>
          </div>
        )}
      </div>

      {/* Staff Notes & Confirm Button */}
      <div className="space-y-3 pt-2">
        <input
          type="text"
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          placeholder="Settlement notes (e.g. Customer agreed to scratch repair fee)..."
          data-testid="input-settlement-notes"
          className="w-full px-2.5 py-1.5 text-xs border border-sh-border rounded bg-white focus:outline-none focus:ring-1 focus:ring-sh-primary"
        />

        <div className="flex items-center justify-end gap-2">
          <Button
            type="button"
            variant="primary"
            disabled={!canConfirm || finalizeMutation.isPending}
            loading={finalizeMutation.isPending}
            onClick={() => finalizeMutation.mutate(cashReceived)}
            data-testid="btn-finalize-settlement"
            className="w-full sm:w-auto font-bold"
          >
            {isExtraFeeDue && cashReceived
              ? 'Collect Cash & Finalize Rental Closure'
              : 'Finalize Settlement & Close Rental →'}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default CheckoutSettlementSection
