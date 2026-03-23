'use client'

import { useState } from 'react'
import { CheckSquare, RefreshCw } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, LoadingSpinner } from '@/components/shared/EmptyState'
import { useTasks, useUpdateTaskStatus } from '@/lib/hooks/useTasks'
import type { TaskStatus } from '@/lib/types/task'
import { cn } from '@/lib/utils/cn'

const STATUSES: TaskStatus[] = ['PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']

export default function StaffTasksPage() {
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const [status, setStatus] = useState<TaskStatus | undefined>()

  const { data, isLoading, refetch } = useTasks(businessId, {
    page: 0,
    size: 50,
    status,
    assignedToMe: true,
  })
  const updateStatus = useUpdateTaskStatus(businessId)

  const tasks = data?.content ?? []

  return (
    <div>
      <PageHeader title="My Tasks" description="Tasks assigned to you" icon={CheckSquare} />

      <div className="flex items-center gap-2 mb-4">
        <button
          onClick={() => setStatus(undefined)}
          className={cn('px-3 py-1.5 rounded-xl border text-xs', !status ? 'bg-emerald-600 border-emerald-600 text-white' : 'border-white/8 text-white/50')}
        >
          All
        </button>
        {STATUSES.map((s) => (
          <button
            key={s}
            onClick={() => setStatus(s)}
            className={cn('px-3 py-1.5 rounded-xl border text-xs', status === s ? 'bg-emerald-600 border-emerald-600 text-white' : 'border-white/8 text-white/50')}
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
        <EmptyState icon={CheckSquare} title="No tasks assigned" description="You're all caught up." />
      ) : (
        <div className="space-y-2">
          {tasks.map((task) => (
            <div key={task.publicId} className="p-4 rounded-2xl border border-white/6 bg-white/[0.02]">
              <div className="flex items-center justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-2 flex-wrap">
                    <p className="text-sm font-medium text-white/85">{task.title}</p>
                    <span className="text-[10px] px-2 py-1 rounded-full bg-white/5 text-white/45">{task.status}</span>
                    <span className="text-[10px] px-2 py-1 rounded-full bg-emerald-500/10 text-emerald-300/85 font-medium">{task.publicId}</span>
                  </div>
                  {task.description && <p className="text-xs text-white/45 mt-1.5">{task.description}</p>}
                  {task.dueDate && <p className="text-[11px] text-white/30 mt-1.5">Due {task.dueDate}</p>}
                </div>
                <select
                  value={task.status}
                  onChange={(e) => updateStatus.mutate({ taskId: task.publicId, status: e.target.value as TaskStatus })}
                  className="bg-white/4 border border-white/8 rounded-lg px-2 py-1.5 text-xs"
                >
                  {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                </select>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
