import React from 'react'
import * as TabsPrimitive from '@radix-ui/react-tabs'
import { cn } from '../../lib/utils'

export const Tabs = TabsPrimitive.Root

export const TabsList = React.forwardRef<
  React.ComponentRef<typeof TabsPrimitive.List>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.List>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.List
    ref={ref}
    className={cn(
      'inline-flex items-center gap-6 border-b border-sh-border w-full text-sh-muted select-none',
      className
    )}
    {...props}
  />
))
TabsList.displayName = 'TabsList'

export const TabsTrigger = React.forwardRef<
  React.ComponentRef<typeof TabsPrimitive.Trigger>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Trigger>
>(({ className, children, ...props }, ref) => (
  <TabsPrimitive.Trigger
    ref={ref}
    className={cn(
      'relative pb-3 pt-1 text-[13px] font-medium typography-nav transition-colors outline-none cursor-pointer',
      'text-sh-muted hover:text-sh-ink',
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary focus-visible:ring-offset-2',
      'data-[state=active]:text-sh-primary data-[state=active]:font-semibold',
      'after:absolute after:bottom-0 after:left-0 after:right-0 after:h-[2px] after:scale-x-0 after:transition-transform',
      'data-[state=active]:after:scale-x-100 data-[state=active]:after:bg-sh-primary',
      'disabled:pointer-events-none disabled:opacity-50',
      className
    )}
    {...props}
  >
    {children}
  </TabsPrimitive.Trigger>
))
TabsTrigger.displayName = 'TabsTrigger'

export const TabsContent = React.forwardRef<
  React.ComponentRef<typeof TabsPrimitive.Content>,
  React.ComponentPropsWithoutRef<typeof TabsPrimitive.Content>
>(({ className, ...props }, ref) => (
  <TabsPrimitive.Content
    ref={ref}
    className={cn(
      'mt-4 outline-none focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary',
      className
    )}
    {...props}
  />
))
TabsContent.displayName = 'TabsContent'
