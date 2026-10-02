import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { browseUnits } from '../../api/unit'
import type { BrowseFilter } from '../../types/unit'
import { UnitCard } from '../../components/unit/UnitCard'
import { UnitFilterBar } from '../../components/unit/UnitFilterBar'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Card } from '../../components/ui/Card'
import { Skeleton } from '../../components/ui/Skeleton'

const STORAGE_KEY = 'storagehub_browse_filter'

function loadSavedFilters(): BrowseFilter {
  if (typeof window === 'undefined') return { durationMonths: 1 }
  try {
    const saved = sessionStorage.getItem(STORAGE_KEY)
    if (saved) {
      return JSON.parse(saved)
    }
  } catch {
    // Ignore storage parse errors
  }
  return { durationMonths: 1 }
}

function saveFilters(filters: BrowseFilter) {
  if (typeof window === 'undefined') return
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(filters))
  } catch {
    // Ignore storage save errors
  }
}

export function BrowseUnitsPage() {
  const [filters, setFilters] = useState<BrowseFilter>(loadSavedFilters)

  useEffect(() => {
    saveFilters(filters)
  }, [filters])

  const {
    data,
    isLoading,
    isError,
    error,
    refetch,
  } = useQuery({
    queryKey: ['browse-units', filters],
    queryFn: () => browseUnits(filters),
  })

  const handleSearch = (newFilters: BrowseFilter) => {
    setFilters(newFilters)
  }

  const handleRemoveFilter = (key: keyof BrowseFilter) => {
    const updated = { ...filters }
    delete updated[key]
    setFilters(updated)
  }

  const handleClearFilters = () => {
    setFilters({ durationMonths: 1 })
  }

  const hasActiveFilters = Boolean(
    filters.type || filters.size || filters.startDate || (filters.durationMonths && filters.durationMonths > 1)
  )

  return (
    <div className="space-y-6 pb-12" data-testid="browse-units-page">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="typography-display text-sh-ink font-bold">Browse Units</h1>
          <p className="typography-meta text-sh-muted mt-1">
            Explore secure storage units with guaranteed real-time availability.
          </p>
        </div>

        {/* Live Availability Badge */}
        <div>
          <Badge
            status="available"
            showDot
            className="px-3 py-1 text-xs font-semibold"
            data-testid="live-available-badge"
          >
            {data ? `${data.totalAvailable} units available · live` : 'Checking availability...'}
          </Badge>
        </div>
      </div>

      {/* Filter Toolbar */}
      <UnitFilterBar
        initialFilters={filters}
        onSearch={handleSearch}
        isLoading={isLoading}
      />

      {/* Content State Handling */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6" data-testid="browse-loading">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card key={i} className="flex flex-col h-[380px] p-4 space-y-4">
              <Skeleton className="h-44 w-full rounded" />
              <div className="flex gap-2">
                <Skeleton className="h-5 w-16" />
                <Skeleton className="h-5 w-16" />
              </div>
              <Skeleton className="h-4 w-3/4" />
              <div className="pt-4 border-t border-sh-divider flex justify-between items-center">
                <Skeleton className="h-8 w-24" />
                <Skeleton className="h-8 w-20" />
              </div>
            </Card>
          ))}
        </div>
      ) : isError ? (
        <Card className="p-8 text-center bg-sh-surface border border-sh-border" data-testid="browse-error">
          <h3 className="typography-headline text-sh-ink font-semibold">Failed to load units</h3>
          <p className="typography-body text-sh-muted mt-1 mb-4">
            {error instanceof Error ? error.message : 'An error occurred while fetching available units.'}
          </p>
          <Button onClick={() => refetch()} variant="secondary" size="sm">
            Try Again
          </Button>
        </Card>
      ) : data?.items.length === 0 ? (
        /* Empty State with Filter Chips */
        <Card
          className="p-8 text-center flex flex-col items-center justify-center min-h-[300px] bg-sh-surface border border-dashed"
          data-testid="browse-empty-state"
        >
          <div className="w-12 h-12 rounded-sh-md bg-sh-surface-muted text-sh-muted flex items-center justify-center font-mono text-sm mb-3">
            0
          </div>
          <h3 className="typography-headline text-sh-ink font-semibold">
            0 of {data.totalUnits} units meet all criteria
          </h3>
          <p className="typography-body text-sh-muted max-w-[420px] mt-1 mb-4">
            No available storage units match your selected filter criteria. Try adjusting or clearing your filters.
          </p>

          {/* Active Filter Chips */}
          {hasActiveFilters && (
            <div className="flex flex-wrap items-center justify-center gap-2 mb-6">
              {filters.type && (
                <span
                  data-testid="filter-chip-type"
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-sh-surface-subtle border border-sh-border rounded font-mono text-xs text-sh-ink"
                >
                  Type: {filters.type}
                  <button
                    onClick={() => handleRemoveFilter('type')}
                    className="text-sh-muted hover:text-sh-ink font-bold"
                    aria-label="Remove type filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {filters.size && (
                <span
                  data-testid="filter-chip-size"
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-sh-surface-subtle border border-sh-border rounded font-mono text-xs text-sh-ink"
                >
                  Size: {filters.size}
                  <button
                    onClick={() => handleRemoveFilter('size')}
                    className="text-sh-muted hover:text-sh-ink font-bold"
                    aria-label="Remove size filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {filters.startDate && (
                <span
                  data-testid="filter-chip-date"
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-sh-surface-subtle border border-sh-border rounded font-mono text-xs text-sh-ink"
                >
                  Date: {filters.startDate}
                  <button
                    onClick={() => handleRemoveFilter('startDate')}
                    className="text-sh-muted hover:text-sh-ink font-bold"
                    aria-label="Remove date filter"
                  >
                    ×
                  </button>
                </span>
              )}
              {filters.durationMonths && filters.durationMonths > 1 && (
                <span
                  data-testid="filter-chip-duration"
                  className="inline-flex items-center gap-1.5 px-2.5 py-1 bg-sh-surface-subtle border border-sh-border rounded font-mono text-xs text-sh-ink"
                >
                  Duration: {filters.durationMonths} months
                  <button
                    onClick={() => handleRemoveFilter('durationMonths')}
                    className="text-sh-muted hover:text-sh-ink font-bold"
                    aria-label="Remove duration filter"
                  >
                    ×
                  </button>
                </span>
              )}
            </div>
          )}

          <Button onClick={handleClearFilters} variant="secondary" size="sm" data-testid="clear-filters-btn">
            Clear filters
          </Button>
        </Card>
      ) : (
        /* Unit Grid */
        <div
          className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
          data-testid="browse-units-grid"
        >
          {data?.items.map((unit) => (
            <UnitCard key={unit.id} unit={unit} />
          ))}
        </div>
      )}
    </div>
  )
}
