import React from 'react'
import { cn } from '../../lib/utils'

export interface EmptyStateProps extends React.HTMLAttributes<HTMLDivElement> {
  icon?: React.ReactNode
  title: string
  description?: string
  chips?: string[]
  action?: React.ReactNode
}

export const EmptyState = React.forwardRef<HTMLDivElement, EmptyStateProps>(
  ({ className, icon, title, description, chips, action, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn(
          'flex flex-col items-center justify-center p-8 text-center max-w-[420px] mx-auto',
          className
        )}
        {...props}
      >
        <div className="w-14 h-14 rounded-[8px] bg-sh-surface-muted border border-sh-border flex items-center justify-center text-sh-muted mb-4 shrink-0 shadow-sm">
          {icon || (
            <svg
              className="w-6 h-6 text-sh-muted"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
              strokeWidth={1.5}
              aria-hidden="true"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
              />
            </svg>
          )}
        </div>

        <h4 className="typography-body-strong text-sh-ink font-semibold mb-1">{title}</h4>

        {description && (
          <p className="typography-meta text-sh-muted mb-3 max-w-[340px]">{description}</p>
        )}

        {chips && chips.length > 0 && (
          <div className="flex flex-wrap items-center justify-center gap-1.5 mb-4">
            {chips.map((chip, idx) => (
              <span
                key={`${chip}-${idx}`}
                className="typography-code-sm bg-sh-surface-muted text-sh-ink-secondary border border-sh-border rounded-[6px] px-2 py-0.5"
              >
                {chip}
              </span>
            ))}
          </div>
        )}

        {action && <div className="mt-2">{action}</div>}
      </div>
    )
  }
)

EmptyState.displayName = 'EmptyState'
