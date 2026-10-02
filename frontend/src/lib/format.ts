/**
 * StorageHub Currency and Unit Formatting Utilities
 * Standard: VND full precision with dot separator, followed by space and '₫' symbol.
 * Reference: UX-DR2, DESIGN.md
 */

/**
 * Formats a numeric VND amount to Vietnamese currency format.
 * Example: 1150000 -> "1.150.000 ₫"
 * Example: 0 -> "0 ₫"
 * Example: -50000 -> "-50.000 ₫"
 * Graceful fallback: null, undefined, NaN, Infinity -> "0 ₫"
 * Small negative fractions (-0.3) round to 0 -> "0 ₫"
 */
export function formatMoney(amount: number | null | undefined): string {
  if (amount == null || typeof amount !== 'number' || !Number.isFinite(amount)) {
    return '0 ₫'
  }

  const rounded = Math.round(amount)
  const isNegative = rounded < 0
  const abs = Math.abs(rounded)
  const formatted = abs.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.')

  return `${isNegative ? '-' : ''}${formatted} ₫`
}

/**
 * Formats a unit code to standard uppercase format.
 * Example: "s-3" -> "S-3"
 * Graceful fallback: non-string, null, undefined -> ""
 */
export function formatUnitCode(code: string | null | undefined): string {
  if (typeof code !== 'string') {
    return ''
  }
  return code.trim().toUpperCase()
}
