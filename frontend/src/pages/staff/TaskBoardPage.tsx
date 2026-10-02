import React, { useState, useContext } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import type { TaskDto, TaskStatus, TaskType } from '../../types/task'
import { TASK_TYPE_CONFIG } from '../../types/task'
import { getTasks, updateTaskStatus } from '../../api/task'
import { KanbanColumn } from '../../components/task/KanbanColumn'
import { ToastContext } from '../../context/toast-context-base'
import { SH_COLORS, SH_RADII } from '../../tokens'

const TASK_TYPES: { type: TaskType | 'ALL'; label: string }[] = [
  { type: 'ALL', label: 'All' },
  { type: 'CHECK_IN', label: 'Check-in' },
  { type: 'CHECKOUT', label: 'Checkout' },
  { type: 'CLEANING', label: 'Cleaning' },
  { type: 'SUPPORT', label: 'Support' },
  { type: 'CONTRACT', label: 'Contract' },
]

export const TaskBoardPage: React.FC = () => {
  const queryClient = useQueryClient()
  const toastCtx = useContext(ToastContext)

  const [activeType, setActiveType] = useState<TaskType | 'ALL'>('ALL')
  const [selectedDate, setSelectedDate] = useState<string>(
    () => new Date().toISOString().split('T')[0]
  )

  // Fetch tasks
  const { data: tasks = [], isLoading, error } = useQuery<TaskDto[]>({
    queryKey: ['tasks', selectedDate],
    queryFn: () => getTasks(selectedDate ? { workDate: selectedDate } : undefined),
  })

  // Status update mutation
  const updateMutation = useMutation({
    mutationFn: ({ taskId, newStatus }: { taskId: number; newStatus: TaskStatus }) =>
      updateTaskStatus(taskId, { status: newStatus }),
    onMutate: async ({ taskId, newStatus }) => {
      await queryClient.cancelQueries({ queryKey: ['tasks', selectedDate] })
      const previousTasks = queryClient.getQueryData<TaskDto[]>(['tasks', selectedDate])

      if (previousTasks) {
        queryClient.setQueryData<TaskDto[]>(
          ['tasks', selectedDate],
          previousTasks.map((t) => (t.id === taskId ? { ...t, status: newStatus } : t))
        )
      }

      return { previousTasks }
    },
    onError: (_err, _variables, context) => {
      if (context?.previousTasks) {
        queryClient.setQueryData(['tasks', selectedDate], context.previousTasks)
      }
      toastCtx?.showToast({
        title: 'Unable to update task status. Please try again.',
        tone: 'error',
      })
    },
    onSuccess: (updatedTask) => {
      queryClient.invalidateQueries({ queryKey: ['tasks'] })
      const statusLabel =
        updatedTask.status === 'TODO'
          ? 'To do'
          : updatedTask.status === 'IN_PROGRESS'
          ? 'In progress'
          : 'Done'
      toastCtx?.showToast({
        title: `Task ${updatedTask.refCode || updatedTask.id} moved to ${statusLabel}`,
        tone: 'success',
      })
    },
  })

  const handleStatusChange = (taskId: number, newStatus: TaskStatus) => {
    updateMutation.mutate({ taskId, newStatus })
  }

  // Filter tasks by active tab
  const filteredTasks = tasks.filter((task) => {
    if (activeType !== 'ALL' && task.type !== activeType) return false
    return true
  })

  const todoTasks = filteredTasks.filter((t) => t.status === 'TODO')
  const inProgressTasks = filteredTasks.filter((t) => t.status === 'IN_PROGRESS')
  const doneTasks = filteredTasks.filter((t) => t.status === 'DONE')

  return (
    <div
      style={{
        padding: '24px',
        maxWidth: '1440px',
        margin: '0 auto',
        display: 'flex',
        flexDirection: 'column',
        gap: '20px',
      }}
    >
      {/* Header section */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '16px',
        }}
      >
        <div>
          <h1
            style={{
              fontSize: '21px',
              fontWeight: 700,
              color: SH_COLORS.ink,
              margin: 0,
            }}
          >
            Task Board
          </h1>
          <p style={{ fontSize: '13px', color: SH_COLORS.muted, margin: '4px 0 0 0' }}>
            Shift operations, check-in rituals, and turnover tasks.
          </p>
        </div>

        {/* Work date filter input */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <label
            htmlFor="work-date-filter"
            style={{ fontSize: '12px', fontWeight: 600, color: SH_COLORS.inkSecondary }}
          >
            Date:
          </label>
          <input
            id="work-date-filter"
            type="date"
            data-testid="work-date-input"
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            style={{
              padding: '6px 10px',
              borderRadius: SH_RADII.sm,
              border: `1px solid ${SH_COLORS.border}`,
              backgroundColor: SH_COLORS.surface,
              fontSize: '13px',
              color: SH_COLORS.ink,
            }}
          />
          {selectedDate ? (
            <button
              type="button"
              data-testid="all-dates-btn"
              onClick={() => setSelectedDate('')}
              style={{
                padding: '6px 10px',
                fontSize: '12px',
                color: SH_COLORS.inkSecondary,
                background: 'none',
                border: `1px solid ${SH_COLORS.border}`,
                borderRadius: SH_RADII.sm,
                cursor: 'pointer',
              }}
            >
              All dates
            </button>
          ) : (
            <button
              type="button"
              data-testid="today-btn"
              onClick={() => setSelectedDate(new Date().toISOString().split('T')[0])}
              style={{
                padding: '6px 10px',
                fontSize: '12px',
                color: SH_COLORS.primary,
                background: 'none',
                border: `1px solid ${SH_COLORS.primaryOutlineBorder}`,
                borderRadius: SH_RADII.sm,
                cursor: 'pointer',
              }}
            >
              Today
            </button>
          )}
        </div>
      </div>

      {/* Filter Tabs */}
      <div
        role="tablist"
        aria-label="Filter tasks by type"
        style={{
          display: 'flex',
          gap: '8px',
          flexWrap: 'wrap',
          borderBottom: `1px solid ${SH_COLORS.divider}`,
          paddingBottom: '12px',
        }}
      >
        {TASK_TYPES.map(({ type, label }) => {
          const isActive = activeType === type
          const config = type !== 'ALL' ? TASK_TYPE_CONFIG[type] : null

          return (
            <button
              key={type}
              role="tab"
              aria-selected={isActive}
              data-testid={`filter-tab-${type.toLowerCase().replace('_', '-')}`}
              onClick={() => setActiveType(type)}
              style={{
                padding: '6px 14px',
                fontSize: '13px',
                fontWeight: isActive ? 600 : 500,
                color: isActive ? SH_COLORS.primary : SH_COLORS.inkSecondary,
                backgroundColor: isActive ? SH_COLORS.primaryTint : SH_COLORS.surface,
                border: `1px solid ${isActive ? SH_COLORS.primaryOutlineBorder : SH_COLORS.border}`,
                borderRadius: SH_RADII.sm,
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                transition: 'all 0.15s ease',
              }}
            >
              {config && (
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: config.barColor,
                    display: 'inline-block',
                  }}
                />
              )}
              {label}
            </button>
          )
        })}
      </div>

      {/* Kanban Columns container */}
      {isLoading ? (
        <div
          data-testid="tasks-loading-skeleton"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '20px',
            minHeight: '400px',
          }}
        >
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              style={{
                backgroundColor: SH_COLORS.surfaceSubtle,
                borderRadius: SH_RADII.md,
                border: `1px solid ${SH_COLORS.border}`,
                padding: '16px',
                height: '320px',
              }}
            />
          ))}
        </div>
      ) : error ? (
        <div
          style={{
            padding: '24px',
            backgroundColor: SH_COLORS.errorTint,
            border: `1px solid ${SH_COLORS.errorBorder}`,
            borderRadius: SH_RADII.md,
            color: SH_COLORS.error,
            fontSize: '13px',
          }}
        >
          Failed to load tasks. Please refresh the page or contact support.
        </div>
      ) : (
        <div
          data-testid="kanban-board"
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(3, 1fr)',
            gap: '20px',
            alignItems: 'start',
            overflowX: 'auto',
            paddingBottom: '16px',
          }}
        >
          <KanbanColumn
            status="TODO"
            title="To do"
            tasks={todoTasks}
            onStatusChange={handleStatusChange}
          />
          <KanbanColumn
            status="IN_PROGRESS"
            title="In progress"
            tasks={inProgressTasks}
            onStatusChange={handleStatusChange}
          />
          <KanbanColumn
            status="DONE"
            title="Done"
            tasks={doneTasks}
            onStatusChange={handleStatusChange}
          />
        </div>
      )}
    </div>
  )
}
