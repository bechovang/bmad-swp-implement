import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getMyReservations } from '../../api/rental'
import type { ReservationDto, ReservationStatus } from '../../types/rental'
import { Card } from '../../components/ui/Card'
import { Badge, type BadgeVariant } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/Tabs'
import { EmptyState } from '../../components/ui/EmptyState'
import { Skeleton } from '../../components/ui/Skeleton'
import { CheckInPassModal } from '../../components/rentals/CheckInPassModal'
import { formatMoney, formatUnitCode } from '../../lib/format'

function getStatusBadgeProps(status: ReservationStatus): { variant: BadgeVariant; label: string } {
  switch (status) {
    case 'PENDING_PAYMENT':
      return { variant: 'warning', label: 'Awaiting Deposit' }
    case 'RESERVED':
      return { variant: 'reserved', label: 'Reserved' }
    case 'CHECKED_IN':
      return { variant: 'success', label: 'Active Rental' }
    case 'CHECKOUT_REQUESTED':
      return { variant: 'warning', label: 'Checkout Requested' }
    case 'CLOSED':
      return { variant: 'neutral', label: 'Completed' }
    case 'EXPIRED':
      return { variant: 'neutral', label: 'Expired' }
    case 'CANCELLED':
      return { variant: 'neutral', label: 'Cancelled' }
    default:
      return { variant: 'neutral', label: status }
  }
}

