import React, { useEffect } from 'react'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import { formatUnitCode } from '../../lib/format'
import type { SupportTicketDto, SupportTicketStatus } from '../../types/support'

interface SupportDetailDrawerProps {
  ticket: SupportTicketDto | null
  isOpen: boolean
  onClose: () => void
}

export const SupportDetailDrawer: React.FC<SupportDetailDrawerProps> = ({
  ticket,
  isOpen,
  onClose,
}) => {
  // Listen for Escape key
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
      }
    }
    if (isOpen) {
      window.addEventListener('keydown', handleKeyDown)
      // Prevent body scroll when drawer is open
      document.body.style.overflow = 'hidden'
    }
    return () => {
      window.removeEventListener('keydown', handleKeyDown)
      document.body.style.overflow = ''
    }
  }, [isOpen, onClose])

  if (!isOpen || !ticket) return null

  const getStatusBadgeVariant = (status: SupportTicketStatus) => {
    switch (status) {
      case 'OPEN':
        return 'primary'
      case 'IN_PROGRESS':
        return 'warning'
      case 'RESOLVED':
        return 'success'
      case 'ESCALATED':
        return 'danger'
      default:
        return 'neutral'
    }
  }

  const formatTimestamp = (dateStr?: string | null) => {
    if (!dateStr) return ''
    try {
      const d = new Date(dateStr)
      return `${d.toLocaleDateString('en-GB')} ${d.toLocaleTimeString('en-GB', {
        hour: '2-digit',
        minute: '2-digit',
      })}`
    } catch {
      return dateStr
    }
  }

  const isSevereRelocated =
    ticket.managerDecision === 'MAINTENANCE_RELOCATE' ||
    ticket.managerDecision === 'SEVERE' ||
    Boolean(ticket.relocatedToUnitCode)

  const hasEscalation =
    Boolean(ticket.escalationNote) ||
    ticket.status === 'ESCALATED' ||
    Boolean(ticket.managerDecision)

  return (
    <div
      className="fixed inset-0 z-50 flex justify-end"
      data-testid="support-detail-drawer-backdrop"
      role="dialog"
      aria-modal="true"
      aria-labelledby="drawer-ticket-code"
    >
      {/* Backdrop Scrim */}
      <div
        className="fixed inset-0 bg-black/40 backdrop-blur-xs transition-opacity cursor-pointer"
        onClick={onClose}
        data-testid="drawer-scrim"
      />

      {/* Slide-over Drawer (420px width) */}
      <div
        className="relative w-full sm:w-[420px] max-w-full h-full bg-sh-surface border-l border-sh-border shadow-2xl flex flex-col z-10 animate-in slide-in-from-right duration-200"
        data-testid="support-detail-drawer"
      >
        {/* Header */}
        <div className="p-5 border-b border-sh-border flex items-start justify-between bg-sh-surface-subtle shrink-0">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span
                id="drawer-ticket-code"
                className="font-mono text-base font-bold text-sh-primary bg-sh-primary-tint px-2.5 py-0.5 rounded border border-sh-primary-border"
                data-testid="drawer-ticket-code"
              >
                {ticket.code}
              </span>
              <Badge status={getStatusBadgeVariant(ticket.status)} showDot>
                {ticket.status}
              </Badge>
            </div>
            <div className="flex items-center gap-2 text-xs text-sh-muted">
              <span>Unit <strong className="text-sh-ink font-mono">{formatUnitCode(ticket.unitCode)}</strong></span>
              <span>•</span>
              <span className="font-semibold text-sh-ink">{ticket.incidentType.replace('_', ' ')}</span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close drawer"
            data-testid="close-support-drawer-btn"
            className="w-8 h-8 flex items-center justify-center rounded-sh-sm text-sh-muted hover:text-sh-ink hover:bg-sh-surface-muted transition-colors cursor-pointer"
          >
            ✕
          </button>
        </div>

        {/* Chronological Lifecycle Timeline */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div>
            <h3 className="text-xs font-bold text-sh-muted uppercase tracking-wider mb-4">
              Ticket Resolution Timeline
            </h3>

            <div className="relative pl-6 space-y-6 before:absolute before:left-[9px] before:top-2 before:bottom-2 before:w-[2px] before:bg-sh-border">
              {/* Stage 1: Reported by Customer */}
              <div className="relative" data-testid="timeline-stage-reported">
                <div className="absolute -left-6 top-1 w-5 h-5 rounded-full bg-sh-primary flex items-center justify-center text-white text-[10px] font-bold shadow-xs">
                  1
                </div>
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-sh-ink">Report Submitted</span>
                    <span className="text-[11px] text-sh-faint font-mono">
                      {formatTimestamp(ticket.createdAt)}
                    </span>
                  </div>
                  <div
                    className="p-3 bg-sh-surface border border-sh-border rounded-sh-sm text-xs text-sh-ink-secondary leading-relaxed"
                    data-testid="drawer-customer-description"
                  >
                    {ticket.description}
                  </div>
                </div>
              </div>

              {/* Stage 2: Staff Assigned & Triage */}
              <div className="relative" data-testid="timeline-stage-assigned">
                <div className="absolute -left-6 top-1 w-5 h-5 rounded-full bg-blue-500 flex items-center justify-center text-white text-[10px] font-bold shadow-xs">
                  2
                </div>
                <div className="space-y-1">
                  <span className="text-xs font-bold text-sh-ink">Assigned for Site Triage</span>
                  <p className="text-xs text-sh-muted">
                    Assigned to on-duty staff:{' '}
                    <strong className="text-sh-ink font-semibold">
                      {ticket.assignedStaffName || 'Tan Binh Depot Staff'}
                    </strong>
                  </p>
                </div>
              </div>

              {/* Stage 3: Escalation (if occurred) */}
              {hasEscalation && (
                <div className="relative" data-testid="timeline-stage-escalated">
                  <div className="absolute -left-6 top-1 w-5 h-5 rounded-full bg-purple-600 flex items-center justify-center text-white text-[10px] font-bold shadow-xs">
                    3
                  </div>
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-purple-900">
                        Escalated to Facility Manager
                      </span>
                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-purple-100 text-purple-700 border border-purple-200">
                        Escalated
                      </span>
                    </div>
                    {ticket.escalationNote && (
                      <div
                        className="p-3 bg-purple-50 border border-purple-200 rounded-sh-sm text-xs text-purple-950 leading-relaxed"
                        data-testid="drawer-escalation-note"
                      >
                        <p className="font-semibold text-purple-900 mb-0.5">Staff Triage Note:</p>
                        {ticket.escalationNote}
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Stage 4: Manager Decision & Relocation (if occurred) */}
              {(ticket.managerDecision || ticket.managerNote || isSevereRelocated) && (
                <div className="relative" data-testid="timeline-stage-decision">
                  <div
                    className={`absolute -left-6 top-1 w-5 h-5 rounded-full flex items-center justify-center text-white text-[10px] font-bold shadow-xs ${
                      isSevereRelocated ? 'bg-red-600' : 'bg-blue-600'
                    }`}
                  >
                    4
                  </div>
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span
                        className={`text-xs font-bold ${
                          isSevereRelocated ? 'text-red-900' : 'text-blue-900'
                        }`}
                      >
                        {isSevereRelocated
                          ? 'Emergency Relocation Authorized'
                          : 'Manager Evaluation & Guidance'}
                      </span>
                      <span
                        className={`text-[10px] font-bold px-1.5 py-0.5 rounded border ${
                          isSevereRelocated
                            ? 'bg-red-100 text-red-800 border-red-200'
                            : 'bg-blue-100 text-blue-800 border-blue-200'
                        }`}
                      >
                        {isSevereRelocated ? 'Relocated' : 'Instructions'}
                      </span>
                    </div>

                    {isSevereRelocated ? (
                      <div
                        className="p-3 bg-red-50 border border-red-200 rounded-sh-sm text-xs text-red-950 space-y-1.5"
                        data-testid="drawer-relocation-banner"
                      >
                        <p className="font-semibold text-red-900">
                          Severe damage confirmed on Unit {formatUnitCode(ticket.unitCode)}.
                        </p>
                        <p className="text-red-800">
                          You have been relocated to Unit{' '}
                          <strong className="font-mono text-red-950 font-bold">
                            {formatUnitCode(ticket.relocatedToUnitCode || 'Target Unit')}
                          </strong>
                          . Your existing booking, contract, and deposit remain fully active with new security access PIN.
                        </p>
                        {ticket.managerNote && (
                          <p className="text-[11px] text-red-700 italic border-t border-red-200 pt-1 mt-1">
                            Note: {ticket.managerNote}
                          </p>
                        )}
                      </div>
                    ) : (
                      ticket.managerNote && (
                        <div
                          className="p-3 bg-blue-50 border border-blue-200 rounded-sh-sm text-xs text-blue-950"
                          data-testid="drawer-manager-guidance"
                        >
                          <p className="font-semibold text-blue-900 mb-0.5">Manager Instructions:</p>
                          <p className="text-blue-800">{ticket.managerNote}</p>
                        </div>
                      )
                    )}
                  </div>
                </div>
              )}

              {/* Stage 5: Final Resolution (when resolved) */}
              {ticket.status === 'RESOLVED' && (
                <div className="relative" data-testid="timeline-stage-resolved">
                  <div className="absolute -left-6 top-1 w-5 h-5 rounded-full bg-green-600 flex items-center justify-center text-white text-[10px] font-bold shadow-xs">
                    ✓
                  </div>
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-green-900">Issue Resolved</span>
                      <span className="text-[11px] text-green-700 font-mono">
                        {formatTimestamp(ticket.updatedAt)}
                      </span>
                    </div>
                    <div
                      className="p-3 bg-green-50 border border-green-200 rounded-sh-sm text-xs text-green-950 space-y-1"
                      data-testid="drawer-resolution-note"
                    >
                      <p className="font-semibold text-green-900">Resolution Summary:</p>
                      <p className="text-green-800 leading-relaxed">
                        {ticket.resolutionNote ||
                          'On-site staff has inspected and resolved the issue. Your unit is fully operational.'}
                      </p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-sh-border bg-sh-surface-subtle flex items-center justify-between shrink-0">
          <p className="text-[11px] text-sh-muted">
            Need further help? Reach us at front desk anytime.
          </p>
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={onClose}
            data-testid="drawer-close-bottom-btn"
          >
            Close
          </Button>
        </div>
      </div>
    </div>
  )
}
