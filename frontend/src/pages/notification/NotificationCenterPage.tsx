import { NotificationList } from '../../components/notification/NotificationCenter'
import { Card } from '../../components/ui/Card'

export function NotificationCenterPage() {
  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h1 className="typography-display text-sh-ink font-semibold">Notification Center</h1>
        <p className="typography-body text-sh-muted mt-1">
          Stay informed about critical payments, status changes, and operational tasks.
        </p>
      </div>

      <Card className="p-6">
        <NotificationList showHeaderControls={true} />
      </Card>
    </div>
  )
}

export default NotificationCenterPage
