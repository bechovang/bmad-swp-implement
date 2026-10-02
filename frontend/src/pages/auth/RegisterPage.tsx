import React, { useState, useEffect, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import axios from 'axios'
import { useAuth } from '../../hooks/useAuth'
import { AutoLoginError } from '../../context/auth-context-base'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import type { ApiError } from '../../types/auth'

export function RegisterPage() {
  const { user, isAuthenticated, register, getRoleLanding } = useAuth()
  const navigate = useNavigate()

  const [fullName, setFullName] = useState('')
  const [phone, setPhone] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [agreeToTerms, setAgreeToTerms] = useState(false)

  const [fullNameError, setFullNameError] = useState<string | null>(null)
  const [phoneError, setPhoneError] = useState<string | null>(null)
  const [emailError, setEmailError] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [confirmPasswordError, setConfirmPasswordError] = useState<string | null>(null)
  const [termsError, setTermsError] = useState<string | null>(null)
  const [generalError, setGeneralError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

  const emailRef = useRef<HTMLInputElement>(null)
  const confirmPasswordRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    if (isAuthenticated && user) {
      navigate(getRoleLanding(user.role), { replace: true })
    }
  }, [isAuthenticated, user, getRoleLanding, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setFullNameError(null)
    setPhoneError(null)
    setEmailError(null)
    setPasswordError(null)
    setConfirmPasswordError(null)
    setTermsError(null)
    setGeneralError(null)

    // Client-side validations
    if (password !== confirmPassword) {
      setConfirmPasswordError('Passwords do not match.')
      if (confirmPasswordRef.current) {
        confirmPasswordRef.current.focus()
      }
      return
    }

    if (!agreeToTerms) {
      setTermsError('You must agree to the terms and privacy policy.')
      return
    }

    setIsLoading(true)
    try {
      // Customer-only: payload strictly without role field
      await register({
        fullName: fullName.trim(),
        phone: phone.trim(),
        email: email.trim(),
        password,
        confirmPassword,
        agreeToTerms,
      })
      navigate('/units', { replace: true })
    } catch (err: unknown) {
      if (err instanceof AutoLoginError) {
        navigate(`/login?message=${encodeURIComponent('Account created successfully. Please sign in.')}`, {
          replace: true,
        })
        return
      }

      if (axios.isAxiosError(err) && err.response?.data) {
        const errorData = err.response.data as ApiError
        if (errorData.fieldErrors && errorData.fieldErrors.length > 0) {
          const nameField = errorData.fieldErrors.find((fe) => fe.field === 'fullName')
          if (nameField) setFullNameError(nameField.message)

          const phoneField = errorData.fieldErrors.find((fe) => fe.field === 'phone')
          if (phoneField) setPhoneError(phoneField.message)

          const emailField = errorData.fieldErrors.find((fe) => fe.field === 'email')
          if (emailField) {
            setEmailError(emailField.message || 'Email is already registered.')
            if (emailRef.current) {
              emailRef.current.focus()
            }
          }

          const pwField = errorData.fieldErrors.find((fe) => fe.field === 'password')
          if (pwField) setPasswordError(pwField.message)

          const confirmField = errorData.fieldErrors.find((fe) => fe.field === 'confirmPassword')
          if (confirmField) setConfirmPasswordError(confirmField.message)

          const termsField = errorData.fieldErrors.find((fe) => fe.field === 'agreeToTerms')
          if (termsField) setTermsError(termsField.message)

          return
        }
        setGeneralError(errorData.message || 'Registration failed. Please try again.')
      } else {
        setGeneralError('Registration failed. Please check your network and try again.')
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

      <Card className="w-full max-w-[460px] p-6 shadow-sh-card border border-sh-border bg-sh-surface">
        <h2 className="typography-headline text-sh-ink font-semibold mb-1 text-center">
          Create Customer Account
        </h2>
        <p className="typography-meta text-sh-muted text-center mb-6">
          Sign up to search, reserve, and manage your self-storage units
        </p>

        {generalError && (
          <div
            role="alert"
            className="mb-4 p-3 bg-sh-error-tint border border-sh-error-border rounded-sh-md text-[13px] text-sh-error"
          >
            {generalError}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input
            label="Full name"
            type="text"
            placeholder="Nguyen Van A"
            value={fullName}
            onChange={(e) => {
              setFullName(e.target.value)
              if (fullNameError) setFullNameError(null)
            }}
            error={fullNameError || undefined}
            required
            autoFocus
          />

          <Input
            label="Phone number"
            type="tel"
            placeholder="0901234567"
            value={phone}
            onChange={(e) => {
              setPhone(e.target.value)
              if (phoneError) setPhoneError(null)
            }}
            error={phoneError || undefined}
            required
          />

          <Input
            ref={emailRef}
            label="Email address"
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => {
              setEmail(e.target.value)
              if (emailError) setEmailError(null)
            }}
            error={emailError || undefined}
            required
          />

          <Input
            label="Password"
            type="password"
            placeholder="••••••••"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value)
              if (passwordError) setPasswordError(null)
              if (confirmPasswordError) setConfirmPasswordError(null)
            }}
            error={passwordError || undefined}
            required
          />

          <Input
            ref={confirmPasswordRef}
            label="Confirm password"
            type="password"
            placeholder="••••••••"
            value={confirmPassword}
            onChange={(e) => {
              setConfirmPassword(e.target.value)
              if (confirmPasswordError) setConfirmPasswordError(null)
            }}
            error={confirmPasswordError || undefined}
            required
          />

          {/* Terms Agreement Checkbox */}
          <div className="flex flex-col gap-1 text-left pt-1">
            <label htmlFor="agreeToTerms" className="flex items-start gap-2.5 cursor-pointer select-none">
              <input
                id="agreeToTerms"
                type="checkbox"
                checked={agreeToTerms}
                aria-invalid={Boolean(termsError)}
                aria-describedby={termsError ? 'agreeToTerms-error' : undefined}
                onChange={(e) => {
                  setAgreeToTerms(e.target.checked)
                  if (termsError) setTermsError(null)
                }}
                className="mt-0.5 h-4 w-4 rounded-sh-sm border-sh-border text-sh-primary focus:ring-sh-primary cursor-pointer"
              />
              <span className="typography-body text-[13px] text-sh-ink-secondary">
                I agree to the StorageHub terms of service and privacy policy
              </span>
            </label>
            {termsError && (
              <p id="agreeToTerms-error" role="alert" className="typography-meta text-sh-error ml-6">
                {termsError}
              </p>
            )}
          </div>

          <div className="pt-2">
            <Button
              type="submit"
              variant="primary"
              size="page"
              className="w-full"
              isLoading={isLoading}
            >
              Create account
            </Button>
          </div>
        </form>

        <div className="mt-6 pt-4 border-t border-sh-divider text-center">
          <p className="typography-body text-sh-ink-secondary text-[13px]">
            Already have an account?{' '}
            <Link to="/login" className="text-sh-primary font-semibold hover:underline">
              Sign in
            </Link>
          </p>
        </div>
      </Card>
    </div>
  )
}
