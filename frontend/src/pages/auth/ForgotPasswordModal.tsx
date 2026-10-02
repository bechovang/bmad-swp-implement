import React, { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { Modal } from '../../components/ui/Modal'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { forgotPasswordApi } from '../../api/auth'

export interface ForgotPasswordModalProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function ForgotPasswordModal({ open, onOpenChange }: ForgotPasswordModalProps) {
  const { user, isAuthenticated, getRoleLanding } = useAuth()
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const closeTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) clearTimeout(closeTimeoutRef.current)
    }
  }, [])

  useEffect(() => {
    if (open && isAuthenticated && user) {
      onOpenChange(false)
      navigate(getRoleLanding(user.role), { replace: true })
    }
  }, [open, isAuthenticated, user, getRoleLanding, navigate, onOpenChange])

  const handleClose = () => {
    onOpenChange(false)
    if (closeTimeoutRef.current) clearTimeout(closeTimeoutRef.current)
    closeTimeoutRef.current = setTimeout(() => {
      setEmail('')
      setMessage(null)
      setError(null)
      setIsLoading(false)
    }, 200)
  }

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
      // Per spec: Always answer generic confirmation even on failure or non-existent email
      setMessage('If an account exists with that email, a password reset link has been sent.')
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Modal
      open={open}
      onOpenChange={(isOpen) => {
        if (!isOpen) handleClose()
        else onOpenChange(true)
      }}
      title="Reset Password"
      description="Enter your email address and we will send you a link to reset your password."
    >
      {message ? (
        <div className="flex flex-col gap-4 py-2">
          <div
            role="status"
            className="p-3 bg-sh-surface-subtle border border-sh-border rounded-sh-md text-[13px] text-sh-ink-secondary"
          >
            {message}
          </div>
          <div className="flex justify-end pt-2">
            <Button onClick={handleClose} variant="primary">
              Close
            </Button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 py-2">
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

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant="secondary" onClick={handleClose}>
              Cancel
            </Button>
            <Button type="submit" variant="primary" isLoading={isLoading}>
              Send reset link
            </Button>
          </div>
        </form>
      )}
    </Modal>
  )
}
