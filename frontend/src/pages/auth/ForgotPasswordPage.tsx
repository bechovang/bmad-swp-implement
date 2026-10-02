import React, { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { forgotPasswordApi } from '../../api/auth'

export function ForgotPasswordPage() {
  const { user, isAuthenticated, getRoleLanding } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(getRoleLanding(user.role), { replace: true })
    }
  }, [isAuthenticated, user, getRoleLanding, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!email.trim()) {
      setError('Please enter your email address.')
      return
    }
    setError(null)
    setIsLoading(true)
    try {
      const res = await forgotPasswordApi({ email: email.trim() })
      setMessage(
        res.message ||
          'If an account exists with that email, a password reset link has been sent.'
      )
    } catch {
      setMessage('If an account exists with that email, a password reset link has been sent.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-sh-app-bg flex flex-col justify-center items-center px-4 py-12">
      <div className="mb-6 flex flex-col items-center gap-2 text-center">
        <div className="w-10 h-10 rounded-sh-md bg-sh-primary flex items-center justify-center text-white text-base font-bold shadow-sh-button-primary">
          SH
        </div>
        <h1 className="typography-headline text-sh-ink font-semibold text-xl">StorageHub</h1>
      </div>

      <Card className="w-full max-w-[420px] p-6 shadow-sh-card border border-sh-border bg-sh-surface">
        <h2 className="typography-headline text-sh-ink font-semibold mb-1 text-center">
          Reset Password
        </h2>
        <p className="typography-meta text-sh-muted text-center mb-6">
          Enter your email address to receive a password reset link
        </p>

        {message ? (
          <div className="flex flex-col gap-4">
            <div
              role="status"
              className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md text-[13px] text-sh-ink-secondary"
            >
              {message}
            </div>
            <div className="pt-2 text-center">
              <Link to="/login" className="text-sh-primary font-semibold hover:underline text-[13px]">
                Return to sign in
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="Email address"
              type="email"
              placeholder="you@example.com"
              value={email}
              onChange={(e) => {
                setEmail(e.target.value)
                if (error) setError(null)
              }}
              error={error || undefined}
              required
              autoFocus
            />

            <div className="pt-2">
              <Button
                type="submit"
                variant="primary"
                size="page"
                className="w-full"
                isLoading={isLoading}
              >
                Send reset link
              </Button>
            </div>

            <div className="pt-2 text-center">
              <Link to="/login" className="text-sh-muted hover:text-sh-ink text-[13px]">
                Cancel and return to sign in
              </Link>
            </div>
          </form>
        )}
      </Card>
    </div>
  )
}