export function MyRentalsPage() {
  const navigate = useNavigate()
  const [selectedTab, setSelectedTab] = useState<'active' | 'reservations' | 'history'>('active')
  const [passModalReservation, setPassModalReservation] = useState<ReservationDto | null>(null)

  const { data: reservations = [], isLoading, isError, refetch } = useQuery({
    queryKey: ['my-reservations'],
    queryFn: getMyReservations,
  })

  const activeRentals = reservations.filter(
    (r) => r.status === 'CHECKED_IN' || r.status === 'CHECKOUT_REQUESTED'
  )
  const pendingReservations = reservations.filter(
    (r) => r.status === 'RESERVED' || r.status === 'PENDING_PAYMENT'
  )
  const historyRentals = reservations.filter(
    (r) => r.status === 'CLOSED' || r.status === 'EXPIRED' || r.status === 'CANCELLED'
  )

  const renderCard = (reservation: ReservationDto) => {
    const statusProps = getStatusBadgeProps(reservation.status)
    const isReserved = reservation.status === 'RESERVED'
    const isPendingPayment = reservation.status === 'PENDING_PAYMENT'

    return (
      <Card
        key={reservation.id}
        data-testid={`rental-card-${reservation.id}`}
        className="p-5 flex flex-col md:flex-row md:items-center md:justify-between gap-4 bg-sh-surface border border-sh-border hover:border-sh-ink-secondary/30 transition-colors shadow-sm rounded-sh-md"
      >
        <div className="space-y-2 flex-1">
          <div className="flex flex-wrap items-center gap-2.5">
            <span className="font-mono text-lg font-bold text-sh-ink">
              {formatUnitCode(reservation.unitCode)}
            </span>
            <span className="text-xs text-sh-muted">
              {reservation.unitTypeName ? `${reservation.unitTypeName} Type` : ''}
              {reservation.sizeM2 ? ` · ${reservation.sizeM2} m²` : ''}
              {reservation.floor ? ` · Floor ${reservation.floor}` : ''}
            </span>
            <Badge status={statusProps.variant} showDot>
              {statusProps.label}
            </Badge>
          </div>

          <div className="text-xs text-sh-ink-secondary flex flex-wrap items-center gap-x-4 gap-y-1">
            <span>
              <strong className="text-sh-ink">Code:</strong>{' '}
              <span className="font-mono">{reservation.code}</span>
            </span>
            <span>
              <strong className="text-sh-ink">Facility:</strong>{' '}
              {reservation.facilityName || 'Tan Binh Depot'}
            </span>
            <span>
              <strong className="text-sh-ink">Timeline:</strong>{' '}
              {reservation.startDate} → {reservation.endDate} ({reservation.durationMonths}{' '}
              {reservation.durationMonths > 1 ? 'mos' : 'mo'})
            </span>
          </div>

          <div className="text-xs text-sh-ink-secondary flex flex-wrap items-center gap-x-4 gap-y-1 pt-1">
            <span>
              <strong className="text-sh-ink">Monthly:</strong>{' '}
              {formatMoney(reservation.monthlyRate)}/mo
            </span>
            <span>
              <strong className="text-sh-ink">Deposit:</strong>{' '}
              {formatMoney(reservation.depositAmount)}{' '}
              <span className="text-sh-muted">(Held)</span>
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 pt-2 md:pt-0 shrink-0 border-t md:border-t-0 border-sh-divider">
          {isReserved && (
            <Button
              variant="secondary"
              size="sm"
              onClick={() => setPassModalReservation(reservation)}
            >
              View Check-in Pass
            </Button>
          )}
          {isPendingPayment && (
            <Button
              variant="primary"
              size="sm"
              onClick={() => navigate(`/rentals/${reservation.id}`)}
            >
              Complete Deposit
            </Button>
          )}
          <Button
            variant={isPendingPayment ? 'secondary' : 'primary'}
            size="sm"
            onClick={() => navigate(`/rentals/${reservation.id}`)}
          >
            View Details
          </Button>
        </div>
      </Card>
    )
  }

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
        <div>
          <h1 className="typography-display text-sh-ink font-semibold">My Rentals</h1>
          <p className="typography-meta text-sh-muted mt-1">
            Manage your ongoing self-storage units, booking reservations, and rental history.
          </p>
        </div>
        <div>
          <Button variant="secondary" size="sm" onClick={() => navigate('/units')}>
            Browse Units
          </Button>
        </div>
      </div>

      {/* Tabs */}
      <Tabs
        value={selectedTab}
        onValueChange={(v) => setSelectedTab(v as 'active' | 'reservations' | 'history')}
      >
        <TabsList>
          <TabsTrigger value="active" data-testid="tab-active">
            Active Rentals ({activeRentals.length})
          </TabsTrigger>
          <TabsTrigger value="reservations" data-testid="tab-reservations">
            Reservations ({pendingReservations.length})
          </TabsTrigger>
          <TabsTrigger value="history" data-testid="tab-history">
            History ({historyRentals.length})
          </TabsTrigger>
        </TabsList>

        {isLoading ? (
          <div className="space-y-3 mt-6">
            <Skeleton className="h-24 w-full rounded-sh-md" />
            <Skeleton className="h-24 w-full rounded-sh-md" />
            <Skeleton className="h-24 w-full rounded-sh-md" />
          </div>
        ) : isError ? (
          <Card className="p-8 text-center mt-6">
            <p className="text-sh-error mb-3">Failed to load reservations.</p>
            <Button variant="secondary" size="sm" onClick={() => refetch()}>
              Retry
            </Button>
          </Card>
        ) : (
          <>
            <TabsContent value="active" className="space-y-3 mt-6">
              {activeRentals.length > 0 ? (
                activeRentals.map(renderCard)
              ) : (
                <Card className="p-8 bg-sh-surface border-dashed">
                  <EmptyState
                    title="You have no active rentals"
                    description="You do not currently have any active storage unit leases."
                    action={
                      <Button variant="primary" onClick={() => navigate('/units')}>
                        Browse Units
                      </Button>
                    }
                  />
                </Card>
              )}
            </TabsContent>

            <TabsContent value="reservations" className="space-y-3 mt-6">
              {pendingReservations.length > 0 ? (
                pendingReservations.map(renderCard)
              ) : (
                <Card className="p-8 bg-sh-surface border-dashed">
                  <EmptyState
                    title="You have no upcoming reservations"
                    description="Reserve a storage unit with a 10% refundable deposit to secure your space."
                    action={
                      <Button variant="primary" onClick={() => navigate('/units')}>
                        Browse Units
                      </Button>
                    }
                  />
                </Card>
              )}
            </TabsContent>

            <TabsContent value="history" className="space-y-3 mt-6">
              {historyRentals.length > 0 ? (
                historyRentals.map(renderCard)
              ) : (
                <Card className="p-8 bg-sh-surface border-dashed">
                  <EmptyState
                    title="You have no past rental history"
                    description="Completed and past rental records will appear here once closed."
                    action={
                      <Button variant="primary" onClick={() => navigate('/units')}>
                        Browse Units
                      </Button>
                    }
                  />
                </Card>
              )}
            </TabsContent>
          </>
        )}
      </Tabs>

      {/* Check-in Pass Modal */}
      <CheckInPassModal
        open={Boolean(passModalReservation)}
        onOpenChange={(open) => {
          if (!open) setPassModalReservation(null)
        }}
        reservation={passModalReservation}
      />
    </div>
  )
}
