import React from 'react'
import { cn } from '../../lib/utils'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'destructive'
export type ButtonSize = 'default' | 'page' | 'sm'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  isLoading?: boolean
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      className,
      type = 'button',
      variant = 'primary',
      size = 'default',
      isLoading = false,
      disabled,
      children,
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center font-semibold rounded-sh-md transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary focus-visible:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed disabled:pointer-events-none select-none text-[13px] leading-none'

    const variantStyles: Record<ButtonVariant, string> = {
      primary:
        'bg-sh-primary text-white shadow-sh-button-primary hover:bg-[#4338CA] active:bg-[#3730A3]',
      secondary:
        'bg-sh-surface border border-sh-border-strong text-sh-ink-secondary hover:bg-sh-surface-subtle active:bg-sh-surface-muted',
      ghost:
        'bg-transparent text-sh-ink-secondary hover:bg-sh-surface-subtle active:bg-sh-surface-muted',
      destructive:
        'bg-sh-surface border border-sh-border-strong text-sh-error hover:bg-sh-error-tint active:bg-[#FEE2E2]',
    }

    const sizeStyles: Record<ButtonSize, string> = {
      sm: 'h-[32px] px-3 text-[12px]',
      default: 'h-[36px] px-3.5',
      page: 'h-[40px] px-4',
    }

    const isDisabled = disabled || isLoading

    return (
      <button
        ref={ref}
        type={type}
        disabled={isDisabled}
        aria-busy={isLoading}
        className={cn(baseStyles, variantStyles[variant], sizeStyles[size], className)}
        {...props}
      >
        {isLoading && (
          <>
            <svg
              className="animate-spin -ml-1 mr-2 h-4 w-4"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              aria-hidden="true"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <span className="sr-only">Loading...</span>
          </>
        )}
        {children}
      </button>
    )
  }
)

Button.displayName = 'Button'
