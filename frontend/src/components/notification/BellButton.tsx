import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getUnreadCount } from '../../api/notification'
import { NotificationDrawer } from './NotificationCenter'
import { cn } from '../../lib/utils'

export interface BellButtonProps {
  className?: string
  refetchInterval?: number
}

export function BellButton({ className, refetchInterval = 15000 }: BellButtonProps) {
  const [drawerOpen, setDrawerOpen] = useState(false)

  const { data } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
    refetchInterval,
    enabled: typeof window !== 'undefined' && Boolean(localStorage.getItem('storagehub_token')),
  })

  const count = data?.count ?? 0

  return (
    <>
      <button
        type="button"
        aria-label="Notifications"
        onClick={() => setDrawerOpen(true)}
        className={cn(
          'relative p-1.5 text-sh-muted hover:text-sh-ink rounded-sh-md transition-colors',
          'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sh-primary cursor-pointer',
          className
        )}
      >
        <svg
          className="w-5 h-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
          strokeWidth="2"
          aria-hidden="true"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
          />
        </svg>

        {count > 0 && (
          <span
            data-testid="bell-badge"
            className="absolute -top-1 -right-1 min-w-[16px] h-4 px-1 rounded-full bg-sh-error text-white font-bold text-[10px] leading-none flex items-center justify-center shadow-sm"
          >
            {count > 99 ? '99+' : count}
          </span>
        )}
      </button>

      <NotificationDrawer open={drawerOpen} onOpenChange={setDrawerOpen} />
    </>
  )
}

export default BellButton
