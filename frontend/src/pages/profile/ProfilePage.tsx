import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from '../../components/ui/Card'
import { Button } from '../../components/ui/Button'
import { RoleChip } from '../../components/ui/RoleChip'

export function ProfilePage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  if (!user) return null

  return (
    <div className="max-w-[640px] mx-auto py-6">
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>User Profile</CardTitle>
              <CardDescription>View your account information and session details</CardDescription>
            </div>
            <RoleChip role={user.role} />
          </div>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <span className="typography-label text-sh-muted block mb-1">Full Name</span>
              <p className="typography-body-strong text-sh-ink">{user.fullName}</p>
            </div>

            <div>
              <span className="typography-label text-sh-muted block mb-1">Email Address</span>
              <p className="typography-body text-sh-ink font-mono text-[12.5px]">{user.email}</p>
            </div>

            <div>
              <span className="typography-label text-sh-muted block mb-1">Phone Number</span>
              <p className="typography-body text-sh-ink">{user.phone || '—'}</p>
            </div>

            <div>
              <span className="typography-label text-sh-muted block mb-1">Role Assigned</span>
              <p className="typography-body text-sh-ink">{user.role}</p>
            </div>
          </div>
        </CardContent>

        <CardFooter className="justify-between">
          <span className="typography-meta text-sh-muted">
            Signed in as <strong className="text-sh-ink font-medium">{user.email}</strong>
          </span>
          <Button variant="destructive" onClick={handleLogout}>
            Log out
          </Button>
        </CardFooter>
      </Card>
    </div>
  )
}
