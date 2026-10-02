import React from 'react'
import { cn } from '../../lib/utils'
import { getStatusColorInfo, type UnitStatus, type UnitStatusKey } from '../../tokens'

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  status?: UnitStatus | UnitStatusKey | string
  interactive?: boolean
}

export const Card = React.forwardRef<HTMLDivElement, CardProps>(
  ({ className, status, interactive = false, children, ...props }, ref) => {
    const statusInfo = status ? getStatusColorInfo(status) : undefined

    return (
      <div
        ref={ref}
        className={cn(
          'relative bg-sh-surface border border-sh-border rounded-sh-md shadow-sh-card overflow-hidden transition-colors',
          interactive && 'hover:border-sh-border-strong cursor-pointer',
          className
        )}
        {...props}
      >
        {statusInfo && statusInfo.bar && (
          <span
            data-testid="card-status-bar"
            className="absolute left-0 top-0 bottom-0 w-[3px] z-10"
            style={{ backgroundColor: statusInfo.bar }}
            aria-hidden="true"
          />
        )}
        {children}
      </div>
    )
  }
)

Card.displayName = 'Card'

export const CardHeader = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('p-4 pb-2 flex flex-col gap-1', className)} {...props} />
))
CardHeader.displayName = 'CardHeader'

export const CardTitle = React.forwardRef<
  HTMLHeadingElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h3
    ref={ref}
    className={cn('typography-headline text-sh-ink font-semibold', className)}
    {...props}
  />
))
CardTitle.displayName = 'CardTitle'

export const CardDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <p ref={ref} className={cn('typography-meta text-sh-muted', className)} {...props} />
))
CardDescription.displayName = 'CardDescription'

export const CardContent = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div ref={ref} className={cn('p-4 pt-2', className)} {...props} />
))
CardContent.displayName = 'CardContent'

export const CardFooter = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn('p-4 pt-0 flex items-center border-t border-sh-divider mt-2', className)}
    {...props}
  />
))
CardFooter.displayName = 'CardFooter'
