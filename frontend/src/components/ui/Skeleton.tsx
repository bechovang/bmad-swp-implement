import React from 'react'
import { cn } from '../../lib/utils'

export type SkeletonProps = React.HTMLAttributes<HTMLDivElement>

export const Skeleton = React.forwardRef<HTMLDivElement, SkeletonProps>(
  ({ className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        className={cn('animate-pulse bg-sh-surface-muted rounded-[8px]', className)}
        {...props}
      />
    )
  }
)

Skeleton.displayName = 'Skeleton'
