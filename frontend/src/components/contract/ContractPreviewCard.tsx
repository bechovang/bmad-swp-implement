import React from 'react'
import type { ContractDto, ContractStatus } from '../../types/contract'
import { Card } from '../ui/Card'
import { Badge, type BadgeVariant } from '../ui/Badge'
import { Button } from '../ui/Button'
import { formatMoney, formatUnitCode } from '../../lib/format'

export interface ContractPreviewCardProps {
  contract: ContractDto
  isStaff?: boolean
  onReDraft?: () => void
  isReDrafting?: boolean
  className?: string
}

function getContractStatusBadge(status: ContractStatus): { variant: BadgeVariant; label: string } {
  switch (status) {
    case 'DRAFT':
      return { variant: 'warning', label: 'Draft' }
    case 'PRINTED':
      return { variant: 'reserved', label: 'Printed' }
    case 'SIGNED':
      return { variant: 'success', label: 'Signed' }
    case 'ACTIVE':
      return { variant: 'success', label: 'Active' }
    case 'CLOSED':
      return { variant: 'neutral', label: 'Closed' }
    case 'SUPERSEDED':
      return { variant: 'neutral', label: 'Superseded' }
    default:
      return { variant: 'neutral', label: status }
  }
}

export function ContractPreviewCard({
  contract,
  isStaff = false,
  onReDraft,
  isReDrafting = false,
  className = '',
}: ContractPreviewCardProps) {
  // Parse snapshot object if available or parse from JSON string
  const snapshot = React.useMemo(() => {
    if (contract.snapshot) return contract.snapshot
    if (contract.contentSnapshot) {
      try {
        return JSON.parse(contract.contentSnapshot)
      } catch {
        return null
      }
    }
    return null
  }, [contract])

  const statusBadge = getContractStatusBadge(contract.status)
  const isDraft = contract.status === 'DRAFT'
  const isSuperseded = contract.status === 'SUPERSEDED'

  const handlePrint = () => {
    if (typeof window !== 'undefined') {
      window.print()
    }
  }

  const policyVersion = snapshot?.policyVersion || contract.policyVersion || 'v3'
  const unitCode = snapshot?.unitCode || 'Unit'
  const monthlyRate = snapshot?.monthlyRate ?? 0
  const baseRent = snapshot?.baseRent ?? 0
  const totalRent = snapshot?.totalRent ?? 0
  const depositAmount = snapshot?.depositAmount ?? 0
  const durationMonths = snapshot?.durationMonths ?? 1
  const startDate = snapshot?.startDate || '—'
  const endDate = snapshot?.endDate || '—'
  const customerName = snapshot?.customerName || 'Customer'
  const customerEmail = snapshot?.customerEmail || '—'
  const customerPhone = snapshot?.customerPhone || '—'
  const facilityName = snapshot?.facilityName || 'Tan Binh Depot'
  const facilityAddress = snapshot?.facilityAddress || '45 Nguyen Van Troi, Tan Binh, Ho Chi Minh City'
  const zoneCode = snapshot?.zoneCode || 'A'
  const floor = snapshot?.floor ?? 1
  const sizeM2 = snapshot?.sizeM2 != null ? `${snapshot.sizeM2} m²` : '5.0 m²'

  return (
    <Card
      data-testid="contract-preview-card"
      className={`p-6 bg-sh-surface border border-sh-border rounded-sh-md shadow-sm space-y-6 ${className}`}
    >
      {/* Header / Meta Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-sh-divider pb-4">
        <div>
          <div className="flex items-center gap-2.5 flex-wrap">
            <span className="text-xs uppercase font-semibold text-sh-muted tracking-wider">
              Self-Service Rental Agreement
            </span>
            <Badge status={statusBadge.variant} showDot>
              {statusBadge.label}
            </Badge>
            {isSuperseded && (
              <span className="text-xs text-sh-muted italic">
                (Superseded revision)
              </span>
            )}
          </div>
          <h2 className="typography-headline text-lg font-bold text-sh-ink mt-1 flex items-center gap-2">
            <span>Contract</span>
            <span className="font-mono text-sh-primary tracking-wide">{contract.code}</span>
          </h2>
          {contract.reservationCode && (
            <p className="typography-meta text-sh-muted text-xs mt-0.5">
              Ref Reservation <span className="font-mono text-sh-ink font-semibold">{contract.reservationCode}</span> · Locked Policy <span className="font-semibold text-sh-ink">{policyVersion}</span>
            </p>
          )}
        </div>

        <div className="flex items-center gap-2 print:hidden">
          <Button
            type="button"
            variant="secondary"
            size="sm"
            onClick={handlePrint}
            data-testid="print-contract-btn"
            aria-label="Print Agreement"
            className="cursor-pointer"
          >
            <span className="mr-1.5">🖨</span> Print Agreement
          </Button>

          {isStaff && isDraft && onReDraft && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={onReDraft}
              disabled={isReDrafting}
              data-testid="redraft-contract-btn"
              className="text-xs text-sh-ink-secondary hover:text-sh-ink"
            >
              {isReDrafting ? 'Re-drafting...' : 'Re-draft'}
            </Button>
          )}
        </div>
      </div>

      {/* Contract Notice / Ritual Prompt */}
      {isDraft && (
        <div className="p-3.5 bg-sh-surface-subtle border-l-4 border-l-sh-warning border-sh-border rounded-sh-sm text-xs text-sh-ink-secondary space-y-1">
          <p className="font-semibold text-sh-ink flex items-center gap-1.5">
            <span>ℹ️</span> Auto-Drafted Agreement — Sign at Check-in
          </p>
          <p>
            This agreement is automatically generated from your locked booking snapshot and Rental Policy{' '}
            <span className="font-semibold text-sh-ink">{policyVersion}</span>. The physical document will be signed in person at the facility counter before unit keys and PIN codes are issued.
          </p>
        </div>
      )}

      {/* Agreement Parties */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
        <div className="p-4 bg-sh-surface-subtle border border-sh-border rounded-sh-md space-y-2">
          <div className="font-semibold text-sh-muted uppercase tracking-wider text-[11px]">
            Tenant / Customer (Party A)
          </div>
          <div className="font-bold text-sh-ink text-sm">{customerName}</div>
          <div className="text-sh-ink-secondary">
            <div><span className="text-sh-muted">Email:</span> {customerEmail}</div>
            <div><span className="text-sh-muted">Phone:</span> {customerPhone}</div>
          </div>
        </div>

        <div className="p-4 bg-sh-surface-subtle border border-sh-border rounded-sh-md space-y-2">
          <div className="font-semibold text-sh-muted uppercase tracking-wider text-[11px]">
            Storage Facility Provider (Party B)
          </div>
          <div className="font-bold text-sh-ink text-sm">StorageHub Vietnam — {facilityName}</div>
          <div className="text-sh-ink-secondary">
            <div><span className="text-sh-muted">Address:</span> {facilityAddress}</div>
            <div><span className="text-sh-muted">Support:</span> 1900-STORAGE / support@storagehub.vn</div>
          </div>
        </div>
      </div>

      {/* Storage Unit & Period Specifications */}
      <div className="space-y-3">
        <h3 className="text-xs font-semibold text-sh-muted uppercase tracking-wider">
          Storage Unit & Term Schedule
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
          <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
            <span className="text-sh-muted block">Unit Code</span>
            <span className="font-mono text-sm font-bold text-sh-ink">
              {formatUnitCode(unitCode)}
            </span>
            <span className="text-[11px] text-sh-muted block">Zone {zoneCode} · Floor {floor}</span>
          </div>
          <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
            <span className="text-sh-muted block">Unit Dimensions</span>
            <span className="text-sm font-semibold text-sh-ink">{sizeM2}</span>
            <span className="text-[11px] text-sh-muted block">Climate controlled</span>
          </div>
          <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
            <span className="text-sh-muted block">Commencement</span>
            <span className="text-sm font-semibold text-sh-ink font-mono">{startDate}</span>
            <span className="text-[11px] text-sh-muted block">Check-in date</span>
          </div>
          <div className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
            <span className="text-sh-muted block">Expiration</span>
            <span className="text-sm font-semibold text-sh-ink font-mono">{endDate}</span>
            <span className="text-[11px] text-sh-muted block">({durationMonths} {durationMonths > 1 ? 'months' : 'month'})</span>
          </div>
        </div>
      </div>

      {/* Financial Agreement & Deposit Ledger */}
      <div className="space-y-3">
        <h3 className="text-xs font-semibold text-sh-muted uppercase tracking-wider">
          Locked Financial Terms & Rent Schedule
        </h3>
        <div className="overflow-x-auto border border-sh-border rounded-sh-md">
          <table className="w-full text-xs text-left">
            <thead className="bg-sh-surface-subtle border-b border-sh-divider text-sh-muted">
              <tr>
                <th className="py-2.5 px-4 font-medium">Item Description</th>
                <th className="py-2.5 px-4 font-medium">Calculation Base</th>
                <th className="py-2.5 px-4 font-medium text-right">Amount (VND)</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-sh-divider text-sh-ink">
              <tr>
                <td className="py-2.5 px-4 font-medium">Monthly Unit Base Rent</td>
                <td className="py-2.5 px-4 text-sh-muted">1 month standard rate</td>
                <td className="py-2.5 px-4 text-right font-mono font-medium">
                  {formatMoney(monthlyRate)}
                </td>
              </tr>
              <tr>
                <td className="py-2.5 px-4 font-medium">Total Term Rent</td>
                <td className="py-2.5 px-4 text-sh-muted">
                  {formatMoney(monthlyRate)} × {durationMonths} {durationMonths > 1 ? 'months' : 'month'}
                </td>
                <td className="py-2.5 px-4 text-right font-mono font-medium">
                  {formatMoney(totalRent || baseRent)}
                </td>
              </tr>
              <tr className="bg-sh-surface-subtle/50">
                <td className="py-2.5 px-4 font-medium text-sh-primary">
                  Refundable Security Deposit (10%)
                </td>
                <td className="py-2.5 px-4 text-sh-muted">
                  Held throughout rental session · Policy {policyVersion}
                </td>
                <td className="py-2.5 px-4 text-right font-mono font-bold text-sh-ink">
                  {formatMoney(depositAmount)}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Standard Terms Excerpt */}
      <div className="pt-2 text-[11px] text-sh-muted leading-relaxed space-y-1 border-t border-sh-divider">
        <p className="font-semibold text-sh-ink">Terms of Agreement & House Rules:</p>
        <ol className="list-decimal list-inside space-y-0.5">
          <li>The Security Deposit is refundable upon final checkout inspection minus any unpaid damage or settlement charges.</li>
          <li>Access codes and keys are non-transferable and strictly allocated to the registered Tenant.</li>
          <li>Rental extensions must be submitted prior to the term expiration date through the StorageHub portal.</li>
        </ol>
      </div>

      {/* Signed Photo Attachment Preview (Story 3.4) */}
      {contract.signedPhotoUrl && (
        <div className="pt-3 border-t border-sh-divider space-y-2" data-testid="signed-contract-attachment">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-sh-muted uppercase tracking-wider">
              Signed Contract Document on Record
            </span>
            <span className="text-[11px] font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
              Verified Attachment
            </span>
          </div>
          <div className="flex items-center gap-4 p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md">
            <img
              src={contract.signedPhotoUrl}
              alt="Signed contract attachment"
              className="w-20 h-20 object-cover rounded border border-sh-border bg-white"
              onError={(e) => {
                ;(e.target as HTMLElement).style.display = 'none'
              }}
            />
            <div className="text-xs space-y-1">
              <span className="font-semibold text-sh-ink block">Physical Contract Signed Copy</span>
              <a
                href={contract.signedPhotoUrl}
                target="_blank"
                rel="noreferrer"
                className="text-sh-primary hover:underline text-[11px] font-medium inline-flex items-center gap-1"
              >
                View Full Document ↗
              </a>
            </div>
          </div>
        </div>
      )}

      {/* Printable Signature Footers for In-Person Ritual */}
      <div className="hidden print:grid grid-cols-2 gap-12 pt-12 text-xs">
        <div className="text-center space-y-16">
          <p className="font-semibold text-sh-ink">Party A (Tenant)</p>
          <div className="border-t border-sh-divider pt-2 text-sh-muted">
            Signature & Full Name: {customerName}
          </div>
        </div>
        <div className="text-center space-y-16">
          <p className="font-semibold text-sh-ink">Party B (Representative)</p>
          <div className="border-t border-sh-divider pt-2 text-sh-muted">
            StorageHub Authorized Officer
          </div>
        </div>
      </div>
    </Card>
  )
}
