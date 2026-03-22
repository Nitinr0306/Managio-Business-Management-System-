'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, UserPlus, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { useCreateStaff } from '@/lib/hooks/useStaff'

const schema = z.object({
  email: z.string().email('Valid email required'),
  role: z.enum(['MANAGER', 'TRAINER', 'RECEPTIONIST', 'ACCOUNTANT', 'STAFF']),
  department: z.string().optional(),
  designation: z.string().optional(),
})
type FormData = z.infer<typeof schema>

export default function NewStaffPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const router = useRouter()
  const createMutation = useCreateStaff(businessId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp =
    'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'

  return (
    <div className="max-w-xl mx-auto">
      <div className="mb-8">
        <Link
          href={`/businesses/${businessId}/staff`}
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to staff
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center">
            <UserPlus className="w-5 h-5 text-violet-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Add Staff Member</h1>
            <p className="text-sm text-white/45">Add an existing user to your team</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]"
      >
        <form
          onSubmit={handleSubmit(async (d) => {
            try {
              await createMutation.mutateAsync({ email: d.email, role: d.role, department: d.department, designation: d.designation })
              router.push(`/businesses/${businessId}/staff`)
            } catch {
              // error toast handled by useCreateStaff onError
            }
          })}
          className="space-y-4"
        >
          <div>
            <label className={lbl}>Email Address *</label>
            <input
              {...register('email')}
              type="email"
              placeholder="staff@example.com"
              className={inp}
            />
            {errors.email && (
              <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>
            )}
          </div>
          <div>
            <label className={lbl}>Role *</label>
            <select {...register('role')} className={inp + ' cursor-pointer'}>
              {['MANAGER', 'TRAINER', 'RECEPTIONIST', 'ACCOUNTANT', 'STAFF'].map((r) => (
                <option key={r} value={r}>
                  {r}
                </option>
              ))}
            </select>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className={lbl}>Department</label>
              <input {...register('department')} placeholder="e.g. Training" className={inp} />
            </div>
            <div>
              <label className={lbl}>Designation</label>
              <input {...register('designation')} placeholder="e.g. Head Trainer" className={inp} />
            </div>
          </div>
          <button
            type="submit"
            disabled={createMutation.isPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
          >
            {createMutation.isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" /> Adding...
              </>
            ) : (
              <>
                <UserPlus className="w-4 h-4" /> Add Staff Member
              </>
            )}
          </button>
        </form>
      </motion.div>
    </div>
  )
}