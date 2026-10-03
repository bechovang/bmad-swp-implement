import React, { useState } from 'react'
import { Modal } from '../../components/ui/Modal'
import { Button } from '../../components/ui/Button'
import { useToast } from '../../hooks/useToast'
import { createSupportTicket } from '../../api/support'
import type { IncidentType } from '../../types/support'

interface RentalOption {
  unitId: number
  unitCode: string
  reservationCode: string
}

interface NewSupportModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess?: () => void
  initialUnitId?: number
  rentals?: RentalOption[]
}

const INCIDENT_TYPE_OPTIONS: { label: string; value: IncidentType; description: string }[] = [
  { label: 'Lost Access / PIN Issue', value: 'LOST_ACCESS', description: 'Cannot unlock door or PIN keypad is unresponsive' },
  { label: 'Device / Lock Issue', value: 'DEVICE_ISSUE', description: 'Smart lock battery, latch, or physical mechanism jammed' },
  { label: 'Security Concern', value: 'SECURITY', description: 'Suspicious activity or broken door/cage fixture' },
  { label: 'Cleanliness Issue', value: 'CLEANLINESS', description: 'Corridor or unit interior needs cleaning attention' },
  { label: 'Other Incident', value: 'OTHER', description: 'General maintenance inquiries or facility issues' },
]

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

export function NewSupportModal({
  isOpen,
  onClose,
  onSuccess,
  initialUnitId,
  rentals = [],
}: NewSupportModalProps) {
  const { showToast } = useSafeToast()
  const [selectedUnitId, setSelectedUnitId] = useState<number | string>(initialUnitId || (rentals[0]?.unitId || ''))
  const [incidentType, setIncidentType] = useState<IncidentType>('LOST_ACCESS')
  const [description, setDescription] = useState('')
  const [descriptionError, setDescriptionError] = useState<string | null>(null)
  const [unitError, setUnitError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleBlurDescription = () => {
    if (!description.trim()) {
      setDescriptionError('Description must not be blank')
    } else if (description.trim().length < 5) {
      setDescriptionError('Description must be at least 5 characters')
    } else {
      setDescriptionError(null)
    }
  }

  const handleBlurUnit = () => {
    if (!selectedUnitId) {
      setUnitError('Please select a rented unit')
    } else {
      setUnitError(null)
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!selectedUnitId) {
      setUnitError('Please select a rented unit')
      return
    }

    if (!description.trim()) {
      setDescriptionError('Description must not be blank')
      return
    }
    if (description.trim().length < 5) {
      setDescriptionError('Description must be at least 5 characters')
      return
    }

    try {
      setIsSubmitting(true)
      const ticket = await createSupportTicket({
        unitId: Number(selectedUnitId),
        incidentType,
        description: description.trim(),
      })

      showToast({
        title: `Ticket ${ticket.code} Created: Unit ${ticket.unitCode} routed to staff`,
        tone: 'success',
      })

      setDescription('')
      onSuccess?.()
      onClose()
    } catch (err: any) {
      const errMsg = err.response?.data?.message || 'Failed to create support ticket'
      showToast({
        title: `Support Request Failed: ${errMsg}`,
        tone: 'error',
      })
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
      title="Open Support Ticket"
    >
      <form onSubmit={handleSubmit} className="space-y-4" data-testid="new-support-modal">
        <div>
          <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
            Rented Unit <span className="text-red-500">*</span>
          </label>
          {rentals.length > 0 ? (
            <select
              value={selectedUnitId}
              onChange={(e) => {
                setSelectedUnitId(e.target.value)
                setUnitError(null)
              }}
              onBlur={handleBlurUnit}
              className="w-full px-3 py-2 text-xs border rounded-sh-md bg-sh-surface border-sh-border text-sh-ink focus:outline-none focus:ring-2 focus:ring-sh-primary"
              data-testid="support-unit-select"
            >
              <option value="">Select a unit...</option>
              {rentals.map((r) => (
                <option key={r.unitId} value={r.unitId}>
                  Unit {r.unitCode} ({r.reservationCode})
                </option>
              ))}
            </select>
          ) : (
            <div className="text-xs text-sh-muted p-2.5 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
              {initialUnitId ? `Unit ID: ${initialUnitId}` : 'No active rentals found'}
            </div>
          )}
          {unitError && <p className="text-xs text-red-500 mt-1">{unitError}</p>}
        </div>

        <div>
          <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
            Incident Type <span className="text-red-500">*</span>
          </label>
          <select
            value={incidentType}
            onChange={(e) => setIncidentType(e.target.value as IncidentType)}
            className="w-full px-3 py-2 text-xs border rounded-sh-md bg-sh-surface border-sh-border text-sh-ink focus:outline-none focus:ring-2 focus:ring-sh-primary"
            data-testid="incident-type-select"
          >
            {INCIDENT_TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <p className="text-[11px] text-sh-muted mt-1">
            {INCIDENT_TYPE_OPTIONS.find((o) => o.value === incidentType)?.description}
          </p>
        </div>

        <div>
          <label className="block text-xs font-semibold text-sh-ink uppercase tracking-wider mb-1">
            Issue Description <span className="text-red-500">*</span>
          </label>
          <textarea
            rows={4}
            value={description}
            onChange={(e) => {
              setDescription(e.target.value)
              if (descriptionError) setDescriptionError(null)
            }}
            onBlur={handleBlurDescription}
            placeholder="Please describe what happened in detail..."
            className="w-full px-3 py-2 text-xs border rounded-sh-md bg-sh-surface border-sh-border text-sh-ink focus:outline-none focus:ring-2 focus:ring-sh-primary resize-none"
            data-testid="support-description-input"
          />
          {descriptionError && (
            <p className="text-xs text-red-500 mt-1">{descriptionError}</p>
          )}
        </div>

        <div className="flex justify-end gap-2 pt-2 border-t border-sh-border">
          <Button variant="secondary" size="sm" type="button" onClick={onClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button variant="primary" size="sm" type="submit" disabled={isSubmitting} data-testid="submit-support-ticket-btn">
            {isSubmitting ? 'Submitting...' : 'Submit Ticket'}
          </Button>
        </div>
      </form>
    </Modal>
  )
}
