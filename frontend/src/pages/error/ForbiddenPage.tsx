import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { APP_ROLES, type AppRole } from '../../tokens'

export function ForbiddenPage() {
  const { user, getRoleLanding } = useAuth()
  const navigate = useNavigate()

  const landingPath = user ? getRoleLanding(user.role) : '/login'
  const roleConfig = user?.role ? APP_ROLES[user.role as AppRole] : null
  const landingScreen = roleConfig ? roleConfig.screen : 'Browse Units'
  const ctaText = user ? `Return to ${landingScreen}` : 'Return to Sign In'

  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] px-4">
      <Card className="max-w-[480px] w-full p-8 text-center flex flex-col items-center gap-4">
        <div className="w-14 h-14 rounded-sh-md bg-sh-error-tint border border-sh-error-border flex items-center justify-center text-sh-error font-mono font-bold text-xl">
          403
        </div>

        <div className="space-y-1">
          <h1 className="typography-headline text-sh-ink font-semibold">Access Denied</h1>
          <p className="typography-body text-sh-ink-secondary">
            You do not have permission to access this page. This surface is restricted to authorized
            roles.
          </p>
        </div>

        <div className="pt-2">
          <Button onClick={() => navigate(landingPath)} variant="primary">
            {ctaText}
          </Button>
        </div>
      </Card>
    </div>
  )
}
