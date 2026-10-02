import React, { useState, useEffect, useRef } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { ForgotPasswordModal } from './ForgotPasswordModal'

function sanitizeRedirect(path: string | null): string | null {
  if (!path) return null
  // Only accept relative paths starting with / (excluding //, /login, /register)
  if (!path.startsWith('/') || path.startsWith('//')) return null
  const cleanPath = path.split('?')[0].toLowerCase()
  if (cleanPath === '/login' || cleanPath === '/register') return null
  return path
}

export function LoginPage() {
  const { user, isAuthenticated, login, getRoleLanding } = useAuth()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const safeRedirect = sanitizeRedirect(searchParams.get('redirect'))
  const isExpired =
    searchParams.get('expired') === 'true' || searchParams.get('reason') === 'expired'
  const infoMessage = searchParams.get('message')

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [forgotOpen, setForgotOpen] = useState(false)

  const emailInputRef = useRef<HTMLInputElement>(null)
  const passwordInputRef = useRef<HTMLInputElement>(null)

  // Redirect if already authenticated
  useEffect(() => {
    if (isAuthenticated && user) {
      const destination = safeRedirect || getRoleLanding(user.role)
      navigate(destination, { replace: true })
    }
  }, [isAuthenticated, user, safeRedirect, getRoleLanding, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErrorMessage(null)

    if (!email.trim() || !password) {
      setErrorMessage('The email or password is not correct.')
      return
    }

    setIsLoading(true)
    try {
      const loggedUser = await login({ email: email.trim(), password })
      const destination = safeRedirect || getRoleLanding(loggedUser.role)
      navigate(destination, { replace: true })
    } catch {
      // Regardless of failure reason, always show one generic error
      setErrorMessage('The email or password is not correct.')
      // Keep email, focus input
      if (passwordInputRef.current) {
        passwordInputRef.current.focus()
      } else if (emailInputRef.current) {
        emailInputRef.current.focus()
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-sh-app-bg flex flex-col justify-center items-center px-4 py-12">
      {/* Brand Header */}
      <div className="mb-6 flex flex-col items-center gap-2 text-center">
        <div className="w-10 h-10 rounded-sh-md bg-sh-primary flex items-center justify-center text-white text-base font-bold shadow-sh-button-primary">
          SH
        </div>
        <h1 className="typography-headline text-sh-ink font-semibold text-xl">StorageHub</h1>
        <p className="typography-meta text-sh-muted">Self-service storage management system</p>
      </div>

      <Card className="w-full max-w-[400px] p-6 shadow-sh-card border border-sh-border bg-sh-surface">
        <h2 className="typography-headline text-sh-ink font-semibold mb-4 text-center">Sign In</h2>

        {infoMessage && (
          <div
            role="status"
            className="mb-4 p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md text-[13px] text-sh-ink-secondary flex items-center gap-2"
          >
            <span>{infoMessage}</span>
          </div>
        )}

        {isExpired && (
          <div
            role="status"
            className="mb-4 p-3 bg-sh-warning-tint border border-sh-status-buffer-border rounded-sh-md text-[13px] text-sh-status-buffer flex items-center gap-2"
          >
            <span>Your session has expired. Please sign in again.</span>
          </div>
        )}

        {errorMessage && (
          <div
            role="alert"
            className="mb-4 p-3 bg-sh-error-tint border border-sh-error-border rounded-sh-md text-[13px] text-sh-error flex flex-col gap-1"
          >
            <span>{errorMessage}</span>
            <span className="text-sh-muted text-[11.5px]">
              Please check your credentials, or{' '}
              <button
                type="button"
                onClick={() => setForgotOpen(true)}
                className="text-sh-primary underline hover:opacity-80 font-medium"
              >
                reset your password
              </button>
              .
            </span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input
            ref={emailInputRef}
            label="Email address"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              if (errorMessage) setErrorMessage(null)
            }}
            required
            autoComplete="email"
            autoFocus
          />

          <div className="flex flex-col gap-1 text-left">
            <div className="flex items-center justify-between">
              <label
                htmlFor="login-password"
                className="typography-label text-sh-muted flex items-center gap-1 select-none"
              >
                Password
                <span className="text-sh-error" aria-hidden="true">
                  *
                </span>
              </label>
              <button
                type="button"
                onClick={() => setForgotOpen(true)}
                className="text-[11.5px] text-sh-primary hover:underline font-medium"
              >
                Forgot password?
              </button>
            </div>
            <Input
              ref={passwordInputRef}
              id="login-password"
              type="password"
              placeholder="••••••••"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value)
                if (errorMessage) setErrorMessage(null)
              }}
              required
              autoComplete="current-password"
            />
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="page"
              className="w-full"
              isLoading={isLoading}
            >
              Sign in
            </Button>
          </div>
        </form>

        <div className="mt-6 pt-4 border-t border-sh-divider text-center">
          <p className="typography-body text-sh-ink-secondary text-[13px]">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="text-sh-primary font-semibold hover:underline">
              Create account
            </Link>
          </p>
        </div>
      </Card>

      <ForgotPasswordModal open={forgotOpen} onOpenChange={setForgotOpen} />
    </div>
  )
}
