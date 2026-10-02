import { useParams } from 'react-router-dom'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'

export interface PlaceholderProps {
  title: string
  description: string
  epic?: string
}

export function PlaceholderView({ title, description, epic = 'Upcoming' }: PlaceholderProps) {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="typography-display text-sh-ink font-semibold">{title}</h1>
          <p className="typography-meta text-sh-muted mt-1">{description}</p>
        </div>
        <Badge status="neutral" showDot={false}>
          {epic}
        </Badge>
      </div>

      <Card className="p-8 text-center flex flex-col items-center justify-center min-h-[320px] bg-sh-surface border-dashed">
        <div className="w-12 h-12 rounded-sh-md bg-sh-surface-muted text-sh-muted flex items-center justify-center font-mono text-sm mb-3">
          SH
        </div>
        <h3 className="typography-headline text-sh-ink font-semibold">{title}</h3>
        <p className="typography-body text-sh-ink-secondary max-w-[420px] mt-1">
          {description}. This workspace interface will be implemented in subsequent epics.
        </p>
      </Card>
    </div>
  )
}

export function BrowseUnitsPage() {
  return (
    <PlaceholderView
      title="Browse Units"
      description="Explore lockable storage units, check real-time availability, and make bookings"
      epic="Epic 2 & Epic 3"
    />
  )
}

export function MyRentalsPage() {
  return (
    <PlaceholderView
      title="My Rentals"
      description="Manage your ongoing self-storage rentals, access codes, and payment history"
      epic="Epic 3"
    />
  )
}

export function SupportPage() {
  return (
    <PlaceholderView
      title="Support"
      description="Contact facility staff, submit inquiries, and track ticket resolutions"
      epic="Epic 6"
    />
  )
}

export function TaskBoardPage() {
  return (
    <PlaceholderView
      title="Task Board"
      description="Daily task queue for facility staff: check-ins, check-outs, cleaning, and maintenance"
      epic="Epic 4"
    />
  )
}

export function FacilityOverviewPage() {
  return (
    <PlaceholderView
      title="Facility Overview"
      description="Real-time occupancy rates, unit state map, and manager dashboard"
      epic="Epic 5"
    />
  )
}

export function StaffingPage() {
  return (
    <PlaceholderView
      title="Staff & Shifts"
      description="Staff rosters, shift scheduling, and assignment records"
      epic="Epic 5"
    />
  )
}

export function OperationsPage() {
  return (
    <PlaceholderView
      title="Operations"
      description="Facility operations, turnover buffer configuration, and maintenance oversight"
      epic="Epic 5"
    />
  )
}

export function ActivityLogPage() {
  return (
    <PlaceholderView
      title="Activity Log"
      description="Append-only immutable audit trail of all operational events and status transitions"
      epic="Epic 1 & Epic 5"
    />
  )
}

export function EscalationsPage() {
  return (
    <PlaceholderView
      title="Escalations"
      description="Overdue tasks, customer disputes, and urgent operational alerts"
      epic="Epic 5"
    />
  )
}

export function BusinessOverviewPage() {
  return (
    <PlaceholderView
      title="Business Overview"
      description="High-level financial KPIs, revenue metrics, and fleet utilization"
      epic="Epic 7"
    />
  )
}

export function PolicyPage() {
  return (
    <PlaceholderView
      title="Policy"
      description="System policies, deposit rules, pricing tables, and version histories"
      epic="Epic 7"
    />
  )
}

export function ReportsPage() {
  return (
    <PlaceholderView
      title="Reports"
      description="Financial exports, revenue breakdown, and utilization reporting"
      epic="Epic 7"
    />
  )
}

export function UserManagementPage() {
  return (
    <PlaceholderView
      title="User Management"
      description="Manage system accounts, user status (active/inactive/locked), and role assignments"
      epic="Epic 1 & Epic 8"
    />
  )
}

export function LoginHistoryPage() {
  return (
    <PlaceholderView
      title="Login History"
      description="Security audit log tracking authentication attempts, IPs, and timestamps"
      epic="Epic 1"
    />
  )
}

export function NotificationsPage() {
  return (
    <PlaceholderView
      title="Notification Center"
      description="Persistent role-scoped notifications, action deep-links, and alert history"
      epic="Story 1.6"
    />
  )
}

export function UnitDetailPage() {
  const { code } = useParams<{ code: string }>()
  return (
    <PlaceholderView
      title={`Unit Detail: ${code || ''}`}
      description="Detailed specifications, dimensions, photos, and booking timeline"
      epic="Epic 2"
    />
  )
}

export function RentalDetailPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <PlaceholderView
      title={`Rental Agreement #${id || ''}`}
      description="Contract terms, payment schedule, access PIN, and renewal options"
      epic="Epic 3"
    />
  )
}

export function TaskDetailPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <PlaceholderView
      title={`Task #${id || ''}`}
      description="Task instructions, checklist steps, photo capture, and completion sign-off"
      epic="Epic 4"
    />
  )
}

export function SupportTicketDetailPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <PlaceholderView
      title={`Support Ticket #${id || ''}`}
      description="Conversation thread, resolution notes, and status updates"
      epic="Epic 6"
    />
  )
}

export function EscalationDetailPage() {
  const { id } = useParams<{ id: string }>()
  return (
    <PlaceholderView
      title={`Escalation #${id || ''}`}
      description="Escalation investigation, resolution actions, and manager override"
      epic="Epic 5"
    />
  )
}

export function PolicyEditorPage() {
  const { version } = useParams<{ version: string }>()
  return (
    <PlaceholderView
      title={`Policy Editor: Version ${version || ''}`}
      description="Review and publish system-wide policy revisions"
      epic="Epic 7"
    />
  )
}
