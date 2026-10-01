import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import App from './App.tsx'

describe('App (scaffold smoke)', () => {
  it('renders the scaffold heading', () => {
    render(<App />)
    expect(screen.getByRole('heading', { level: 1, name: 'StorageHub' })).toBeInTheDocument()
  })

  it('lists the five role landings', () => {
    render(<App />)
    for (const role of ['Customer', 'Staff', 'Facility Manager', 'Business Ops', 'System Administrator']) {
      expect(screen.getByText(role, { exact: false })).toBeInTheDocument()
    }
  })
})
