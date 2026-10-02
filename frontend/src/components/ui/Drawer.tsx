import React from 'react'
import * as DialogPrimitive from '@radix-ui/react-dialog'
import { cn } from '../../lib/utils'

export const DrawerRoot = DialogPrimitive.Root
export const DrawerTrigger = DialogPrimitive.Trigger
export const DrawerPortal = DialogPrimitive.Portal
export const DrawerClose = DialogPrimitive.Close

export const DrawerOverlay = React.forwardRef<
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
DrawerOverlay.displayName = 'DrawerOverlay'

export const DrawerContent = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Content>
>(({ className, children, ...props }, ref) => (
  <DrawerPortal>
    <DrawerOverlay />
    <DialogPrimitive.Content
      ref={ref}
      className={cn(
        'fixed right-0 top-0 bottom-0 z-50 h-full w-[420px] max-w-full',
        'bg-sh-surface border-l border-sh-border p-6 shadow-sh-overlay outline-none flex flex-col',
        'duration-300 data-[state=open]:animate-in data-[state=closed]:animate-out',
        'data-[state=closed]:slide-out-to-right data-[state=open]:slide-in-from-right',
        className
      )}
      {...props}
    >
      {children}
    </DialogPrimitive.Content>
  </DrawerPortal>
))
DrawerContent.displayName = 'DrawerContent'

export interface DrawerHeaderProps extends React.HTMLAttributes<HTMLDivElement> {
  showClose?: boolean
}

export const DrawerHeader = ({
  className,
  showClose = true,
  children,
  ...props
}: DrawerHeaderProps) => (
  <div
    className={cn(
      'flex flex-col gap-1.5 pb-4 mb-4 border-b border-sh-divider text-left relative pr-6',
      className
    )}
    {...props}
  >
    {children}
    {showClose && (
      <DrawerClose
        aria-label="Close"
        className="absolute right-0 top-0 text-sh-muted hover:text-sh-ink p-1 rounded-sh-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary text-[14px] leading-none cursor-pointer"
      >
        <span aria-hidden="true">✕</span>
      </DrawerClose>
    )}
  </div>
)
DrawerHeader.displayName = 'DrawerHeader'

export const DrawerTitle = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Title>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Title>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Title
    ref={ref}
    className={cn('typography-headline text-sh-ink font-semibold', className)}
    {...props}
  />
))
DrawerTitle.displayName = 'DrawerTitle'

export const DrawerDescription = React.forwardRef<
  React.ComponentRef<typeof DialogPrimitive.Description>,
  React.ComponentPropsWithoutRef<typeof DialogPrimitive.Description>
>(({ className, ...props }, ref) => (
  <DialogPrimitive.Description
    ref={ref}
    className={cn('typography-body text-sh-ink-secondary', className)}
    {...props}
  />
))
DrawerDescription.displayName = 'DrawerDescription'

export const DrawerBody = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div className={cn('flex-1 overflow-y-auto py-2 text-left', className)} {...props} />
)
DrawerBody.displayName = 'DrawerBody'

export const DrawerFooter = ({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) => (
  <div
    className={cn(
      'flex items-center justify-end gap-2 pt-4 mt-auto border-t border-sh-divider',
      className
    )}
    {...props}
  />
)
DrawerFooter.displayName = 'DrawerFooter'

export interface DrawerProps {
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

export function Drawer({
  open,
  onOpenChange,
  title,
  description,
  children,
  footer,
  trigger,
  showClose = true,
  className,
}: DrawerProps) {
  return (
    <DrawerRoot open={open} onOpenChange={onOpenChange}>
      {trigger &&
        (React.isValidElement(trigger) ? (
          <DrawerTrigger asChild>{trigger}</DrawerTrigger>
        ) : (
          <DrawerTrigger>{trigger}</DrawerTrigger>
        ))}
      <DrawerContent className={className}>
        {!title && (
          <DialogPrimitive.Title className="sr-only">Drawer</DialogPrimitive.Title>
        )}
        {(title || description) ? (
          <DrawerHeader showClose={showClose}>
            {title && <DrawerTitle>{title}</DrawerTitle>}
            {description && <DrawerDescription>{description}</DrawerDescription>}
          </DrawerHeader>
        ) : (
          showClose && (
            <DrawerClose
              aria-label="Close"
              className="absolute right-4 top-4 text-sh-muted hover:text-sh-ink p-1 rounded-sh-sm transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary text-[14px] leading-none cursor-pointer"
            >
              <span aria-hidden="true">✕</span>
            </DrawerClose>
          )
        )}
        <DrawerBody>{children}</DrawerBody>
        {footer && <DrawerFooter>{footer}</DrawerFooter>}
      </DrawerContent>
    </DrawerRoot>
  )
}
