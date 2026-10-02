import React from 'react'
import { cn } from '../../lib/utils'
import {
  getStatusColorInfo,
  SH_COLORS,
  type UnitStatus,
  type UnitStatusKey,
} from '../../tokens'

export type BadgeVariant =
  | UnitStatus
  | UnitStatusKey
  | 'neutral'
  | 'success'
  | 'warning'
  | 'error'

export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  status?: BadgeVariant | string
  showDot?: boolean
}

export const Badge = React.forwardRef<HTMLSpanElement, BadgeProps>(
  ({ className, status = 'neutral', showDot = true, children, ...props }, ref) => {
    let bg: string = SH_COLORS.surfaceMuted
    let border: string = SH_COLORS.border
    let text: string = SH_COLORS.inkSecondary
    let dotColor: string | null = null
    let defaultLabel = 'Neutral'

    const lowerStatus = (status || '').toLowerCase()

    if (
      lowerStatus === 'available' ||
      lowerStatus === 'buffer' ||
      lowerStatus === 'reserved' ||
      lowerStatus === 'rented' ||
      lowerStatus === 'preparing' ||
      lowerStatus === 'maintenance' ||
      lowerStatus === 'retired'
    ) {
      const info = getStatusColorInfo(lowerStatus)
      if (info) {
        bg = info.bg
        border = info.border
        text = info.text
        dotColor = info.dot
        defaultLabel = info.label
      }
    } else if (lowerStatus === 'success') {
      bg = SH_COLORS.successTint
      border = SH_COLORS.statusAvailableBorder
      text = SH_COLORS.success
      dotColor = SH_COLORS.success
      defaultLabel = 'Success'
    } else if (lowerStatus === 'warning') {
      bg = SH_COLORS.warningTint
      border = SH_COLORS.statusBufferBorder
      text = SH_COLORS.warning
      dotColor = SH_COLORS.warningBar
      defaultLabel = 'Warning'
    } else if (lowerStatus === 'error') {
      bg = SH_COLORS.errorTint
      border = SH_COLORS.errorBorder
      text = SH_COLORS.error
      dotColor = SH_COLORS.error
      defaultLabel = 'Error'
    }

    const hasDot = showDot && Boolean(dotColor)

    return (
      <span
        ref={ref}
        data-testid="badge"
        className={cn(
          'inline-flex items-center rounded-sh-sm py-[3px] px-[8px] border typography-label select-none font-semibold leading-none',
          className
        )}
        style={{
          backgroundColor: bg,
          borderColor: border,
          color: text,
        }}
        {...props}
      >
        {hasDot && (
          <span
            data-testid="badge-dot"
            className="w-[7px] h-[7px] rounded-full mr-1.5 shrink-0 inline-block"
            style={{ backgroundColor: dotColor! }}
            aria-hidden="true"
          />
        )}
        <span>{children ?? defaultLabel}</span>
      </span>
    )
  }
)

Badge.displayName = 'Badge'
