import React, { useId } from 'react'
import { cn } from '../../lib/utils'

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  helperText?: string
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ className, id: customId, label, error, helperText, disabled, required, ...props }, ref) => {
    const generatedId = useId()
    const inputId = customId || generatedId
    const errorId = `${inputId}-error`
    const helperId = `${inputId}-helper`

    const describedBy = error ? errorId : helperText ? helperId : undefined

    return (
      <div className="flex flex-col gap-1 w-full text-left">
        {label && (
          <label
            htmlFor={inputId}
            className="typography-label text-sh-muted flex items-center gap-1 select-none"
          >
            {label}
            {required && <span className="text-sh-error" aria-hidden="true">*</span>}
          </label>
        )}
        <input
          ref={ref}
          id={inputId}
          disabled={disabled}
          required={required}
          aria-invalid={Boolean(error)}
          aria-describedby={describedBy}
          className={cn(
            'h-[36px] w-full px-3 text-[13px] text-sh-ink bg-sh-surface rounded-sh-md border transition-colors outline-none placeholder:text-sh-faint',
            'focus:outline-none focus:ring-2 focus:ring-sh-primary focus:ring-offset-1 focus:border-transparent',
            'disabled:opacity-50 disabled:cursor-not-allowed disabled:bg-sh-surface-subtle',
            error
              ? 'border-sh-error focus:ring-sh-error focus:ring-offset-1'
              : 'border-sh-border hover:border-sh-border-strong',
            className
          )}
          {...props}
        />
        {error ? (
          <p id={errorId} role="alert" className="typography-meta text-sh-error mt-0.5">
            {error}
          </p>
        ) : helperText ? (
          <p id={helperId} className="typography-meta text-sh-muted mt-0.5">
            {helperText}
          </p>
        ) : null}
      </div>
    )
  }
)

Input.displayName = 'Input'
