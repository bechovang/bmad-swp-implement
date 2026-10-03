import React, { useState, useEffect } from 'react'
import { Modal } from '../ui/Modal'
import { Button } from '../ui/Button'
import { getCheckoutTaskDetail, submitInspection } from '../../api/checkout'
import { useToast } from '../../hooks/useToast'
import { formatUnitCode } from '../../lib/format'
import type {
  CheckoutTaskDetailDto,
  InspectionItem,
  InspectionResult,
  InspectionItemInput,
} from '../../types/checkout'

interface CheckoutTaskModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess?: () => void
  taskId?: number
  reservationId?: number
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

const INSPECTION_ITEMS_CONFIG: {
  item: InspectionItem
  label: string
  description: string
  icon: string
}[] = [
  {
    item: 'ACCESS_CARD',
    label: 'Access Card / Key Fob',
    description: 'RFID badge and QR access pass returned undamaged',
    icon: '💳',
  },
  {
    item: 'PADLOCK',
    label: 'Depot Padlock & Keys',
    description: 'Original brass padlock and all keys returned',
    icon: '🔒',
  },
  {
    item: 'CLEANLINESS',
    label: 'Unit Cleanliness',
    description: 'Floor swept clean, no debris or trash left behind',
    icon: '🧹',
  },
  {
    item: 'STRUCTURE',
    label: 'Unit Structure & Door',
    description: 'Roll-up door, hinges, side panels, and roof intact without denting',
    icon: '🏗️',
  },
]

