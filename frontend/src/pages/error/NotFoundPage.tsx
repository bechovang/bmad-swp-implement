import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'

export function NotFoundPage() {
  const { user, getRoleLanding } = useAuth()
  const navigate = useNavigate()

  const homePath = user ? getRoleLanding(user.role) : '/login'

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] px-4">
      <Card className="max-w-[480px] w-full p-8 text-center flex flex-col items-center gap-4">
        <div className="w-14 h-14 rounded-sh-md bg-sh-surface-muted border border-sh-border flex items-center justify-center text-sh-muted font-mono font-bold text-xl">
          404
        </div>

        <div className="space-y-1">
          <h1 className="typography-headline text-sh-ink font-semibold">Page Not Found</h1>
          <p className="typography-body text-sh-ink-secondary">
            The page you are looking for does not exist or has been moved.
          </p>
        </div>

        <div className="pt-2">
          <Button onClick={() => navigate(homePath)} variant="primary">
            Go to Home
          </Button>
        </div>
      </Card>
    </div>
  )
}
