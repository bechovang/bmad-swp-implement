import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, expect, it, beforeEach, vi } from 'vitest'
import { createMemoryRouter, RouterProvider } from 'react-router-dom'
import { http, HttpResponse } from 'msw'
import { routesConfig } from '../router/routes'
import { AuthProvider } from '../context/AuthContext'
import { apiClient, AUTH_TOKEN_KEY, AUTH_USER_KEY } from '../api/client'
import { server } from '../mocks/server'

function renderWithRouter(initialEntries: string[] = ['/login']) {
  const router = createMemoryRouter(routesConfig, { initialEntries })
  const result = render(
    <AuthProvider>
      <RouterProvider router={router} />
    </AuthProvider>
  )
  return { ...result, router }
}

describe('Authentication & Auth Screens', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('redirects unauthenticated user accessing protected route to /login with redirect query param', async () => {
    const { router } = renderWithRouter(['/units'])

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/login')
      expect(router.state.location.search).toContain('redirect=%2Funits')
    })
    expect(screen.getByRole('heading', { level: 2, name: 'Sign In' })).toBeInTheDocument()
  })

  it('logs in successfully with valid credentials, stores token/user, and redirects to role landing', async () => {
    const { router } = renderWithRouter(['/login'])

    const emailInput = screen.getByLabelText(/email address/i)
    const passwordInput = screen.getByLabelText(/password/i)
    const submitBtn = screen.getByRole('button', { name: /sign in/i })

    fireEvent.change(emailInput, { target: { value: 'lan@storagehub.dev' } })
    fireEvent.change(passwordInput, { target: { value: 'Demo1234!' } })
    fireEvent.click(submitBtn)

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/units')
    })

    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toContain('mock-jwt-token')
    const storedUser = JSON.parse(localStorage.getItem(AUTH_USER_KEY) || '{}')
    expect(storedUser.email).toBe('lan@storagehub.dev')
    expect(storedUser.role).toBe('CUSTOMER')
  })

  it('preserves intended relative redirect destination after successful login', async () => {
    const { router } = renderWithRouter(['/login?redirect=%2Frentals'])

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'lan@storagehub.dev' },
    })
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'Demo1234!' },
    })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/rentals')
    })
  })

  it('sanitizes unsafe redirect param (e.g. external //evil.com or /register) and falls back to role landing', async () => {
    const { router } = renderWithRouter(['/login?redirect=//evil.com'])

    fireEvent.change(screen.getByLabelText(/email address/i), {
      target: { value: 'lan@storagehub.dev' },
    })
    fireEvent.change(screen.getByLabelText(/password/i), {
      target: { value: 'Demo1234!' },
    })
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/units')
    })
  })

  it('displays session expired notice when expired query param is present on /login', () => {
    renderWithRouter(['/login?expired=true'])
    expect(screen.getByText(/your session has expired\. please sign in again\./i)).toBeInTheDocument()
  })

  it('displays single generic inline error on 401 without revealing field details', async () => {
    renderWithRouter(['/login'])

    const emailInput = screen.getByLabelText(/email address/i)
    const passwordInput = screen.getByLabelText(/password/i)
    const submitBtn = screen.getByRole('button', { name: /sign in/i })

    fireEvent.change(emailInput, { target: { value: 'lan@storagehub.dev' } })
    fireEvent.change(passwordInput, { target: { value: 'WrongPassword!' } })
    fireEvent.click(submitBtn)

    const alert = await screen.findByRole('alert')
    expect(alert).toHaveTextContent('The email or password is not correct.')
    expect(alert).toHaveTextContent(/reset your password/i)
    // Verify email is kept in field
    expect(emailInput).toHaveValue('lan@storagehub.dev')
  })

  it('redirects already logged-in user away from /login to role landing', async () => {
    localStorage.setItem(AUTH_TOKEN_KEY, 'mock-jwt-existing')
    localStorage.setItem(
      AUTH_USER_KEY,
      JSON.stringify({
        id: 1,
        fullName: 'Lan Nguyen',
        email: 'lan@storagehub.dev',
        role: 'CUSTOMER',
      })
    )

    const { router } = renderWithRouter(['/login'])

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/units')
    })
  })

  describe('Customer Register', () => {
    it('contains no role selector or role input anywhere on the form', () => {
      renderWithRouter(['/register'])

      expect(screen.queryByLabelText(/role/i)).not.toBeInTheDocument()
      expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
      expect(screen.queryByRole('radio')).not.toBeInTheDocument()
      expect(screen.getByRole('heading', { name: /create customer account/i })).toBeInTheDocument()
    })

    it('blocks submit and shows inline error if passwords do not match', async () => {
      renderWithRouter(['/register'])

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Nguyen Van B' } })
      fireEvent.change(screen.getByLabelText(/phone number/i), { target: { value: '0912345678' } })
      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: 'test-new@storagehub.dev' },
      })
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'Secret123!' } })
      fireEvent.change(screen.getByLabelText(/confirm password/i), {
        target: { value: 'SecretMismatch!' },
      })
      fireEvent.click(screen.getByRole('checkbox'))

      fireEvent.click(screen.getByRole('button', { name: /create account/i }))

      const confirmError = await screen.findByText('Passwords do not match.')
      expect(confirmError).toBeInTheDocument()
      expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
    })

    it('blocks submit and shows inline error if terms are not accepted', async () => {
      renderWithRouter(['/register'])

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Nguyen Van B' } })
      fireEvent.change(screen.getByLabelText(/phone number/i), { target: { value: '0912345678' } })
      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: 'test-new@storagehub.dev' },
      })
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'Secret123!' } })
      fireEvent.change(screen.getByLabelText(/confirm password/i), {
        target: { value: 'Secret123!' },
      })
      // agreeToTerms is left unchecked

      fireEvent.click(screen.getByRole('button', { name: /create account/i }))

      const termsError = await screen.findByText(/you must agree to the terms/i)
      expect(termsError).toBeInTheDocument()
      expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
    })

    it('displays duplicate email inline error when API returns 400 with duplicate email', async () => {
      renderWithRouter(['/register'])

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'Lan Nguyen Duplicate' } })
      fireEvent.change(screen.getByLabelText(/phone number/i), { target: { value: '0912345678' } })
      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: 'lan@storagehub.dev' }, // existing demo email
      })
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'Secret123!' } })
      fireEvent.change(screen.getByLabelText(/confirm password/i), {
        target: { value: 'Secret123!' },
      })
      fireEvent.click(screen.getByRole('checkbox'))

      fireEvent.click(screen.getByRole('button', { name: /create account/i }))

      const emailErr = await screen.findByText('Email is already registered.')
      expect(emailErr).toBeInTheDocument()
    })

    it('successfully registers, automatically signs in, and redirects to /units', async () => {
      const { router } = renderWithRouter(['/register'])

      const uniqueEmail = `testuser_${Date.now()}@storagehub.dev`

      fireEvent.change(screen.getByLabelText(/full name/i), { target: { value: 'New Customer' } })
      fireEvent.change(screen.getByLabelText(/phone number/i), { target: { value: '0987654321' } })
      fireEvent.change(screen.getByLabelText(/email address/i), {
        target: { value: uniqueEmail },
      })
      fireEvent.change(screen.getByLabelText(/^password/i), { target: { value: 'Demo1234!' } })
      fireEvent.change(screen.getByLabelText(/confirm password/i), {
        target: { value: 'Demo1234!' },
      })
      fireEvent.click(screen.getByRole('checkbox'))

      fireEvent.click(screen.getByRole('button', { name: /create account/i }))

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/units')
      })

      expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeTruthy()
      const stored = JSON.parse(localStorage.getItem(AUTH_USER_KEY) || '{}')
      expect(stored.email).toBe(uniqueEmail)
      expect(stored.role).toBe('CUSTOMER')
    })
  })

  describe('Forgot Password Modal & Page', () => {
    it('opens modal from login page, submits email, and shows generic message without leaking account state', async () => {
      renderWithRouter(['/login'])

      const forgotBtn = screen.getByRole('button', { name: /forgot password\?/i })
      fireEvent.click(forgotBtn)

      expect(screen.getByRole('heading', { level: 2, name: 'Reset Password' })).toBeInTheDocument()

      const modalEmailInput = screen.getAllByLabelText(/email address/i)[1] // dialog's input
      fireEvent.change(modalEmailInput, { target: { value: 'nonexistent@nowhere.dev' } })

      const sendBtn = screen.getByRole('button', { name: /send reset link/i })
      fireEvent.click(sendBtn)

      const statusMsg = await screen.findByRole('status')
      expect(statusMsg).toHaveTextContent(
        'If an account exists with that email, a password reset link has been sent.'
      )

      // Close modal
      const closeButtons = screen.getAllByRole('button', { name: /close/i })
      fireEvent.click(closeButtons[0])

      await waitFor(() => {
        expect(screen.queryByRole('status')).not.toBeInTheDocument()
      })
    })

    it('redirects authenticated user away from /forgot-password to role landing', async () => {
      localStorage.setItem(AUTH_TOKEN_KEY, 'mock-jwt-existing')
      localStorage.setItem(
        AUTH_USER_KEY,
        JSON.stringify({
          id: 1,
          fullName: 'Lan Nguyen',
          email: 'lan@storagehub.dev',
          role: 'CUSTOMER',
        })
      )

      const { router } = renderWithRouter(['/forgot-password'])

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/units')
      })
    })
  })

  describe('apiClient Interceptors', () => {
    it('attaches Authorization header when token is present and omits when null', async () => {
      let capturedAuth: string | null = null
      server.use(
        http.get('/api/v1/test-auth-header', ({ request }) => {
          capturedAuth = request.headers.get('Authorization')
          return HttpResponse.json({ ok: true })
        })
      )

      // Null token
      localStorage.removeItem(AUTH_TOKEN_KEY)
      await apiClient.get('/test-auth-header')
      expect(capturedAuth).toBeNull()

      // Present token
      localStorage.setItem(AUTH_TOKEN_KEY, 'sample-jwt-123')
      await apiClient.get('/test-auth-header')
      expect(capturedAuth).toBe('Bearer sample-jwt-123')
    })

    it('clears session and dispatches unauthorized event on 401 from protected endpoint', async () => {
      server.use(
        http.get('/api/v1/protected-test-endpoint', () => {
          return HttpResponse.json({ code: 'UNAUTHENTICATED', message: 'Token expired' }, { status: 401 })
        })
      )

      localStorage.setItem(AUTH_TOKEN_KEY, 'valid-token')
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify({ id: 1, role: 'CUSTOMER' }))

      const unauthorizedSpy = vi.fn()
      window.addEventListener('storagehub:auth-unauthorized', unauthorizedSpy)

      await expect(apiClient.get('/protected-test-endpoint')).rejects.toThrow()

      expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull()
      expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull()
      expect(unauthorizedSpy).toHaveBeenCalled()

      window.removeEventListener('storagehub:auth-unauthorized', unauthorizedSpy)
    })
  })
})
