import React, { useState } from 'react'
import type { TaskDto, TaskStatus } from '../../types/task'
import { TaskCard } from './TaskCard'
import { SH_COLORS, SH_RADII } from '../../tokens'

interface KanbanColumnProps {
  status: TaskStatus
  title: string
  tasks: TaskDto[]
  onStatusChange: (taskId: number, newStatus: TaskStatus) => void
  onCardClick?: (task: TaskDto) => void
}

export const KanbanColumn: React.FC<KanbanColumnProps> = ({
  status,
  title,
  tasks,
  onStatusChange,
  onCardClick,
}) => {
  const [isDragOver, setIsDragOver] = useState(false)

  const handleDragOver = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
    if (!isDragOver) setIsDragOver(true)
  }

  const handleDragLeave = (e: React.DragEvent<HTMLDivElement>) => {
    // Only clear if leaving the column itself
    if (e.currentTarget.contains(e.relatedTarget as Node)) return
    setIsDragOver(false)
  }

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault()
    setIsDragOver(false)

    try {
      const dataStr = e.dataTransfer.getData('application/json')
      if (dataStr) {
        const data = JSON.parse(dataStr)
        if (data.currentStatus === status) {
          return
        }
        if (data.taskId) {
          onStatusChange(data.taskId, status)
          return
        }
      }
      const plainId = e.dataTransfer.getData('text/plain')
      if (plainId) {
        const taskId = Number(plainId)
        if (!isNaN(taskId)) {
          if (tasks.some((t) => t.id === taskId)) {
            return
          }
          onStatusChange(taskId, status)
        }
      }
    } catch {
      // ignore
    }
  }

  return (
    <div
      data-testid={`kanban-column-${status.toLowerCase()}`}
      style={{
        flex: 1,
        minWidth: '280px',
        backgroundColor: isDragOver ? '#EEF2FF' : SH_COLORS.surfaceSubtle,
        borderRadius: SH_RADII.md,
        border: `1px solid ${isDragOver ? SH_COLORS.primary : SH_COLORS.border}`,
        padding: '16px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
        transition: 'background-color 0.15s ease, border-color 0.15s ease',
      }}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Column Header */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          paddingBottom: '8px',
          borderBottom: `1px solid ${SH_COLORS.divider}`,
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <h2
            style={{
              fontSize: '14px',
              fontWeight: 700,
              color: SH_COLORS.ink,
              margin: 0,
            }}
          >
            {title}
          </h2>
          <span
            data-testid={`column-count-${status.toLowerCase()}`}
            style={{
              fontSize: '11.5px',
              fontWeight: 600,
              color: SH_COLORS.muted,
              backgroundColor: SH_COLORS.surfaceMuted,
              padding: '2px 8px',
              borderRadius: SH_RADII.full,
              fontVariantNumeric: 'tabular-nums',
            }}
          >
            {tasks.length}
          </span>
        </div>
      </div>

      {/* Cards List or Empty State */}
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '10px',
          minHeight: '220px',
        }}
      >
        {tasks.length === 0 ? (
          <div
            data-testid={`empty-column-${status.toLowerCase()}`}
            style={{
              border: `1px dashed ${SH_COLORS.borderStrong}`,
              borderRadius: SH_RADII.md,
              backgroundColor: SH_COLORS.surface,
              padding: '28px 16px',
              textAlign: 'center',
              color: SH_COLORS.muted,
              fontSize: '13px',
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '4px',
              minHeight: '140px',
            }}
          >
            <span style={{ fontWeight: 600, color: SH_COLORS.inkSecondary }}>Nothing here</span>
            <span style={{ fontSize: '11.5px', color: SH_COLORS.faint }}>No tasks in this column</span>
          </div>
        ) : (
          tasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onStatusChange={onStatusChange}
              onCardClick={onCardClick}
            />
          ))
        )}
      </div>
    </div>
  )
}