export const CheckoutTaskModal: React.FC<CheckoutTaskModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
  taskId,
  reservationId,
}) => {
  const { showToast } = useSafeToast()

  const [isLoading, setIsLoading] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [detail, setDetail] = useState<CheckoutTaskDetailDto | null>(null)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // Checklist state
  const [keyReturned, setKeyReturned] = useState(false)
  const [unitEmptied, setUnitEmptied] = useState(false)
  const [generalNotes, setGeneralNotes] = useState('')

  // 4 items state
  const [itemResults, setItemResults] = useState<Record<InspectionItem, { result: InspectionResult; note: string }>>({
    ACCESS_CARD: { result: 'OK', note: '' },
    PADLOCK: { result: 'OK', note: '' },
    CLEANLINESS: { result: 'OK', note: '' },
    STRUCTURE: { result: 'OK', note: '' },
  })

  // Load task detail
  useEffect(() => {
    if (!isOpen) return

    async function loadData() {
      try {
        setIsLoading(true)
        setErrorMsg(null)
        if (taskId) {
          const res = await getCheckoutTaskDetail(taskId)
          setDetail(res)
          setKeyReturned(res.keyReturned || false)
          setUnitEmptied(res.unitEmptied || false)
          if (res.inspections && res.inspections.length > 0) {
            const nextResults = { ...itemResults }
            res.inspections.forEach((insp) => {
              if (nextResults[insp.item]) {
                nextResults[insp.item] = {
                  result: insp.result,
                  note: insp.note || '',
                }
              }
            })
            setItemResults(nextResults)
          }
        }
      } catch (err: any) {
        setErrorMsg(err.response?.data?.message || 'Failed to load checkout inspection details')
      } finally {
        setIsLoading(false)
      }
    }

    loadData()
  }, [isOpen, taskId, reservationId])

  const handleItemResultChange = (item: InspectionItem, result: InspectionResult) => {
    setItemResults((prev) => ({
      ...prev,
      [item]: { ...prev[item], result },
    }))
  }

  const handleItemNoteChange = (item: InspectionItem, note: string) => {
    setItemResults((prev) => ({
      ...prev,
      [item]: { ...prev[item], note },
    }))
  }

  const majorItems = (Object.keys(itemResults) as InspectionItem[]).filter(
    (k) => itemResults[k].result === 'MAJOR'
  )
  const hasMajorDamage = majorItems.length > 0

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMsg(null)

    const resId = detail?.reservationId || reservationId
    if (!resId) {
      setErrorMsg('Reservation ID is required')
      return
    }

    try {
      setIsSubmitting(true)
      const itemsPayload: InspectionItemInput[] = (Object.keys(itemResults) as InspectionItem[]).map((k) => ({
        item: k,
        result: itemResults[k].result,
        note: itemResults[k].note.trim() || undefined,
      }))

      const saved = await submitInspection(resId, {
        items: itemsPayload,
        keyReturned,
        unitEmptied,
        generalNotes: generalNotes.trim() || undefined,
      })

      setDetail(saved)
      showToast({
        title: `Inspection Saved for Unit ${saved.unitCode}`,
        tone: 'success',
      })
      onSuccess?.()
      onClose()
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Failed to save unit inspection')
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
      title="Checkout Reception & Unit Inspection"
    >
      <div data-testid="checkout-task-modal" className="space-y-6">
        {isLoading ? (
          <div className="py-8 text-center text-xs text-sh-muted animate-pulse">
            Loading checkout inspection details...
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-5">
            {errorMsg && (
              <div
                className="p-3 bg-red-50 border border-red-200 text-xs text-red-700 rounded-sh-sm"
                data-testid="inspection-error-banner"
              >
                {errorMsg}
              </div>
            )}

            {/* Context Info Header */}
            {detail && (
              <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                <div>
                  <span className="text-sh-muted block">Unit:</span>
                  <strong className="text-sh-ink font-mono">{formatUnitCode(detail.unitCode)}</strong>
                </div>
                <div>
                  <span className="text-sh-muted block">Reservation:</span>
                  <span className="font-mono text-sh-ink font-semibold">{detail.reservationCode}</span>
                </div>
                <div>
                  <span className="text-sh-muted block">Customer:</span>
                  <span className="text-sh-ink font-medium">{detail.customerName || '—'}</span>
                </div>
                <div>
                  <span className="text-sh-muted block">Requested Date:</span>
                  <span className="font-mono text-sh-ink">{detail.requestedDate || 'Today'}</span>
                </div>
              </div>
            )}

            {/* Step 1: Reception Checklist */}
            <div className="space-y-3 pt-1">
              <h4 className="text-xs font-bold uppercase tracking-wider text-sh-ink flex items-center gap-1.5">
                <span>1.</span> Key Handover & Reception Checklist
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                <label
                  className={`flex items-center gap-2.5 p-3 rounded border transition-colors cursor-pointer select-none ${
                    keyReturned
                      ? 'bg-emerald-50/70 border-emerald-300 text-emerald-950 font-semibold'
                      : 'bg-white border-sh-border text-sh-ink hover:bg-sh-surface-subtle'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={keyReturned}
                    onChange={(e) => setKeyReturned(e.target.checked)}
                    data-testid="key-returned-checkbox"
                    className="w-4 h-4 rounded text-sh-primary border-gray-300 focus:ring-sh-primary"
                  />
                  <span>🔑 Padlock & Key Returned</span>
                </label>

                <label
                  className={`flex items-center gap-2.5 p-3 rounded border transition-colors cursor-pointer select-none ${
                    unitEmptied
                      ? 'bg-emerald-50/70 border-emerald-300 text-emerald-950 font-semibold'
                      : 'bg-white border-sh-border text-sh-ink hover:bg-sh-surface-subtle'
                  }`}
                >
                  <input
                    type="checkbox"
                    checked={unitEmptied}
                    onChange={(e) => setUnitEmptied(e.target.checked)}
                    data-testid="unit-emptied-checkbox"
                    className="w-4 h-4 rounded text-sh-primary border-gray-300 focus:ring-sh-primary"
                  />
                  <span>📦 Unit Vacated & Emptied</span>
                </label>
              </div>
            </div>

            {/* Step 2: 4-Point Physical Inspection Matrix */}
            <div className="space-y-3 pt-2">
              <div className="flex items-center justify-between">
                <h4 className="text-xs font-bold uppercase tracking-wider text-sh-ink flex items-center gap-1.5">
                  <span>2.</span> 4-Point Physical Condition Inspection
                </h4>
                <span className="text-[11px] text-sh-muted">Required before deposit settlement</span>
              </div>

              <div className="space-y-3" data-testid="inspection-matrix">
                {INSPECTION_ITEMS_CONFIG.map(({ item, label, description, icon }) => {
                  const current = itemResults[item]
                  return (
                    <div
                      key={item}
                      data-testid={`inspection-row-${item.toLowerCase()}`}
                      className={`p-3 rounded-sh-sm border space-y-2 transition-colors ${
                        current.result === 'MAJOR'
                          ? 'bg-rose-50/50 border-rose-200'
                          : current.result === 'MINOR'
                          ? 'bg-amber-50/40 border-amber-200'
                          : 'bg-white border-sh-border'
                      }`}
                    >
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-1.5 font-bold text-xs text-sh-ink">
                            <span>{icon}</span>
                            <span>{label}</span>
                          </div>
                          <p className="text-[11px] text-sh-muted">{description}</p>
                        </div>

                        {/* Result Selection Buttons */}
                        <div className="flex items-center gap-1 self-start sm:self-auto">
                          {(['OK', 'MINOR', 'MAJOR'] as InspectionResult[]).map((res) => {
                            const isSelected = current.result === res
                            return (
                              <button
                                key={res}
                                type="button"
                                onClick={() => handleItemResultChange(item, res)}
                                data-testid={`btn-${item.toLowerCase()}-${res.toLowerCase()}`}
                                className={`px-2.5 py-1 text-[11px] font-bold rounded transition-colors ${
                                  isSelected
                                    ? res === 'OK'
                                      ? 'bg-emerald-600 text-white shadow-xs'
                                      : res === 'MINOR'
                                      ? 'bg-amber-500 text-white shadow-xs'
                                      : 'bg-rose-600 text-white shadow-xs'
                                    : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                                }`}
                              >
                                {res === 'OK' && '✓ OK'}
                                {res === 'MINOR' && '⚠️ Minor'}
                                {res === 'MAJOR' && '✕ Major'}
                              </button>
                            )
                          })}
                        </div>
                      </div>

                      {/* Line Note */}
                      <div>
                        <input
                          type="text"
                          value={current.note}
                          onChange={(e) => handleItemNoteChange(item, e.target.value)}
                          placeholder={`Staff notes for ${label.toLowerCase()}...`}
                          data-testid={`input-note-${item.toLowerCase()}`}
                          className="w-full px-2.5 py-1.5 text-xs bg-white border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
                        />
                      </div>
                    </div>
                  )
                })}
              </div>
            </div>

            {/* Major Damage Warning Banner */}
            {hasMajorDamage && (
              <div
                data-testid="major-damage-warning-banner"
                className="p-3 bg-rose-50 border border-rose-300 rounded-sh-sm text-xs text-rose-900 space-y-1"
              >
                <div className="flex items-center gap-2 font-bold uppercase tracking-wider">
                  <span>⚠️ Major Damage Finding Detected</span>
                </div>
                <p className="text-[11px] text-rose-800 leading-relaxed">
                  Major issues flagged on <strong>{majorItems.join(', ')}</strong>. These will form the required basis for itemized Settlement Charges in Step 6.3.
                </p>
              </div>
            )}

            {/* General Notes */}
            <div>
              <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
                General Handover Comments (Optional)
              </label>
              <textarea
                rows={2}
                value={generalNotes}
                onChange={(e) => setGeneralNotes(e.target.value)}
                placeholder="Overall depot inspection comments..."
                data-testid="general-inspection-notes"
                className="w-full px-3 py-2 text-xs border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
              />
            </div>

            {/* Action Buttons */}
            <div className="flex items-center justify-end gap-2 pt-3 border-t border-sh-divider">
              <Button type="button" variant="secondary" onClick={onClose}>
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                loading={isSubmitting}
                data-testid="submit-inspection-btn"
                className="font-bold"
              >
                Save Inspection & Record Findings →
              </Button>
            </div>
          </form>
        )}
      </div>
    </Modal>
  )
}
