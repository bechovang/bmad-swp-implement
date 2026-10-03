import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Card } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { Badge } from '../../components/ui/Badge'
import { Skeleton } from '../../components/ui/Skeleton'
import { EmptyState } from '../../components/ui/EmptyState'
import { NewSupportModal } from './NewSupportModal'
import { SupportDetailDrawer } from '../../components/support/SupportDetailDrawer'
import { getSupportTickets } from '../../api/support'
import { getMyReservations } from '../../api/reservation'
import { formatUnitCode } from '../../lib/format'
import type { SupportTicketDto, SupportTicketStatus } from '../../types/support'

export function SupportPage() {
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [statusFilter, setStatusFilter] = useState<string>('ALL')
  const [selectedTicket, setSelectedTicket] = useState<SupportTicketDto | null>(null)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  const {
    data: tickets = [],
    isLoading: isTicketsLoading,
    refetch,
  } = useQuery({
    queryKey: ['support-tickets'],
    queryFn: () => getSupportTickets(),
  })

  const { data: reservations = [] } = useQuery({
    queryKey: ['my-reservations'],
    queryFn: () => getMyReservations(),
  })

  const rentalOptions = reservations
    .filter((r) => r.status === 'CHECKED_IN' || r.status === 'CHECKOUT_REQUESTED')
    .map((r) => ({
      unitId: r.unitId || 1,
      unitCode: r.unitCode,
      reservationCode: r.code,
    }))

  const filteredTickets = tickets.filter((t) => {
    if (statusFilter === 'ALL') return true
    return t.status === statusFilter
  })

  const handleOpenDrawer = (ticket: SupportTicketDto) => {
    setSelectedTicket(ticket)
    setIsDrawerOpen(true)
  }

  const handleCloseDrawer = () => {
    setIsDrawerOpen(false)
  }

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

  return (
    <div className="space-y-6 max-w-5xl mx-auto pb-12" data-testid="support-page">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="typography-display text-sh-ink font-bold">Support Requests</h1>
          <p className="typography-meta text-sh-muted mt-1">
            Submit inquiries and report access or maintenance issues for your rented units.
          </p>
        </div>
        <Button
          variant="primary"
          size="sm"
          onClick={() => setIsModalOpen(true)}
          data-testid="open-support-modal-btn"
        >
          New Support Ticket
        </Button>
      </div>

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-sh-border pb-2">
        {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'ESCALATED'].map((status) => (
          <button
            key={status}
            onClick={() => setStatusFilter(status)}
            className={`px-3 py-1.5 text-xs font-semibold rounded-sh-sm transition-colors cursor-pointer ${
              statusFilter === status
                ? 'bg-sh-primary text-white'
                : 'text-sh-muted hover:text-sh-ink hover:bg-sh-surface-subtle'
            }`}
          >
            {status}
          </button>
        ))}
      </div>

      {/* Ticket List */}
      {isTicketsLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20 w-full rounded-sh-md" />
          <Skeleton className="h-20 w-full rounded-sh-md" />
          <Skeleton className="h-20 w-full rounded-sh-md" />
        </div>
      ) : filteredTickets.length === 0 ? (
        <EmptyState
          title="No support tickets found"
          description={
            statusFilter === 'ALL'
              ? 'You have not submitted any support tickets yet.'
              : `No tickets with status ${statusFilter}.`
          }
          action={
            statusFilter === 'ALL' ? (
              <Button
                variant="primary"
                size="sm"
                onClick={() => setIsModalOpen(true)}
                data-testid="empty-new-support-btn"
              >
                Open First Ticket
              </Button>
            ) : undefined
          }
        />
      ) : (
        <div className="space-y-3" data-testid="support-tickets-list">
          {filteredTickets.map((ticket: SupportTicketDto) => (
            <Card
              key={ticket.id}
              onClick={() => handleOpenDrawer(ticket)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  handleOpenDrawer(ticket)
                }
              }}
              tabIndex={0}
              role="button"
              aria-label={`View support ticket ${ticket.code}`}
              className="p-5 bg-sh-surface border-sh-border hover:border-sh-primary-border hover:shadow-xs transition-all rounded-sh-md shadow-xs cursor-pointer focus:outline-none focus:ring-2 focus:ring-sh-primary/20"
              data-testid={`support-ticket-card-${ticket.code}`}
            >
              <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-3 pointer-events-none">
                <div className="space-y-1.5">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-sh-primary bg-sh-primary-tint px-2 py-0.5 rounded border border-sh-primary-border">
                      {ticket.code}
                    </span>
                    <Badge status={getStatusBadgeVariant(ticket.status)} showDot>
                      {ticket.status}
                    </Badge>
                    <span className="text-xs text-sh-muted">·</span>
                    <span className="font-mono text-xs font-semibold text-sh-ink">
                      Unit {formatUnitCode(ticket.unitCode)}
                    </span>
                  </div>

                  <p className="text-xs font-semibold text-sh-ink mt-1">
                    {ticket.incidentType.replace('_', ' ')}
                  </p>
                  <p className="text-xs text-sh-ink-secondary line-clamp-2">
                    {ticket.description}
                  </p>
                </div>

                <div className="sm:text-right space-y-1 shrink-0">
                  <div className="text-[11px] text-sh-muted">
                    Assigned: <span className="font-semibold text-sh-ink">{ticket.assignedStaffName || 'Unassigned'}</span>
                  </div>
                  {ticket.createdAt && (
                    <div className="text-[11px] text-sh-faint font-mono">
                      {new Date(ticket.createdAt).toLocaleDateString('en-GB')}
                    </div>
                  )}
                  <span className="text-[11px] text-sh-primary font-semibold block mt-1">
                    View full thread →
                  </span>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      {/* Detail Slide-over Drawer (420px) */}
      <SupportDetailDrawer
        ticket={selectedTicket}
        isOpen={isDrawerOpen}
        onClose={handleCloseDrawer}
      />

      {/* New Support Modal */}
      <NewSupportModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => refetch()}
        rentals={rentalOptions}
      />
    </div>
  )
}
