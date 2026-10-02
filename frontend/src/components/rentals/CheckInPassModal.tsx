import { Modal, ModalHeader, ModalTitle, ModalDescription, ModalFooter } from '../ui/Modal'
import { Badge } from '../ui/Badge'
import { Button } from '../ui/Button'
import type { ReservationDto } from '../../types/rental'
import { formatMoney } from '../../lib/format'

export interface CheckInPassModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  reservation: ReservationDto | null
}

export function CheckInPassModal({ open, onOpenChange, reservation }: CheckInPassModalProps) {
  if (!reservation) return null

  const remainingBalance = reservation.totalRent ? reservation.totalRent : reservation.baseRent

  return (
    <Modal open={open} onOpenChange={onOpenChange}>
      <ModalHeader>
        <div className="flex items-center justify-between pr-4">
          <ModalTitle>Check-in Pass</ModalTitle>
          <Badge status="reserved" showDot>
            Confirmed
          </Badge>
        </div>
        <ModalDescription>
          Present this pass at the facility reception desk to complete contract signing and unit handover.
        </ModalDescription>
      </ModalHeader>

      <div className="space-y-4">
        {/* Reservation Monospace Code Banner */}
        <div className="p-4 bg-sh-surface-muted border border-sh-border rounded-sh-md text-center">
          <div className="text-xs uppercase tracking-wider text-sh-muted font-medium mb-1">
            Reservation Code
          </div>
          <div
            data-testid="check-in-pass-code"
            className="font-mono text-2xl md:text-3xl font-bold tracking-wider text-sh-ink select-all"
          >
            {reservation.code}
          </div>
          <div className="text-xs text-sh-ink-secondary mt-1">
            Unit <span className="font-semibold text-sh-ink">{reservation.unitCode}</span> · {reservation.facilityName || 'Tan Binh Depot'}
          </div>
        </div>

        {/* Booking Details Summary */}
        <div className="grid grid-cols-2 gap-2 text-xs p-3 bg-sh-surface border border-sh-border rounded-sh-md">
          <div>
            <span className="text-sh-muted block">Start Date:</span>
            <span className="font-medium text-sh-ink">{reservation.startDate}</span>
          </div>
          <div>
            <span className="text-sh-muted block">Duration:</span>
            <span className="font-medium text-sh-ink">{reservation.durationMonths} {reservation.durationMonths > 1 ? 'months' : 'month'}</span>
          </div>
          <div>
            <span className="text-sh-muted block">Deposit Paid (Held):</span>
            <span className="font-medium text-sh-success">{formatMoney(reservation.depositAmount)}</span>
          </div>
          <div>
            <span className="text-sh-muted block">Remaining Rent at Counter:</span>
            <span className="font-medium text-sh-ink">{formatMoney(remainingBalance)}</span>
          </div>
        </div>

        {/* Counter Instructions */}
        <div className="space-y-2.5 pt-2">
          <h4 className="typography-body-strong text-xs font-semibold text-sh-ink uppercase tracking-wider">
            Counter Check-in Instructions
          </h4>
          <ol className="space-y-2 text-xs text-sh-ink-secondary list-decimal list-inside pl-1">
            <li className="leading-relaxed">
              <span className="font-medium text-sh-ink">Bring National ID / Passport:</span> Present valid government identification at the front desk.
            </li>
            <li className="leading-relaxed">
              <span className="font-medium text-sh-ink">Sign Rental Agreement:</span> Review terms and sign the physical/digital rental contract.
            </li>
            <li className="leading-relaxed">
              <span className="font-medium text-sh-ink">Pay Remaining Balance:</span> Pay 100% rent balance ({formatMoney(remainingBalance)}) via Cash or PayOS QR at reception.
            </li>
          </ol>
        </div>
      </div>

      <ModalFooter>
        <Button variant="secondary" onClick={() => onOpenChange(false)}>
          Close
        </Button>
      </ModalFooter>
    </Modal>
  )
}
