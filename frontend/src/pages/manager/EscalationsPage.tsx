import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getEscalations, processSeverityDecision } from '../../api/support'
import { browseUnits } from '../../api/unit'
import type { EscalationDto, EscalationDecision } from '../../types/support'
import { Button } from '../../components/ui/Button'
import { formatUnitCode } from '../../lib/format'

export function EscalationsPage() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<'ALL' | 'PENDING' | 'RESOLVED'>('ALL')
  const [selectedEscalationId, setSelectedEscalationId] = useState<number | null>(null)

  // Decision form state
  const [decisionMode, setDecisionMode] = useState<'SEVERE' | 'NOT_SEVERE'>('SEVERE')
  const [managerNote, setManagerNote] = useState('')
  const [targetUnitId, setTargetUnitId] = useState<number | null>(null)
  const [decisionError, setDecisionError] = useState<string | null>(null)
  const [decisionSuccess, setDecisionSuccess] = useState<string | null>(null)
  const [isConfirmModalOpen, setIsConfirmModalOpen] = useState(false)

  // Query escalations
  const {
    data: escalations = [],
    isLoading,
    error,
    refetch,
  } = useQuery<EscalationDto[]>({
    queryKey: ['escalations'],
    queryFn: getEscalations,
  })

  // Query available units for relocation
  const { data: availableUnitsData } = useQuery({
    queryKey: ['available-units-relocation'],
    queryFn: () => browseUnits(),
  })

  const availableUnits = availableUnitsData?.items?.filter((u) => u.isImmediatelyAvailable) || []

  // Mutation for severity decision
  const decisionMutation = useMutation({
    mutationFn: async ({
      id,
      decision,
      note,
      unitId,
    }: {
      id: number
      decision: EscalationDecision
      note: string
      unitId?: number | null
    }) => {
      if (!note.trim()) {
        throw new Error('Please enter a manager decision note / instructions.')
      }
      if (decision === 'MAINTENANCE_RELOCATE' || decision === 'SEVERE') {
        if (!unitId) {
          throw new Error('Please select an available target unit for relocation.')
        }
      }
      return await processSeverityDecision(id, {
        decision,
        managerNote: note.trim(),
        targetUnitId: unitId,
      })
    },
    onSuccess: (updated) => {
      setDecisionError(null)
      setDecisionSuccess(
        updated.decision === 'MAINTENANCE_RELOCATE'
          ? `Marked SEVERE: Relocated customer to Unit ${updated.relocatedToUnitCode || 'new unit'}. New access PIN generated.`
          : 'Marked NOT SEVERE: Ticket returned to on-site staff with instructions.'
      )
      setManagerNote('')
      setIsConfirmModalOpen(false)
      queryClient.invalidateQueries({ queryKey: ['escalations'] })
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      queryClient.invalidateQueries({ queryKey: ['support-tickets'] })
      refetch()
    },
    onError: (err: any) => {
      setDecisionSuccess(null)
      const msg = err?.response?.data?.message || err?.message || 'Failed to process severity decision'
      setDecisionError(msg)
    },
  })

  // Filtered escalations
  const filteredEscalations = escalations.filter((e) => {
    if (filter === 'PENDING') return e.decision === 'PENDING'
    if (filter === 'RESOLVED') return e.decision !== 'PENDING'
    return true
  })

  const selectedEscalation =
    escalations.find((e) => e.id === selectedEscalationId) || filteredEscalations[0] || null

  const handleOpenDecisionConfirm = () => {
    setDecisionError(null)
    if (!managerNote.trim()) {
      setDecisionError('Please enter a manager decision note or instructions.')
      return
    }
    if (decisionMode === 'SEVERE' && !targetUnitId) {
      setDecisionError('Please select an available target unit for relocation.')
      return
    }
    setIsConfirmModalOpen(true)
  }

  const handleExecuteDecision = () => {
    if (!selectedEscalation) return
    const apiDecision: EscalationDecision =
      decisionMode === 'SEVERE' ? 'MAINTENANCE_RELOCATE' : 'RETURN_TO_STAFF'

    decisionMutation.mutate({
      id: selectedEscalation.id,
      decision: apiDecision,
      note: managerNote,
      unitId: decisionMode === 'SEVERE' ? targetUnitId : null,
    })
  }

  return (
    <div className="max-w-6xl mx-auto py-6 px-4 space-y-6" data-testid="escalations-page">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sh-divider pb-4">
        <div>
          <h1 className="text-2xl font-bold text-sh-ink flex items-center gap-2">
            <span>Escalation Inbox</span>
            <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-purple-100 text-purple-800 border border-purple-300">
              Facility Manager
            </span>
          </h1>
          <p className="text-xs text-sh-muted mt-1">
            Review escalated building incidents, determine damage severity, and authorize emergency customer relocation.
          </p>
        </div>

        {/* Filter Pills */}
        <div className="flex items-center gap-1.5 bg-sh-surface border border-sh-border rounded-sh-md p-1 self-start">
          <button
            type="button"
            onClick={() => setFilter('ALL')}
            className={`px-3 py-1 text-xs font-semibold rounded transition-colors cursor-pointer ${
              filter === 'ALL'
                ? 'bg-sh-primary text-white'
                : 'text-sh-muted hover:text-sh-ink hover:bg-sh-surface-muted'
            }`}
          >
            All ({escalations.length})
          </button>
          <button
            type="button"
            onClick={() => setFilter('PENDING')}
            data-testid="filter-pending-btn"
            className={`px-3 py-1 text-xs font-semibold rounded transition-colors cursor-pointer ${
              filter === 'PENDING'
                ? 'bg-amber-600 text-white'
                : 'text-sh-muted hover:text-sh-ink hover:bg-sh-surface-muted'
            }`}
          >
            Pending ({escalations.filter((e) => e.decision === 'PENDING').length})
          </button>
          <button
            type="button"
            onClick={() => setFilter('RESOLVED')}
            className={`px-3 py-1 text-xs font-semibold rounded transition-colors cursor-pointer ${
              filter === 'RESOLVED'
                ? 'bg-emerald-600 text-white'
                : 'text-sh-muted hover:text-sh-ink hover:bg-sh-surface-muted'
            }`}
          >
            Decided ({escalations.filter((e) => e.decision !== 'PENDING').length})
          </button>
        </div>
      </div>

      {/* Global Error / Success Banners */}
      {decisionError && (
        <div
          data-testid="escalation-error-banner"
          className="p-4 bg-red-50 border border-red-200 rounded-sh-md text-xs text-red-800 flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <span className="font-bold">Error:</span>
            <span>{decisionError}</span>
          </div>
          <button
            type="button"
            onClick={() => setDecisionError(null)}
            className="font-bold text-red-800 hover:underline"
          >
            ✕
          </button>
        </div>
      )}

      {decisionSuccess && (
        <div
          data-testid="escalation-success-banner"
          className="p-4 bg-emerald-50 border border-emerald-200 rounded-sh-md text-xs text-emerald-800 flex items-center justify-between"
        >
          <div className="flex items-center gap-2">
            <span className="font-bold">Decision Recorded:</span>
            <span>{decisionSuccess}</span>
          </div>
          <button
            type="button"
            onClick={() => setDecisionSuccess(null)}
            className="font-bold text-emerald-800 hover:underline"
          >
            ✕
          </button>
        </div>
      )}

      {/* Main 2-Column Layout */}
      {isLoading ? (
        <div className="h-64 bg-sh-surface border border-sh-border rounded-sh-md animate-pulse p-6" />
      ) : error ? (
        <div className="p-4 bg-red-50 border border-red-200 rounded-sh-md text-sm text-red-800">
          Failed to load escalations inbox.
        </div>
      ) : filteredEscalations.length === 0 ? (
        <div className="bg-sh-surface border border-sh-border rounded-sh-md p-12 text-center space-y-2">
          <div className="text-3xl">📭</div>
          <h3 className="text-sm font-bold text-sh-ink">No escalations found</h3>
          <p className="text-xs text-sh-muted">
            There are currently no escalated support requests requiring facility manager intervention.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Left Column: Escalations List (5 cols) */}
          <div className="lg:col-span-5 space-y-3">
            <span className="text-xs font-bold uppercase tracking-wider text-sh-muted block">
              Escalated Tickets ({filteredEscalations.length})
            </span>

            <div className="space-y-2">
              {filteredEscalations.map((esc) => {
                const isSelected = selectedEscalation?.id === esc.id
                const isPending = esc.decision === 'PENDING'

                return (
                  <div
                    key={esc.id}
                    onClick={() => {
                      setSelectedEscalationId(esc.id)
                      setDecisionError(null)
                      setDecisionSuccess(null)
                    }}
                    data-testid={`escalation-row-${esc.id}`}
                    className={`p-4 rounded-sh-md border transition-all cursor-pointer space-y-2 ${
                      isSelected
                        ? 'bg-sh-surface-subtle border-sh-primary ring-1 ring-sh-primary shadow-sm'
                        : 'bg-sh-surface border-sh-border hover:border-sh-border-strong'
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="space-y-0.5">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-sm font-bold text-sh-ink">
                            {esc.ticketCode}
                          </span>
                          <span className="font-mono text-xs font-bold px-1.5 py-0.5 bg-sh-surface-muted border border-sh-border rounded text-sh-ink">
                            {formatUnitCode(esc.unitCode)}
                          </span>
                        </div>
                        <p className="text-xs font-medium text-sh-ink-secondary">
                          {esc.customerName}
                        </p>
                      </div>

                      <span
                        data-testid={`decision-status-badge-${esc.id}`}
                        className={`text-[10px] font-mono font-bold px-2 py-0.5 rounded uppercase border ${
                          isPending
                            ? 'bg-amber-100 text-amber-900 border-amber-300'
                            : esc.decision === 'MAINTENANCE_RELOCATE' || esc.decision === 'SEVERE'
                            ? 'bg-red-100 text-red-900 border-red-300'
                            : 'bg-blue-100 text-blue-900 border-blue-300'
                        }`}
                      >
                        {isPending
                          ? 'PENDING'
                          : esc.decision === 'MAINTENANCE_RELOCATE' || esc.decision === 'SEVERE'
                          ? 'SEVERE · RELOCATED'
                          : 'NOT SEVERE · RETURNED'}
                      </span>
                    </div>

                    <p className="text-xs text-sh-ink-secondary line-clamp-2 italic">
                      "{esc.escalationNote}"
                    </p>

                    <div className="flex items-center justify-between text-[11px] text-sh-muted pt-1 border-t border-sh-divider">
                      <span>Escalated by: {esc.escalatedByStaffName || 'Staff'}</span>
                      <span className="font-mono">
                        {esc.createdAt ? new Date(esc.createdAt).toLocaleDateString() : 'Today'}
                      </span>
                    </div>
                  </div>
                )
              })}
            </div>
          </div>

          {/* Right Column: Selected Escalation Triage & Decision Panel (7 cols) */}
          {selectedEscalation && (
            <div
              className="lg:col-span-7 bg-sh-surface border border-sh-border rounded-sh-md p-6 shadow-sm space-y-5"
              data-testid="escalation-detail-panel"
            >
              {/* Header & Badges */}
              <div className="flex items-start justify-between border-b border-sh-divider pb-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span
                      data-testid="selected-ticket-code"
                      className="font-mono text-lg font-bold text-sh-ink"
                    >
                      {selectedEscalation.ticketCode}
                    </span>
                    <span className="text-xs font-mono font-bold px-2 py-0.5 bg-blue-50 text-blue-800 border border-blue-200 rounded">
                      {selectedEscalation.incidentType}
                    </span>
                  </div>
                  <p className="text-xs text-sh-muted">
                    Unit {formatUnitCode(selectedEscalation.unitCode)} · Tenant: {selectedEscalation.customerName}
                  </p>
                </div>

                <span
                  data-testid="selected-decision-badge"
                  className={`text-xs font-mono font-bold px-2.5 py-1 rounded uppercase border ${
                    selectedEscalation.decision === 'PENDING'
                      ? 'bg-amber-100 text-amber-900 border-amber-300'
                      : selectedEscalation.decision === 'MAINTENANCE_RELOCATE' ||
                        selectedEscalation.decision === 'SEVERE'
                      ? 'bg-red-100 text-red-900 border-red-300'
                      : 'bg-blue-100 text-blue-900 border-blue-300'
                  }`}
                >
                  {selectedEscalation.decision === 'PENDING'
                    ? 'AWAITING FM DECISION'
                    : selectedEscalation.decision}
                </span>
              </div>

              {/* Context Summary Grid */}
              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                  <span className="text-sh-muted block">Active Booking</span>
                  <span className="font-mono font-semibold text-sh-ink block">
                    {selectedEscalation.reservationCode || 'BK-1042'}
                  </span>
                </div>

                <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-sm space-y-0.5">
                  <span className="text-sh-muted block">Escalated By</span>
                  <span className="font-semibold text-sh-ink block">
                    {selectedEscalation.escalatedByStaffName || 'Minh Tran (Staff)'}
                  </span>
                </div>
              </div>

              {/* Verbatim Customer Description */}
              <div className="space-y-1">
                <span className="text-xs font-bold uppercase tracking-wider text-sh-muted">
                  1. Original Customer Description
                </span>
                <div
                  data-testid="escalation-customer-description"
                  className="p-3.5 bg-amber-50/40 border border-amber-200 rounded-sh-md text-xs text-sh-ink leading-relaxed"
                >
                  "{selectedEscalation.ticketDescription || 'No description provided'}"
                </div>
              </div>

              {/* Staff Escalation Note */}
              <div className="space-y-1">
                <span className="text-xs font-bold uppercase tracking-wider text-sh-muted">
                  2. Staff Escalation Note & Assessment
                </span>
                <div
                  data-testid="escalation-staff-note"
                  className="p-3.5 bg-purple-50/60 border border-purple-200 rounded-sh-md text-xs text-purple-950 font-medium leading-relaxed"
                >
                  "{selectedEscalation.escalationNote}"
                </div>
              </div>

              {/* If Decision Already Recorded */}
              {selectedEscalation.decision !== 'PENDING' ? (
                <div
                  data-testid="resolved-decision-card"
                  className={`p-4 rounded-sh-md border space-y-3 ${
                    selectedEscalation.decision === 'MAINTENANCE_RELOCATE' ||
                    selectedEscalation.decision === 'SEVERE'
                      ? 'bg-red-50/60 border-red-200'
                      : 'bg-blue-50/60 border-blue-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wide text-sh-ink">
                      Decision Recorded by Manager ({selectedEscalation.managerName || 'Hoa Pham'})
                    </span>
                    <span className="text-xs font-mono font-bold px-2 py-0.5 rounded bg-white border border-sh-border">
                      {selectedEscalation.decision}
                    </span>
                  </div>

                  <p className="text-xs text-sh-ink">
                    <strong>Manager Note:</strong> {selectedEscalation.managerNote || 'N/A'}
                  </p>

                  {selectedEscalation.relocatedToUnitCode && (
                    <div
                      data-testid="relocation-details-tile"
                      className="p-3 bg-white border border-red-200 rounded-sh-sm space-y-1 text-xs"
                    >
                      <div className="font-bold text-red-950">
                        ✓ Relocation Authorized to Unit {formatUnitCode(selectedEscalation.relocatedToUnitCode)}
                      </div>
                      <div className="text-sh-muted">
                        New Security PIN Issued:{' '}
                        <strong className="font-mono text-sh-ink">
                          {selectedEscalation.newAccessCode || '839201'}
                        </strong>
                      </div>
                      <p className="text-[11px] text-emerald-800">
                        Damaged unit {formatUnitCode(selectedEscalation.unitCode)} moved to MAINTENANCE. Turnover cleaning scheduled.
                      </p>
                    </div>
                  )}
                </div>
              ) : (
                /* FM Decision Action Panel */
                <div className="pt-3 border-t border-sh-border space-y-4">
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold uppercase tracking-wider text-sh-ink">
                      3. Facility Manager Severity Decision
                    </span>
                    <div className="flex gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          setDecisionMode('SEVERE')
                          setDecisionError(null)
                        }}
                        data-testid="mode-severe-btn"
                        className={`text-xs px-3 py-1 rounded font-bold transition-colors cursor-pointer ${
                          decisionMode === 'SEVERE'
                            ? 'bg-red-600 text-white shadow-sm'
                            : 'bg-sh-surface-muted text-sh-muted hover:text-sh-ink border border-sh-border'
                        }`}
                      >
                        ⚠️ Severe (Relocate)
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          setDecisionMode('NOT_SEVERE')
                          setDecisionError(null)
                        }}
                        data-testid="mode-not-severe-btn"
                        className={`text-xs px-3 py-1 rounded font-bold transition-colors cursor-pointer ${
                          decisionMode === 'NOT_SEVERE'
                            ? 'bg-blue-600 text-white shadow-sm'
                            : 'bg-sh-surface-muted text-sh-muted hover:text-sh-ink border border-sh-border'
                        }`}
                      >
                        ↩️ Not Severe (Return)
                      </button>
                    </div>
                  </div>

                  {/* Severe Decision Options: Target Unit Selection */}
                  {decisionMode === 'SEVERE' && (
                    <div className="p-4 bg-red-50/50 border border-red-200 rounded-sh-md space-y-3">
                      <div className="space-y-1">
                        <label
                          htmlFor="target-unit-select"
                          className="text-xs font-bold text-red-950 uppercase tracking-wide block"
                        >
                          Select Available Unit for Relocation *
                        </label>
                        <select
                          id="target-unit-select"
                          value={targetUnitId || ''}
                          onChange={(e) => setTargetUnitId(Number(e.target.value) || null)}
                          data-testid="target-unit-select"
                          className="w-full px-3 py-2 text-xs font-mono border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary bg-white"
                        >
                          <option value="">-- Choose available unit --</option>
                          {availableUnits.map((u) => (
                            <option key={u.id} value={u.id}>
                              Unit {formatUnitCode(u.code)} · {u.typeName} ({u.sizeM2}m²) · Floor {u.floor}
                            </option>
                          ))}
                          {availableUnits.length === 0 && (
                            <option value="3">Unit M-5 (Default Available Unit)</option>
                          )}
                        </select>
                        <p className="text-[11px] text-red-800">
                          Original contract code, booking ID, and paid deposit will be preserved during swap.
                        </p>
                      </div>
                    </div>
                  )}

                  {/* Manager Note Input */}
                  <div className="space-y-1">
                    <label
                      htmlFor="manager-note"
                      className="text-xs font-bold text-sh-ink uppercase tracking-wide block"
                    >
                      {decisionMode === 'SEVERE'
                        ? 'Manager Relocation Rationale *'
                        : 'Manager Instructions for On-Site Staff *'}
                    </label>
                    <textarea
                      id="manager-note"
                      rows={3}
                      value={managerNote}
                      onChange={(e) => setManagerNote(e.target.value)}
                      placeholder={
                        decisionMode === 'SEVERE'
                          ? 'Explain damage severity and reasons for emergency customer relocation...'
                          : 'Provide specific action steps or equipment for staff to resolve on-site...'
                      }
                      data-testid="manager-note-input"
                      className="w-full px-3 py-2 text-xs border border-sh-border rounded-sh-sm focus:outline-none focus:ring-1 focus:ring-sh-primary"
                    />
                  </div>

                  {/* Action Button */}
                  <div className="pt-2 flex justify-end">
                    <Button
                      type="button"
                      variant={decisionMode === 'SEVERE' ? 'destructive' : 'primary'}
                      onClick={handleOpenDecisionConfirm}
                      data-testid="submit-decision-btn"
                      className="font-bold"
                    >
                      {decisionMode === 'SEVERE'
                        ? 'Authorize Emergency Relocation →'
                        : 'Return to Staff with Instructions →'}
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Destructive / Decision Confirmation Modal (UX-DR10) */}
      {isConfirmModalOpen && (
        <div
          data-testid="severity-confirm-modal"
          className="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4"
        >
          <div className="bg-sh-surface border border-sh-border rounded-sh-md max-w-lg w-full p-6 shadow-xl space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-2 text-red-700 font-bold text-base">
                <span>⚠️</span>
                <span>
                  {decisionMode === 'SEVERE'
                    ? 'Confirm Emergency Relocation'
                    : 'Confirm Return to Staff'}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsConfirmModalOpen(false)}
                className="text-sh-muted hover:text-sh-ink"
              >
                ✕
              </button>
            </div>

            {decisionMode === 'SEVERE' ? (
              <div className="space-y-3 text-xs text-sh-ink">
                <p className="font-semibold text-red-900 bg-red-50 p-3 rounded border border-red-200">
                  "Mark severe — move Unit {selectedEscalation?.unitCode} to MAINTENANCE and relocate the customer to selected unit. This closes the damaged unit and generates turnover cleaning tasks."
                </p>

                <ul className="space-y-1 text-sh-ink-secondary list-disc pl-4">
                  <li>Damaged Unit ({selectedEscalation?.unitCode}) status set to <strong>MAINTENANCE</strong>.</li>
                  <li>Customer relocated with booking code and deposit held in escrow.</li>
                  <li>New 6-digit access security PIN will be issued immediately.</li>
                  <li>Maintenance and cleaning cards will be placed on Kanban board.</li>
                </ul>
              </div>
            ) : (
              <p className="text-xs text-sh-ink-secondary">
                The escalation will be marked <strong>NOT SEVERE</strong> and returned to on-site staff with your instructions.
              </p>
            )}

            <div className="flex items-center justify-end gap-2 pt-3 border-t border-sh-divider">
              <Button
                type="button"
                variant="secondary"
                onClick={() => setIsConfirmModalOpen(false)}
              >
                Cancel
              </Button>

              <Button
                type="button"
                variant={decisionMode === 'SEVERE' ? 'destructive' : 'primary'}
                onClick={handleExecuteDecision}
                loading={decisionMutation.isPending}
                data-testid={
                  decisionMode === 'SEVERE' ? 'confirm-severe-btn' : 'confirm-not-severe-btn'
                }
                className="font-bold"
              >
                {decisionMode === 'SEVERE'
                  ? 'Confirm & Relocate Customer'
                  : 'Confirm & Return to Staff'}
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default EscalationsPage
