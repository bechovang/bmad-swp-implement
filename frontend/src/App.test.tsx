import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'

describe('App', () => {
  it('renders the platform and redirects unauthenticated visitor to login', () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'StorageHub' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Sign In' })).toBeInTheDocument()
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument()
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })
})
