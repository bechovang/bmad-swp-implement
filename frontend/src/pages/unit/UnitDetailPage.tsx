import { useState } from 'react'
import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { getUnitDetail } from '../../api/unit'
import { calculatePricing } from '../../api/pricing'
import { formatMoney, formatUnitCode } from '../../lib/format'
import { Card } from '../../components/ui/Card'
import { Badge } from '../../components/ui/Badge'
import { Button } from '../../components/ui/Button'
import { Skeleton } from '../../components/ui/Skeleton'

const DURATION_OPTIONS = [
  { value: 1, label: '1 Month' },
  { value: 3, label: '3 Months' },
  { value: 6, label: '6 Months' },
  { value: 12, label: '12 Months' },
]

function getTomorrowLocalDateString(): string {
  const d = new Date()
  d.setDate(d.getDate() + 1)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getTodayLocalDateString(): string {
  const d = new Date()
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function UnitDetailPage() {
  const { code = '' } = useParams<{ code: string }>()
  const navigate = useNavigate()
  const formattedCode = formatUnitCode(code)

  const [durationMonths, setDurationMonths] = useState<number>(3)
  const [startDate, setStartDate] = useState<string>(getTomorrowLocalDateString)
  const [imageError, setImageError] = useState(false)

  // Fetch unit details
  const {
    data: unit,
    isLoading: unitLoading,
    isError: unitError,
    error: unitErrorObj,
    refetch: refetchUnit,
  } = useQuery({
    queryKey: ['unit', formattedCode],
    queryFn: () => getUnitDetail(formattedCode),
    enabled: Boolean(formattedCode),
  })

  // Fetch live pricing calculation
  const {
    data: pricing,
    isLoading: pricingLoading,
    isError: isPricingError,
  } = useQuery({
    queryKey: ['pricing', formattedCode, durationMonths, startDate],
    queryFn: () =>
      calculatePricing({
        unitCode: formattedCode,
        durationMonths,
        startDate: startDate || undefined,
      }),
    enabled: Boolean(formattedCode && durationMonths >= 1),
  })

  if (unitLoading) {
    return (
      <div className="space-y-6 pb-12" data-testid="unit-detail-loading">
        <div className="flex items-center gap-2">
          <Skeleton className="h-5 w-28" />
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          <div className="lg:col-span-7 space-y-6">
            <Skeleton className="h-72 w-full rounded-sh-md" />
            <Skeleton className="h-10 w-48" />
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
              <Skeleton className="h-20" />
            </div>
          </div>
          <div className="lg:col-span-5 space-y-6">
            <Skeleton className="h-96 w-full rounded-sh-md" />
          </div>
        </div>
      </div>
    )
  }

  if (unitError || !unit) {
    return (
      <div className="space-y-6" data-testid="unit-detail-error">
        <Link
          to="/units"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-sh-primary hover:underline"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Browse Units
        </Link>
        <Card className="p-8 text-center flex flex-col items-center justify-center min-h-[300px]">
          <div className="w-12 h-12 rounded-full bg-red-50 text-red-600 flex items-center justify-center mb-3">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <h3 className="typography-headline text-sh-ink font-semibold">Unit Not Found</h3>
          <p className="typography-body text-sh-muted max-w-[420px] mt-1 mb-6">
            {(unitErrorObj as any)?.response?.data?.message || `Unit "${formattedCode}" could not be found or is unavailable.`}
          </p>
          <div className="flex gap-3">
            <Button variant="secondary" onClick={() => refetchUnit()}>
              Retry
            </Button>
            <Button variant="primary" onClick={() => navigate('/units')}>
              Explore Available Units
            </Button>
          </div>
        </Card>
      </div>
    )
  }

  const isBookable = unit.status === 'AVAILABLE' || unit.status === 'PREPARING'

  const handleReserve = () => {
    // Navigate or trigger booking summary / reservation flow with parameters
    navigate(`/booking/summary?unit=${encodeURIComponent(unit.code)}&duration=${durationMonths}&startDate=${startDate}`)
  }

  return (
    <div className="space-y-6 pb-12" data-testid="unit-detail-page">
      {/* Breadcrumb back button */}
      <div>
        <Link
          to="/units"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-sh-muted hover:text-sh-ink transition-colors uppercase tracking-wider"
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" />
          </svg>
          Back to Browse Units
        </Link>
      </div>

      {/* Main 2-Column Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        {/* Left Column: Media, Specs, Features (7/12) */}
        <div className="lg:col-span-7 space-y-6">
          {/* Unit Photo / Visual Card */}
          <Card className="overflow-hidden border-sh-border p-0 bg-sh-surface">
            <div className="relative h-64 sm:h-80 w-full bg-slate-100 flex items-center justify-center overflow-hidden">
              {!imageError ? (
                <img
                  src={unit.imageUrl}
                  alt={`Unit ${unit.code}`}
                  className="w-full h-full object-cover"
                  onError={() => setImageError(true)}
                  data-testid="unit-image"
                />
              ) : (
                <div
                  className="w-full h-full flex flex-col items-center justify-center bg-gradient-to-br from-slate-100 to-slate-200 text-sh-muted p-6"
                  data-testid="unit-image-fallback"
                >
                  <svg className="w-16 h-16 mb-2 text-slate-400 stroke-current" fill="none" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                  </svg>
                  <span className="font-mono font-semibold text-base text-sh-ink">Unit {unit.code}</span>
                  <span className="text-xs text-sh-muted">Type {unit.typeName} Storage Unit</span>
                </div>
              )}
              {/* Overlay Unit Code Tag */}
              <div className="absolute top-4 left-4 flex items-center gap-2">
                <span className="bg-slate-900/80 backdrop-blur text-white font-mono font-bold text-sm px-3 py-1 rounded-sh-sm shadow-sm">
                  {unit.code}
                </span>
                <Badge status={unit.status}>{unit.status}</Badge>
              </div>
            </div>
          </Card>

          {/* Title & Core Overview */}
          <div className="space-y-2">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h1
                  aria-label={`Unit Detail: ${unit.code}`}
                  className="typography-display text-sh-ink font-bold flex items-center gap-3"
                >
                  <span className="font-mono">Unit {unit.code}</span>
                  <span className="text-sh-muted font-normal text-lg">·</span>
                  <span className="font-normal text-sh-ink-secondary text-lg">Type {unit.typeName} Unit</span>
                </h1>
                <p className="typography-meta text-sh-muted mt-1 flex items-center gap-1.5">
                  <svg className="w-3.5 h-3.5 text-sh-muted" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  {unit.facilityName} · Zone {unit.zoneCode} ({unit.facilityAddress})
                </p>
              </div>
            </div>
            {unit.typeDescription && (
              <p className="typography-body text-sh-ink-secondary pt-1">{unit.typeDescription}</p>
            )}
          </div>

          {/* Specifications Grid */}
          <div>
            <h2 className="typography-label text-sh-muted uppercase tracking-wider font-semibold mb-3">
              Unit Specifications
            </h2>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <Card className="p-3 bg-sh-surface-subtle border-sh-border">
                <span className="text-xs text-sh-muted block">Usable Area</span>
                <span className="typography-bodyStrong text-sh-ink font-semibold font-mono text-base mt-0.5 block" data-testid="spec-size">
                  {unit.sizeM2.toFixed(1)} m²
                </span>
              </Card>
              <Card className="p-3 bg-sh-surface-subtle border-sh-border">
                <span className="text-xs text-sh-muted block">Floor Level</span>
                <span className="typography-bodyStrong text-sh-ink font-semibold text-base mt-0.5 block" data-testid="spec-floor">
                  Floor {unit.floor}
                </span>
              </Card>
              <Card className="p-3 bg-sh-surface-subtle border-sh-border">
                <span className="text-xs text-sh-muted block">Access Mechanism</span>
                <span className="typography-bodyStrong text-sh-ink font-semibold text-base mt-0.5 block" data-testid="spec-access">
                  {unit.accessType}
                </span>
              </Card>
              <Card className="p-3 bg-sh-surface-subtle border-sh-border">
                <span className="text-xs text-sh-muted block">Zone & Depot</span>
                <span className="typography-bodyStrong text-sh-ink font-semibold text-base mt-0.5 block" data-testid="spec-zone">
                  Zone {unit.zoneCode}
                </span>
              </Card>
            </div>
          </div>

          {/* Security & Facility Provisions */}
          <Card className="p-5 border-sh-border bg-sh-surface space-y-4">
            <h2 className="typography-headline text-sh-ink font-semibold flex items-center gap-2">
              <svg className="w-5 h-5 text-sh-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
              </svg>
              Security & Storage Environment
            </h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3" data-testid="security-features-list">
              {unit.securityFeatures?.map((feature, idx) => (
                <div key={idx} className="flex items-center gap-2.5 text-xs text-sh-ink font-medium">
                  <span className="w-5 h-5 rounded-full bg-emerald-50 text-emerald-600 flex items-center justify-center shrink-0">
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                    </svg>
                  </span>
                  <span>{feature}</span>
                </div>
              ))}
            </div>
          </Card>
        </div>

        {/* Right Column: Pricing Breakdown & Reservation Card (5/12) */}
        <div className="lg:col-span-5 sticky top-20 space-y-4">
          <Card
            className="p-6 border-sh-border bg-sh-surface shadow-sm space-y-6 relative overflow-hidden"
            data-testid="pricing-card"
          >
            {/* 3px left status bar */}
            <div className="absolute top-0 left-0 bottom-0 w-[3px] bg-sh-primary" />

            <div>
              <div className="flex items-center justify-between">
                <h2 className="typography-headline text-sh-ink font-bold">Transparent Pricing</h2>
                <span className="text-xs text-emerald-600 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded font-semibold" data-testid="policy-version-badge">
                  Active Policy {pricing?.policyVersion || 'v3'}
                </span>
              </div>
              <p className="text-xs text-sh-muted mt-1">
                Guaranteed transparent rates. No hidden facility or maintenance fees.
              </p>
            </div>

            {/* Inline Pricing Error Banner */}
            {isPricingError && (
              <div
                className="p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-sh-sm flex items-center gap-2"
                data-testid="pricing-error-banner"
              >
                <svg className="w-4 h-4 shrink-0 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <span>Unable to calculate pricing for selected duration. Please retry.</span>
              </div>
            )}

            {/* Start Date Selector */}
            <div className="space-y-1.5">
              <label htmlFor="start-date-input" className="typography-label text-sh-ink-secondary block font-semibold">
                Rental Start Date
              </label>
              <input
                id="start-date-input"
                type="date"
                value={startDate}
                min={getTodayLocalDateString()}
                onChange={(e) => setStartDate(e.target.value)}
                className="w-full px-3 py-2 border border-sh-border rounded-sh-sm text-sm font-mono text-sh-ink bg-white focus:outline-none focus:ring-2 focus:ring-sh-primary/20 focus:border-sh-primary"
                data-testid="start-date-picker"
              />
            </div>

            {/* Duration Selector Pills */}
            <div className="space-y-2">
              <label className="typography-label text-sh-ink-secondary block font-semibold">
                Select Rental Duration
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2" data-testid="duration-selector">
                {DURATION_OPTIONS.map((opt) => {
                  const isSelected = durationMonths === opt.value
                  return (
                    <button
                      key={opt.value}
                      type="button"
                      onClick={() => setDurationMonths(opt.value)}
                      data-testid={`duration-option-${opt.value}`}
                      className={`py-2 px-3 text-xs font-semibold rounded-sh-sm border transition-all text-center ${
                        isSelected
                          ? 'bg-sh-primary text-white border-sh-primary shadow-sm'
                          : 'bg-sh-surface-subtle text-sh-ink border-sh-border hover:bg-sh-surface-muted hover:border-slate-300'
                      }`}
                    >
                      {opt.label}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Live Pricing Breakdown Table */}
            <div className="border-t border-b border-sh-divider py-4 space-y-3" data-testid="pricing-breakdown">
              <div className="flex justify-between text-xs text-sh-ink-secondary">
                <span>Monthly Rate</span>
                <span className="font-mono tabular-nums font-semibold text-sh-ink" data-testid="monthly-rate-display">
                  {formatMoney(pricing?.monthlyRate ?? unit.monthlyRate)} / mo
                </span>
              </div>
              <div className="flex justify-between text-xs text-sh-ink-secondary">
                <span>Duration</span>
                <span className="font-mono tabular-nums text-sh-ink font-medium" data-testid="duration-display">
                  {durationMonths} {durationMonths === 1 ? 'month' : 'months'}
                </span>
              </div>
              <div className="flex justify-between text-xs text-sh-ink-secondary">
                <span>Base Rent</span>
                <span className="font-mono tabular-nums font-semibold text-sh-ink" data-testid="base-rent-display">
                  {pricingLoading ? '...' : formatMoney(pricing?.baseRent ?? (unit.monthlyRate * durationMonths))}
                </span>
              </div>

              {/* Surcharges lines if any */}
              {pricing?.surcharges && pricing.surcharges.length > 0 && (
                <div className="space-y-1.5 pt-1 border-t border-dashed border-sh-divider">
                  {pricing.surcharges.map((surcharge, sIdx) => (
                    <div key={sIdx} className="flex justify-between text-xs text-sh-muted">
                      <span>{surcharge.name}</span>
                      <span className="font-mono tabular-nums">{formatMoney(surcharge.amount)}</span>
                    </div>
                  ))}
                </div>
              )}

              <div className="flex justify-between text-xs text-sh-ink-secondary pt-2 border-t border-sh-divider">
                <span className="flex items-center gap-1.5">
                  Deposit ({pricing?.depositRate ?? unit.depositRate ?? 10}% refundable)
                  <span className="inline-block w-2 h-2 rounded-full bg-emerald-500" title="100% refundable upon move-out" />
                </span>
                <span className="font-mono tabular-nums font-semibold text-emerald-700" data-testid="deposit-amount-display">
                  {pricingLoading ? '...' : formatMoney(pricing?.depositAmount ?? Math.round((unit.monthlyRate * durationMonths * 0.1)))}
                </span>
              </div>

              <div className="flex justify-between text-sm font-bold text-sh-ink pt-2 border-t border-sh-border">
                <span>Total Contract Rent</span>
                <span className="font-mono tabular-nums" data-testid="total-rent-display">
                  {pricingLoading ? '...' : formatMoney(pricing?.totalRent ?? (unit.monthlyRate * durationMonths))}
                </span>
              </div>
            </div>

            {/* Deposit Due Now Highlight Card */}
            <div className="p-4 bg-indigo-50/70 border border-indigo-100 rounded-sh-sm flex items-center justify-between" data-testid="deposit-due-now-card">
              <div>
                <span className="text-xs font-semibold text-indigo-900 block">Refundable Deposit Due Now</span>
                <span className="text-[11px] text-indigo-700 block">Holds this unit exclusively for you</span>
              </div>
              <div className="text-right">
                <span className="font-mono font-bold text-lg text-indigo-950 block tabular-nums" data-testid="deposit-due-now-amount">
                  {pricingLoading ? '...' : formatMoney(pricing?.totalDueNow ?? pricing?.depositAmount ?? Math.round((unit.monthlyRate * durationMonths * 0.1)))}
                </span>
                <span className="text-[10px] text-emerald-700 font-semibold bg-emerald-100/80 px-1.5 py-0.5 rounded">
                  100% Refundable
                </span>
              </div>
            </div>

            {/* Reserve CTA */}
            <div className="space-y-2">
              <Button
                variant="primary"
                size="page"
                className="w-full font-bold shadow-sm"
                disabled={!isBookable || pricingLoading}
                onClick={handleReserve}
                data-testid="reserve-unit-cta"
              >
                {!isBookable
                  ? 'Unit Currently Unavailable'
                  : `Reserve Unit (${formatMoney(pricing?.totalDueNow ?? pricing?.depositAmount)})`}
              </Button>
              <p className="text-[11px] text-center text-sh-muted">
                Initial rental contract auto-drafted upon deposit confirmation.
              </p>
            </div>
          </Card>
        </div>
      </div>
    </div>
  )
}
