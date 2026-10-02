import React from 'react'
import { cn } from '../../lib/utils'
import { APP_ROLES, type AppRole } from '../../tokens'

export interface RoleChipProps extends React.HTMLAttributes<HTMLSpanElement> {
  role: AppRole | string
  label?: string
}

export const RoleChip = React.forwardRef<HTMLSpanElement, RoleChipProps>(
  ({ className, role, label, ...props }, ref) => {
    const roleKey = (role || '').toUpperCase() as AppRole
    const displayLabel = label || APP_ROLES[roleKey]?.label || role

    return (
      <span
        ref={ref}
        data-testid="role-chip"
        className={cn(
          'inline-flex items-center rounded-sh-sm py-[3px] px-[8px] border bg-sh-primary-tint border-sh-primary-border text-sh-primary typography-label font-semibold select-none leading-none',
          className
        )}
        {...props}
      >
        {displayLabel}
      </span>
    )
  }
)

RoleChip.displayName = 'RoleChip'
