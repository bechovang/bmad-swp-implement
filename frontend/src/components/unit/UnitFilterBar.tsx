import { useState, useEffect } from 'react'
import type { BrowseFilter } from '../../types/unit'
import { Card } from '../ui/Card'
import { Button } from '../ui/Button'

export interface UnitFilterBarProps {
  initialFilters: BrowseFilter
  onSearch: (filters: BrowseFilter) => void
  isLoading?: boolean
}

const TYPE_OPTIONS = [
  { value: 'all', label: 'All Types' },
  { value: 'Locker', label: 'Locker (0.5 m²)' },
  { value: 'S', label: 'Type S (~5 m²)' },
  { value: 'M', label: 'Type M (~8 m²)' },
  { value: 'L', label: 'Type L (~12 m²)' },
]

const SIZE_OPTIONS = [
  { value: 'all', label: 'All Sizes' },
  { value: 'Small', label: 'Small (≤ 5 m²)' },
  { value: 'Medium', label: 'Medium (5 - 10 m²)' },
  { value: 'Large', label: 'Large (> 10 m²)' },
]

const DURATION_OPTIONS = [
  { value: 1, label: '1 Month' },
  { value: 3, label: '3 Months' },
  { value: 6, label: '6 Months' },
  { value: 12, label: '12 Months' },
]

export function UnitFilterBar({ initialFilters, onSearch, isLoading = false }: UnitFilterBarProps) {
  const [type, setType] = useState<string>(initialFilters.type || 'all')
  const [size, setSize] = useState<string>(initialFilters.size || 'all')
  const [startDate, setStartDate] = useState<string>(initialFilters.startDate || '')
  const [durationMonths, setDurationMonths] = useState<number>(initialFilters.durationMonths || 1)

  useEffect(() => {
    setType(initialFilters.type || 'all')
    setSize(initialFilters.size || 'all')
    setStartDate(initialFilters.startDate || '')
    setDurationMonths(initialFilters.durationMonths || 1)
  }, [initialFilters])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    onSearch({
      type: type === 'all' ? undefined : type,
      size: size === 'all' ? undefined : size,
      startDate: startDate || undefined,
      durationMonths,
    })
  }

  return (
    <Card className="p-4 bg-sh-surface border border-sh-border shadow-sm">
      <form onSubmit={handleSubmit} className="flex flex-col lg:flex-row items-end gap-3.5">
        {/* Type selector */}
        <div className="flex-1 w-full space-y-1">
          <label htmlFor="unit-type-select" className="block text-xs font-semibold text-sh-ink-secondary">
            Unit Type
          </label>
          <select
            id="unit-type-select"
            value={type}
            onChange={(e) => setType(e.target.value)}
            className="w-full h-9 px-3 text-xs bg-sh-surface-subtle border border-sh-border rounded-sh-md focus:outline-none focus:ring-1 focus:ring-sh-primary text-sh-ink"
            data-testid="filter-type-select"
          >
            {TYPE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Size selector */}
        <div className="flex-1 w-full space-y-1">
          <label htmlFor="unit-size-select" className="block text-xs font-semibold text-sh-ink-secondary">
            Unit Size
          </label>
          <select
            id="unit-size-select"
            value={size}
            onChange={(e) => setSize(e.target.value)}
            className="w-full h-9 px-3 text-xs bg-sh-surface-subtle border border-sh-border rounded-sh-md focus:outline-none focus:ring-1 focus:ring-sh-primary text-sh-ink"
            data-testid="filter-size-select"
          >
            {SIZE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Start Date */}
        <div className="flex-1 w-full space-y-1">
          <label htmlFor="unit-start-date" className="block text-xs font-semibold text-sh-ink-secondary">
            Move-in Date
          </label>
          <input
            id="unit-start-date"
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="w-full h-9 px-3 text-xs bg-sh-surface-subtle border border-sh-border rounded-sh-md focus:outline-none focus:ring-1 focus:ring-sh-primary text-sh-ink"
            data-testid="filter-start-date"
          />
        </div>

        {/* Duration selector */}
        <div className="flex-1 w-full space-y-1">
          <label htmlFor="unit-duration-select" className="block text-xs font-semibold text-sh-ink-secondary">
            Duration
          </label>
          <select
            id="unit-duration-select"
            value={durationMonths}
            onChange={(e) => setDurationMonths(Number(e.target.value))}
            className="w-full h-9 px-3 text-xs bg-sh-surface-subtle border border-sh-border rounded-sh-md focus:outline-none focus:ring-1 focus:ring-sh-primary text-sh-ink"
            data-testid="filter-duration-select"
          >
            {DURATION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>

        {/* Search Submit Button */}
        <div className="w-full lg:w-auto pt-1 lg:pt-0">
          <Button
            type="submit"
            isLoading={isLoading}
            className="w-full lg:w-auto h-9 px-5 text-xs font-bold"
            data-testid="filter-search-btn"
          >
            Search
          </Button>
        </div>
      </form>
    </Card>
  )
}
