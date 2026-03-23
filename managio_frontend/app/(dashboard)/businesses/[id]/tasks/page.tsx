'use client'

import { useMemo, useState } from 'react'
import { useParams } from 'next/navigation'
import { CheckSquare, Plus, RefreshCw, Trash2 } from 'lucide-react'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, LoadingSpinner } from '@/components/shared/EmptyState'
import { useCreateTask, useDeleteTask, useTasks, useUpdateTaskStatus } from '@/lib/hooks/useTasks'
import { useStaff } from '@/lib/hooks/useStaff'
import { cn } from '@/lib/utils/cn'
import type { TaskPriority, TaskStatus } from '@/lib/types/task'

const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
const STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']

const PRIORITY_STYLE: Record<TaskPriority, string> = {
  LOW: 'bg-white/5 text-white/50',
  MEDIUM: 'bg-cyan-500/15 text-cyan-300',
  HIGH: 'bg-amber-500/15 text-amber-300',
  URGENT: 'bg-red-500/15 text-red-300',
}

export default function BusinessTasksPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const [status, setStatus] = useState<TaskStatus | undefined>()
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [priority, setPriority] = useState<TaskPriority>('MEDIUM')
  const [dueDate, setDueDate] = useState('')
  const [assignedStaffId, setAssignedStaffId] = useState('')

  const { data, isLoading, refetch } = useTasks(businessId, { page: 0, size: 50, status })
  const { data: staff } = useStaff(businessId, { page: 0, size: 100 })

  const createTask = useCreateTask(businessId)
  const updateStatus = useUpdateTaskStatus(businessId)
  const deleteTask = useDeleteTask(businessId)

  const tasks = data?.content ?? []
  const staffOptions = useMemo(() => staff?.content ?? [], [staff?.content])

  return (
    <div>
      <PageHeader
        title="Tasks"
        description="Assign and track work across your staff"
        icon={CheckSquare}
      />

      <div className="p-4 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))] mb-5">
        <div className="grid md:grid-cols-5 gap-3">
          <input
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Task title"
            className="md:col-span-2 bg-white/4 border border-white/8 rounded-xl px-3 py-2.5 text-sm"
          />
          <select
            value={assignedStaffId}
            onChange={(e) => setAssignedStaffId(e.target.value)}
            className="bg-white/4 border border-white/8 rounded-xl px-3 py-2.5 text-sm"
          >
            <option value="">Unassigned</option>
            {staffOptions.map((s) => (
              <option key={s.id} value={s.publicId || s.id}>{s.userName}</option>
            ))}
          </select>
          <select
            value={priority}
            onChange={(e) => setPriority(e.target.value as TaskPriority)}
            className="bg-white/4 border border-white/8 rounded-xl px-3 py-2.5 text-sm"
          >
            {PRIORITIES.map((p) => <option key={p} value={p}>{p}</option>)}
          </select>
          <input
            value={dueDate}
            onChange={(e) => setDueDate(e.target.value)}
            type="date"
            className="bg-white/4 border border-white/8 rounded-xl px-3 py-2.5 text-sm"
          />
        </div>
        <div className="flex gap-3 mt-3">
          <input
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Description (optional)"
            className="flex-1 bg-white/4 border border-white/8 rounded-xl px-3 py-2.5 text-sm"
          />
          <button
            onClick={() => {
              if (!title.trim()) return
              createTask.mutate({
                title: title.trim(),
                description: description || undefined,
                priority,
                dueDate: dueDate || undefined,
                assignedStaffId: assignedStaffId || undefined,
              })
              setTitle('')
              setDescription('')
              setDueDate('')
              setAssignedStaffId('')
            }}
            className="inline-flex items-center gap-2 px-4 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-sm font-medium"
          >
            <Plus className="w-4 h-4" />
            Add Task
          </button>
        </div>
      </div>

      <div className="flex items-center gap-2 mb-4">
        <button
          onClick={() => setStatus(undefined)}
          className={cn('px-3 py-1.5 rounded-xl border text-xs', !status ? 'bg-indigo-600 border-indigo-600' : 'border-white/8 text-white/50')}
        >
          All
        </button>
        {STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setStatus(s)}
            className={cn('px-3 py-1.5 rounded-xl border text-xs', status === s ? 'bg-indigo-600 border-indigo-600' : 'border-white/8 text-white/50')}
          >
            {s}
          </button>
        ))}
        <button onClick={() => refetch()} className="ml-auto w-8 h-8 rounded-lg border border-white/8 flex items-center justify-center text-white/50 hover:text-white/80">
          <RefreshCw className="w-3.5 h-3.5" />
        </button>
      </div>

      {isLoading ? (
        <LoadingSpinner />
      ) : tasks.length === 0 ? (
        <EmptyState icon={CheckSquare} title="No tasks" description="Create a task to start assigning work" />
      ) : (
        <div className="space-y-2">
          {tasks.map((task) => (
            <div key={task.publicId} className="p-4 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))] flex items-start gap-3">
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <p className="text-sm font-medium text-white/85">{task.title}</p>
                  <span className={cn('text-[10px] px-2 py-1 rounded-full font-medium', PRIORITY_STYLE[task.priority])}>{task.priority}</span>
                  <span className="text-[10px] px-2 py-1 rounded-full bg-white/5 text-white/45">{task.status}</span>
                  <span className="text-[10px] px-2 py-1 rounded-full bg-indigo-500/10 text-indigo-300/85 font-medium">{task.publicId}</span>
                </div>
                {task.description && <p className="text-xs text-white/45 mt-1.5">{task.description}</p>}
                <p className="text-[11px] text-white/30 mt-1.5">
                  {task.assignedStaffName ? `Assigned: ${task.assignedStaffName}` : 'Unassigned'}
                  {task.dueDate ? ` • Due ${task.dueDate}` : ''}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <select
                  value={task.status}
                  onChange={(e) => updateStatus.mutate({ taskId: task.publicId, status: e.target.value as TaskStatus })}
                  className="bg-white/4 border border-white/8 rounded-lg px-2 py-1.5 text-xs"
                >
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
                <button
                  onClick={() => deleteTask.mutate(task.publicId)}
                  className="w-8 h-8 rounded-lg border border-red-500/20 text-red-300 hover:bg-red-500/10 flex items-center justify-center"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
