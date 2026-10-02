import { describe, expect, it } from 'vitest'
import { formatMoney, formatUnitCode } from '../lib/format'

describe('formatMoney (VND Currency Formatting)', () => {
  it('formats standard positive amount correctly with dot separator and dong symbol', () => {
    expect(formatMoney(1150000)).toBe('1.150.000 ₫')
  })

  it('formats zero correctly', () => {
    expect(formatMoney(0)).toBe('0 ₫')
  })

  it('formats negative amount correctly', () => {
    expect(formatMoney(-50000)).toBe('-50.000 ₫')
  })

  it('formats small negative fractions as 0 ₫ instead of -0 ₫', () => {
    expect(formatMoney(-0.3)).toBe('0 ₫')
    expect(formatMoney(-0.49)).toBe('0 ₫')
    expect(formatMoney(-0.6)).toBe('-1 ₫')
  })

  it('handles Infinity and -Infinity gracefully by returning 0 ₫', () => {
    expect(formatMoney(Infinity)).toBe('0 ₫')
    expect(formatMoney(-Infinity)).toBe('0 ₫')
  })

  it('handles null gracefully without throwing', () => {
    expect(formatMoney(null as unknown as number)).toBe('0 ₫')
  })

  it('handles undefined gracefully without throwing', () => {
    expect(formatMoney(undefined as unknown as number)).toBe('0 ₫')
  })

  it('handles NaN gracefully without throwing', () => {
    expect(formatMoney(Number.NaN)).toBe('0 ₫')
  })

  it('formats large values with multiple thousand separators', () => {
    expect(formatMoney(1000000000)).toBe('1.000.000.000 ₫')
  })

  it('formats amounts under 1000 without separators', () => {
    expect(formatMoney(500)).toBe('500 ₫')
  })

  it('rounds decimal values to nearest integer VND', () => {
    expect(formatMoney(1150000.4)).toBe('1.150.000 ₫')
    expect(formatMoney(1150000.6)).toBe('1.150.001 ₫')
  })
})

describe('formatUnitCode', () => {
  it('uppercases and trims unit code', () => {
    expect(formatUnitCode('s-3')).toBe('S-3')
    expect(formatUnitCode('  m-2  ')).toBe('M-2')
  })

  it('handles empty, null, or undefined values gracefully', () => {
    expect(formatUnitCode('')).toBe('')
    expect(formatUnitCode(null)).toBe('')
    expect(formatUnitCode(undefined)).toBe('')
  })

  it('handles non-string types safely by returning empty string', () => {
    expect(formatUnitCode(123 as unknown as string)).toBe('')
    expect(formatUnitCode({} as unknown as string)).toBe('')
    expect(formatUnitCode(true as unknown as string)).toBe('')
  })
})
