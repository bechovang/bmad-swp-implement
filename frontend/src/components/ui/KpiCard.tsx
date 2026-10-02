import React from 'react'
import { cn } from '../../lib/utils'
import { Card } from './Card'

export interface KpiCardProps extends React.HTMLAttributes<HTMLDivElement> {
  label: string
  value: string | number
  delta?: string
  deltaType?: 'positive' | 'negative' | 'neutral'
  period?: string
}

export const KpiCard = React.forwardRef<HTMLDivElement, KpiCardProps>(
  ({ className, label, value, delta, deltaType, period, ...props }, ref) => {
    let resolvedDeltaType = deltaType
    if (!resolvedDeltaType && delta) {
      if (delta.startsWith('+')) {
        resolvedDeltaType = 'positive'
      } else if (delta.startsWith('-') || delta.startsWith('−')) {
        resolvedDeltaType = 'negative'
      } else {
        resolvedDeltaType = 'neutral'
      }
    }

    const deltaColorClasses = {
      positive: 'bg-sh-status-available-tint border-sh-status-available-border text-sh-status-available',
      negative: 'bg-sh-error-tint border-sh-error-border text-sh-error',
      neutral: 'bg-sh-surface-muted border-sh-border text-sh-ink-secondary',
    }

    return (
      <Card ref={ref} className={cn('p-4 flex flex-col justify-between gap-3', className)} {...props}>
        <div className="flex items-center justify-between gap-2">
          <span className="typography-label text-sh-muted select-none">{label}</span>
          {delta && (
            <span
              className={cn(
                'inline-flex items-center rounded-[6px] border px-1.5 py-0.5 text-[10.5px] font-semibold tabular-nums leading-none',
                deltaColorClasses[resolvedDeltaType || 'neutral']
              )}
            >
              {delta}
            </span>
          )}
        </div>

        <div className="flex flex-col gap-1">
          <div className="typography-kpi-value text-sh-ink tabular-nums">{value}</div>
          {period && <span className="typography-meta text-sh-faint">{period}</span>}
        </div>
      </Card>
    )
  }
)

KpiCard.displayName = 'KpiCard'
