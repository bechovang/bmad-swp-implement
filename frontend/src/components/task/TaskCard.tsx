import React from 'react'
import type { TaskDto, TaskStatus } from '../../types/task'
import { TASK_TYPE_CONFIG } from '../../types/task'
import { SH_COLORS, SH_RADII } from '../../tokens'

interface TaskCardProps {
  task: TaskDto
  onStatusChange: (taskId: number, newStatus: TaskStatus) => void
  onCardClick?: (task: TaskDto) => void
}

export const TaskCard: React.FC<TaskCardProps> = ({
  task,
  onStatusChange,
  onCardClick,
}) => {
  const config = TASK_TYPE_CONFIG[task.type] || TASK_TYPE_CONFIG.CHECK_IN
  const isDone = task.status === 'DONE'

  const handleDragStart = (e: React.DragEvent<HTMLDivElement>) => {
    e.dataTransfer.setData('application/json', JSON.stringify({ taskId: task.id, currentStatus: task.status }))
    e.dataTransfer.setData('text/plain', String(task.id))
    e.dataTransfer.effectAllowed = 'move'
  }

  const getPreviousStatus = (current: TaskStatus): TaskStatus | null => {
    if (current === 'DONE') return 'IN_PROGRESS'
    if (current === 'IN_PROGRESS') return 'TODO'
    return null
  }

  const getNextStatus = (current: TaskStatus): TaskStatus | null => {
    if (current === 'TODO') return 'IN_PROGRESS'
    if (current === 'IN_PROGRESS') return 'DONE'
    return null
  }

  const prevStatus = getPreviousStatus(task.status)
  const nextStatus = getNextStatus(task.status)

  // 3px left bar styling
  // In DONE column: 60% opacity and no colored left bar
  const cardStyle: React.CSSProperties = {
    backgroundColor: SH_COLORS.surface,
    borderRadius: SH_RADII.md,
    border: `1px solid ${SH_COLORS.border}`,
    borderLeft: isDone ? `1px solid ${SH_COLORS.border}` : `3px solid ${config.barColor}`,
    opacity: isDone ? 0.6 : 1,
    padding: '12px 14px',
    boxShadow: '0 1px 2px rgba(15,23,42,.05)',
    display: 'flex',
    flexDirection: 'column',
    gap: '8px',
    cursor: 'grab',
    transition: 'border-color 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease',
  }

  return (
    <div
      role="article"
      aria-label={`${config.label} task ${task.refCode || task.id}`}
      data-testid={`task-card-${task.id}`}
      data-task-type={task.type}
      data-task-status={task.status}
      style={cardStyle}
      draggable
      onDragStart={handleDragStart}
      tabIndex={0}
      onClick={() => onCardClick?.(task)}
      onKeyDown={(e) => {
        if ((e.target as HTMLElement).tagName === 'SELECT' || (e.target as HTMLElement).tagName === 'INPUT') {
          return
        }
        if (e.key === 'ArrowLeft' && prevStatus) {
          e.preventDefault()
          onStatusChange(task.id, prevStatus)
        } else if (e.key === 'ArrowRight' && nextStatus) {
          e.preventDefault()
          onStatusChange(task.id, nextStatus)
        }
      }}
    >
      {/* Top row: Type chip & Due date chip */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
        <span
          data-testid={`task-type-chip-${task.id}`}
          style={{
            backgroundColor: config.tintBg,
            color: config.textColor,
            border: `1px solid ${config.borderColor}`,
            fontSize: '11px',
            fontWeight: 600,
            padding: '2px 8px',
            borderRadius: SH_RADII.sm,
            lineHeight: 1.4,
            textTransform: 'uppercase',
            letterSpacing: '0.02em',
          }}
        >
          {config.label}
        </span>

        {task.workDate && (
          <span
            style={{
              fontSize: '11px',
              color: SH_COLORS.muted,
              fontVariantNumeric: 'tabular-nums',
              fontWeight: 500,
            }}
          >
            {task.workDate}
          </span>
        )}
      </div>

      {/* Main info: Unit code and Ref code */}
      <div style={{ display: 'flex', alignItems: 'baseline', gap: '8px' }}>
        {task.unitCode && (
          <span
            data-testid={`task-unit-${task.id}`}
            style={{
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
              fontSize: '14px',
              fontWeight: 700,
              color: SH_COLORS.ink,
              backgroundColor: SH_COLORS.surfaceSubtle,
              padding: '1px 6px',
              borderRadius: SH_RADII.sm,
              border: `1px solid ${SH_COLORS.border}`,
            }}
          >
            {task.unitCode}
          </span>
        )}

        {task.refCode && (
          <span
            data-testid={`task-ref-${task.id}`}
            style={{
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace',
              fontSize: '12px',
              color: SH_COLORS.muted,
              fontWeight: 500,
            }}
          >
            {task.refCode}
          </span>
        )}
      </div>

      {/* Customer Name or Description */}
      {(task.customerName || task.description || task.title) && (
        <div style={{ fontSize: '12px', color: SH_COLORS.inkSecondary, lineHeight: 1.4 }}>
          {task.customerName ? (
            <div style={{ fontWeight: 600 }}>{task.customerName}</div>
          ) : null}
          {task.description && (
            <div style={{ color: SH_COLORS.muted, fontSize: '11.5px', marginTop: '2px' }}>
              {task.description}
            </div>
          )}
        </div>
      )}

      {/* Accessible Movement Controls (UX-DR6/DR10) */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginTop: '6px',
          paddingTop: '6px',
          borderTop: `1px solid ${SH_COLORS.divider}`,
          gap: '6px',
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', gap: '4px' }}>
          {prevStatus && (
            <button
              type="button"
              data-testid={`move-left-${task.id}`}
              aria-label={`Move ${task.refCode || task.id} to ${prevStatus === 'TODO' ? 'To do' : 'In progress'}`}
              onClick={() => onStatusChange(task.id, prevStatus)}
              style={{
                fontSize: '11px',
                fontWeight: 600,
                color: SH_COLORS.inkSecondary,
                backgroundColor: SH_COLORS.surfaceSubtle,
                border: `1px solid ${SH_COLORS.border}`,
                borderRadius: SH_RADII.sm,
                padding: '3px 8px',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '2px',
              }}
            >
              ◀ {prevStatus === 'TODO' ? 'To do' : 'In prog'}
            </button>
          )}

          {nextStatus && (
            <button
              type="button"
              data-testid={`move-right-${task.id}`}
              aria-label={`Move ${task.refCode || task.id} to ${nextStatus === 'IN_PROGRESS' ? 'In progress' : 'Done'}`}
              onClick={() => onStatusChange(task.id, nextStatus)}
              style={{
                fontSize: '11px',
                fontWeight: 600,
                color: SH_COLORS.primary,
                backgroundColor: SH_COLORS.primaryTint,
                border: `1px solid ${SH_COLORS.primaryOutlineBorder}`,
                borderRadius: SH_RADII.sm,
                padding: '3px 8px',
                cursor: 'pointer',
                display: 'inline-flex',
                alignItems: 'center',
                gap: '2px',
              }}
            >
              {nextStatus === 'IN_PROGRESS' ? 'In prog' : 'Done'} ▶
            </button>
          )}
        </div>

        {/* Quick status dropdown for screen-readers & full keyboard accessibility */}
        <select
          data-testid={`status-select-${task.id}`}
          aria-label={`Status for task ${task.refCode || task.id}`}
          value={task.status}
          onChange={(e) => onStatusChange(task.id, e.target.value as TaskStatus)}
          style={{
            fontSize: '11px',
            color: SH_COLORS.muted,
            backgroundColor: 'transparent',
            border: 'none',
            cursor: 'pointer',
            padding: '2px 4px',
            outline: 'none',
          }}
        >
          <option value="TODO">To do</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="DONE">Done</option>
        </select>
      </div>
    </div>
  )
}
