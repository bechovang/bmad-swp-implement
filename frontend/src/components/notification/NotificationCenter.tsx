import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { getNotifications, markAsRead, markAllAsRead, getUnreadCount } from '../../api/notification'
import { Drawer } from '../ui/Drawer'
import { EmptyState } from '../ui/EmptyState'
import { Button } from '../ui/Button'
import { Badge } from '../ui/Badge'
import { cn } from '../../lib/utils'
import { formatNotificationDate } from '../../lib/format'
import type { NotificationDto } from '../../types/notification'

export interface NotificationListProps {
  onItemClick?: () => void
  showHeaderControls?: boolean
  className?: string
}

export function NotificationList({
  onItemClick,
  showHeaderControls = true,
  className,
}: NotificationListProps) {
  const [filter, setFilter] = useState<'all' | 'unread'>('all')
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const {
    data: listData,
    isLoading,
    refetch,
  } = useQuery({
    queryKey: ['notifications', 'list'],
    queryFn: () => getNotifications(1, 50),
  })

  const { data: unreadData } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
  })

  const markReadMutation = useMutation({
    mutationFn: (id: number) => markAsRead(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const markAllMutation = useMutation({
    mutationFn: () => markAllAsRead(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
  })

  const allItems = listData?.items || []
  const displayedItems =
    filter === 'unread' ? allItems.filter((item) => !item.isRead) : allItems
  const unreadCount = unreadData?.count ?? allItems.filter((i) => !i.isRead).length

  const handleItemClick = async (item: NotificationDto) => {
    if (!item.isRead) {
      try {
        await markReadMutation.mutateAsync(item.id)
      } catch (err) {
        console.error('Failed to mark notification as read', err)
      }
    }
    if (onItemClick) {
      onItemClick()
    }
    if (item.deepLink) {
      navigate(item.deepLink)
    }
  }

  const handleMarkAllRead = async () => {
    try {
      await markAllMutation.mutateAsync()
      refetch()
    } catch (err) {
      console.error('Failed to mark all as read', err)
    }
  }

  return (
    <div className={cn('flex flex-col h-full', className)}>
      {/* Header controls: Filter tabs & Mark all read button */}
      {showHeaderControls && (
        <div className="flex items-center justify-between gap-2 pb-3 mb-3 border-b border-sh-divider">
          <div className="flex items-center gap-1.5" role="tablist">
            <button
              type="button"
              role="tab"
              aria-selected={filter === 'all'}
              onClick={() => setFilter('all')}
              className={cn(
                'px-2.5 py-1 text-xs font-semibold rounded-sh-sm transition-colors cursor-pointer',
                filter === 'all'
                  ? 'bg-sh-primary text-white'
                  : 'bg-sh-surface-muted text-sh-muted hover:text-sh-ink'
              )}
            >
              All ({listData?.total ?? allItems.length})
            </button>
            <button
              type="button"
              role="tab"
              aria-selected={filter === 'unread'}
              onClick={() => setFilter('unread')}
              className={cn(
                'px-2.5 py-1 text-xs font-semibold rounded-sh-sm transition-colors cursor-pointer',
                filter === 'unread'
                  ? 'bg-sh-primary text-white'
                  : 'bg-sh-surface-muted text-sh-muted hover:text-sh-ink'
              )}
            >
              Unread ({unreadCount})
            </button>
          </div>

          <button
            type="button"
            onClick={handleMarkAllRead}
            disabled={unreadCount === 0 || markAllMutation.isPending}
            className="typography-meta text-sh-primary hover:underline disabled:text-sh-faint disabled:no-underline font-medium cursor-pointer disabled:cursor-not-allowed"
          >
            Mark all as read
          </button>
        </div>
      )}

      {/* Notifications list feed */}
      <div role="tabpanel" aria-label="Notifications list" className="flex-1 overflow-y-auto space-y-2">
        {isLoading ? (
          <div className="py-8 text-center text-sh-muted typography-body" data-testid="notifications-loading">
            Loading notifications...
          </div>
        ) : displayedItems.length === 0 ? (
          <EmptyState
            title="No notifications yet"
            description={
              filter === 'unread'
                ? "You've read all your notifications!"
                : "You're all caught up! Important updates will appear here."
            }
            icon={
              <svg
                className="w-6 h-6 text-sh-muted"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                strokeWidth="1.5"
                aria-hidden="true"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"
                />
              </svg>
            }
          />
        ) : (
          displayedItems.map((item) => (
            <div
              key={item.id}
              role="button"
              tabIndex={0}
              data-testid={`notification-item-${item.id}`}
              onClick={() => handleItemClick(item)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault()
                  handleItemClick(item)
                }
              }}
              className={cn(
                'p-3 rounded-sh-md border text-left transition-colors cursor-pointer relative group',
                item.isRead
                  ? 'bg-sh-surface border-sh-border text-sh-ink-secondary hover:bg-sh-surface-subtle'
                  : 'bg-sh-primary-tint/20 border-sh-primary-outline-border text-sh-ink hover:bg-sh-primary-tint/30'
              )}
            >
              <div className="flex items-start justify-between gap-2">
                <div className="flex items-center gap-1.5 flex-1 min-w-0">
                  {!item.isRead && (
                    <span
                      aria-label="Unread"
                      data-testid="unread-dot"
                      className="w-2 h-2 rounded-full bg-sh-primary shrink-0"
                    />
                  )}
                  <p
                    className={cn(
                      'typography-body leading-snug line-clamp-2',
                      item.isRead ? 'font-normal text-sh-ink-secondary' : 'font-semibold text-sh-ink'
                    )}
                  >
                    {item.title}
                  </p>
                </div>
                <span className="typography-meta text-sh-faint shrink-0 whitespace-nowrap">
                  {formatNotificationDate(item.createdAt)}
                </span>
              </div>

              <div className="mt-2 flex items-center justify-between text-xs">
                <span className="typography-code-sm text-sh-muted uppercase tracking-wider">
                  {item.type ? item.type.replace(/_/g, ' ') : ''}
                </span>
                {item.deepLink && (
                  <span className="text-sh-primary group-hover:underline font-semibold typography-meta">
                    View &rarr;
                  </span>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

export interface NotificationDrawerProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function NotificationDrawer({ open, onOpenChange }: NotificationDrawerProps) {
  const { data: unreadData } = useQuery({
    queryKey: ['notifications', 'unread-count'],
    queryFn: getUnreadCount,
  })

  const unreadCount = unreadData?.count ?? 0

  return (
    <Drawer
      open={open}
      onOpenChange={onOpenChange}
      title={
        <div className="flex items-center gap-2">
          <span>Notifications</span>
          {unreadCount > 0 && (
            <Badge status="error" showDot={false}>
              {unreadCount} new
            </Badge>
          )}
        </div>
      }
      footer={
        <div className="w-full flex items-center justify-between">
          <Link
            to="/notifications"
            onClick={() => onOpenChange(false)}
            className="typography-meta text-sh-primary hover:underline font-semibold"
          >
            Open full Notification Center &rarr;
          </Link>
          <Button variant="ghost" size="sm" onClick={() => onOpenChange(false)}>
            Close
          </Button>
        </div>
      }
    >
      <NotificationList onItemClick={() => onOpenChange(false)} />
    </Drawer>
  )
}

export function NotificationCenter({
  asDrawer = false,
  open = false,
  onOpenChange,
}: {
  asDrawer?: boolean
  open?: boolean
  onOpenChange?: (open: boolean) => void
}) {
  if (asDrawer && onOpenChange) {
    return <NotificationDrawer open={open} onOpenChange={onOpenChange} />
  }

  return <NotificationList />
}

export default NotificationCenter
