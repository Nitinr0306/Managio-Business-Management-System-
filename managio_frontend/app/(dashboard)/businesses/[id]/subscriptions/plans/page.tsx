'use client'

import { useParams } from 'next/navigation'
import { Calendar, Plus, Check, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { usePlans, useCreatePlan } from '@/lib/hooks/useSubscriptions'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, CardSkeleton } from '@/components/shared/EmptyState'
import { formatCurrency } from '@/lib/utils/formatters'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useState } from 'react'
import type { SubscriptionPlan } from '@/lib/types/subscription'

const schema = z.object({
  name: z.string().min(1, 'Plan name is required'),
  price: z.coerce.number().min(0, 'Price must be 0 or more'),
  durationDays: z.coerce.number().min(1, 'Duration must be at least 1 day'),
  description: z.string().optional(),
})
type PlanFormData = z.infer<typeof schema>

function PlanCard({ plan }: { plan: SubscriptionPlan }) {
  const durationLabel = plan.durationDays >= 365
    ? `${Math.round(plan.durationDays / 365)} year(s)`
    : plan.durationDays >= 30
    ? `${Math.round(plan.durationDays / 30)} month(s)`
    : plan.durationDays >= 7
    ? `${Math.round(plan.durationDays / 7)} week(s)`
    : `${plan.durationDays} day(s)`

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={{ y: -3 }}
      transition={{ type: 'spring', stiffness: 300, damping: 24 }}
      className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] hover:border-white/10 transition-colors"
    >
      <div className="flex items-start justify-between mb-3">
        <div className="flex-1 min-w-0">
          <h3 className="font-display font-600 text-white/90 truncate">{plan.name}</h3>
          {plan.description && (
            <p className="text-xs text-white/40 mt-0.5 line-clamp-2">{plan.description}</p>
          )}
        </div>
        <span className={`ml-2 flex-shrink-0 text-xs px-2 py-1 rounded-full ${plan.isActive ? 'bg-emerald-500/15 text-emerald-400' : 'bg-white/5 text-white/40'}`}>
          {plan.isActive ? 'Active' : 'Inactive'}
        </span>
      </div>
      <div className="flex items-end justify-between mt-4">
        <div>
          <div className="text-2xl font-display font-700 text-amber-400">{formatCurrency(plan.price)}</div>
          <div className="text-xs text-white/40 mt-0.5">per {durationLabel}</div>
        </div>
      </div>
    </motion.div>
  )
}

export default function PlansPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const { data: plans, isLoading } = usePlans(businessId)
  const createMutation = useCreatePlan(businessId)
  const [showForm, setShowForm] = useState(false)

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<PlanFormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      durationDays: 30,
    },
  })

  const inp = 'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'

  const onSubmit = async (data: PlanFormData) => {
    await createMutation.mutateAsync(data)
    reset()
    setShowForm(false)
  }

  return (
    <div>
      <PageHeader
        title="Subscription Plans"
        description={`${plans?.length ?? 0} plans created`}
        icon={Calendar}
        actions={
          <button
            onClick={() => setShowForm(!showForm)}
            className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
          >
            <Plus className="w-4 h-4" />
            {showForm ? 'Cancel' : 'New Plan'}
          </button>
        }
      />

      {/* Create form */}
      {showForm && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
          className="mb-6 p-6 rounded-2xl border border-indigo-500/20 bg-indigo-500/5"
        >
          <h3 className="text-sm font-display font-600 text-white/80 mb-4">Create New Plan</h3>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className={lbl}>Plan Name *</label>
                <input {...register('name')} placeholder="e.g. Monthly Basic" className={inp} />
                {errors.name && <p className="text-red-400 text-xs mt-1">{errors.name.message}</p>}
              </div>
              <div>
                <label className={lbl}>Price (₹) *</label>
                <input {...register('price')} type="number" min={0} placeholder="999" className={inp} />
                {errors.price && <p className="text-red-400 text-xs mt-1">{errors.price.message}</p>}
              </div>
            </div>

            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className={lbl}>Duration (days) *</label>
                <input
                  {...register('durationDays')}
                  type="number"
                  min={1}
                  placeholder="30"
                  className={inp}
                />
                <p className="text-white/30 text-xs mt-1">Common: 30 (monthly), 90 (quarterly), 365 (yearly)</p>
                {errors.durationDays && <p className="text-red-400 text-xs mt-1">{errors.durationDays.message}</p>}
              </div>
              <div>
                <label className={lbl}>Description</label>
                <input {...register('description')} placeholder="Brief plan description" className={inp} />
              </div>
            </div>

            <div>
              <button
                type="submit"
                disabled={createMutation.isPending}
                className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white text-sm font-medium rounded-xl transition-all"
              >
                {createMutation.isPending
                  ? <Loader2 className="w-4 h-4 animate-spin" />
                  : <Check className="w-4 h-4" />
                }
                Create Plan
              </button>
            </div>
          </form>
        </motion.div>
      )}

      {isLoading ? (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 3 }).map((_, i) => <CardSkeleton key={i} />)}
        </div>
      ) : plans?.length === 0 ? (
        <EmptyState
          icon={Calendar}
          title="No plans yet"
          description="Create subscription plans to assign to your members"
        />
      ) : (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-4">
          {plans?.map(plan => <PlanCard key={plan.id} plan={plan} />)}
        </div>
      )}
    </div>
  )
}