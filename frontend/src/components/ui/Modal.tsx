import React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { cn } from '../../lib/utils'

export const ModalRoot = DialogPrimitive.Root
export const ModalTrigger = DialogPrimitive.Trigger
export const ModalPortal = DialogPrimitive.Portal
export const ModalClose = DialogPrimitive.Close

export const ModalOverlay = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Overlay>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Overlay>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Overlay
    ref={ref}
    className={cn(
      'fixed inset-0 z-50 bg-sh-scrim backdrop-blur-[0.5px]',
      'data-[state=open]:animate-in data-[state=closed]:animate-out data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0',
      className
    )}
    {...props}
  />
))
ModalOverlay.displayName = 'ModalOverlay'

export const ModalContent = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content>
>(({ className, children, ...props }, ref) => (
  <ModalPortal>
    <ModalOverlay />
    <DialogPrimitive.Content
      ref={ref}
      className={cn(
        'fixed left-1/2 top-1/2 z-50 -translate-x-1/2 -translate-y-1/2 w-full max-w-[480px]',
        'bg-sh-surface border border-sh-border rounded-sh-md p-6 shadow-sh-overlay outline-none',
        'duration-200 data-[state=open]:animate-in data-[state=closed]:animate-out',
        'data-[state=closed]:fade-out-0 data-[state=open]:fade-in-0 data-[state=closed]:zoom-out-95 data-[state=open]:zoom-in-95',
        className
      )}
      {...props}
    >
      {children}
    </DialogPrimitive.Content>
  </ModalPortal>
))
ModalContent.displayName = 'ModalContent'

export interface ModalHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  showClose?: boolean
}

export const ModalHeader = ({
  className,
  showClose = true,
  children,
  ...props
}: ModalHeaderProps) => (
  <div className={cn('flex flex-col gap-1.5 mb-4 text-left relative pr-6', className)} {...props}>
    {children}
    {showClose && (
      <ModalClose
        aria-label="Close"
        className="absolute right-0 top-0 text-sh-muted hover:text-sh-ink p-1 rounded-sh-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary text-[14px] leading-none cursor-pointer"
      >
        <span aria-hidden="true">✕</span>
      </ModalClose>
    )}
  </div>
)
ModalHeader.displayName = 'ModalHeader'

export const ModalTitle = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn('typography-headline text-sh-ink font-semibold', className)}
    {...props}
  />
))
ModalTitle.displayName = 'ModalTitle'

export const ModalDescription = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn('typography-body text-sh-ink-secondary', className)}
    {...props}
  />
))
ModalDescription.displayName = 'ModalDescription'

export const ModalFooter = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn(
      'flex items-center justify-end gap-2 mt-6 pt-4 border-t border-sh-divider',
      className
    )}
    {...props}
  />
)
ModalFooter.displayName = 'ModalFooter'

export interface ModalProps {
  open?: boolean
  onOpenChange?: (open: boolean) => void
  title?: React.ReactNode
  description?: React.ReactNode
  children?: React.ReactNode
  footer?: React.ReactNode
  trigger?: React.ReactNode
  showClose?: boolean
  className?: string
}

export function Modal({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  trigger,
  showClose = true,
  className,
}: ModalProps) {
  return (
    <ModalRoot open={open} onOpenChange={onOpenChange}>
      {trigger &&
        (React.isValidElement(trigger) ? (
          <ModalTrigger asChild>{trigger}</ModalTrigger>
        ) : (
          <ModalTrigger>{trigger}</ModalTrigger>
        ))}
      <ModalContent className={className}>
        {!title && (
          <DialogPrimitive.Title className="sr-only">Dialog</DialogPrimitive.Title>
        )}
        {(title || description) ? (
          <ModalHeader showClose={showClose}>
            {title && <ModalTitle>{title}</ModalTitle>}
            {description && <ModalDescription>{description}</ModalDescription>}
          </ModalHeader>
        ) : (
          showClose && (
            <ModalClose
              aria-label="Close"
              className="absolute right-4 top-4 text-sh-muted hover:text-sh-ink p-1 rounded-sh-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary text-[14px] leading-none cursor-pointer"
            >
              <span aria-hidden="true">✕</span>
            </ModalClose>
          )
        )}
        {children}
        {footer && <ModalFooter>{footer}</ModalFooter>}
      </ModalContent>
    </ModalRoot>
  )
}
