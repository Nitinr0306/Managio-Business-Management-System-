'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { ArrowLeft, Mail, Loader2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { useInviteStaff } from '@/lib/hooks/useStaff'

const inviteSchema = z.object({
  email: z.string().email('Valid email required'),
  role: z.enum(['MANAGER', 'TRAINER', 'RECEPTIONIST', 'ACCOUNTANT', 'STAFF']),
  message: z.string().max(500).optional(),
  department: z.string().optional(),
  designation: z.string().optional(),
})
type InviteData = z.infer<typeof inviteSchema>

export default function InviteStaffPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const router = useRouter()
  const inviteMutation = useInviteStaff(businessId)

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<InviteData>({ resolver: zodResolver(inviteSchema) })

  const inp =
    'w-full bg-[hsl(var(--surface-1))] border border-[hsl(var(--border))] rounded-xl px-4 py-2.5 text-sm text-[hsl(var(--foreground))] placeholder:text-[hsl(var(--muted-foreground))] focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-[hsl(var(--muted-foreground))] mb-1.5'

  return (
    <div className="max-w-xl mx-auto">
      <div className="mb-8">
        <Link
          href={`/businesses/${businessId}/staff`}
          className="inline-flex items-center gap-2 text-sm text-[hsl(var(--muted-foreground))] hover:text-[hsl(var(--foreground))] transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to staff
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center">
            <Mail className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Invite Staff Member</h1>
            <p className="text-sm text-[hsl(var(--muted-foreground))]">Send an email invitation to join your team</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-[hsl(var(--border))] bg-[hsl(var(--card))]"
      >
        <form
          onSubmit={handleSubmit(async (d) => {
            try {
              await inviteMutation.mutateAsync(d)
              router.push(`/businesses/${businessId}/staff`)
            } catch {
              // error toast handled by useInviteStaff onError
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
          <div>
            <label className={lbl}>Personal Message (optional)</label>
            <textarea
              {...register('message')}
              placeholder="Welcome to the team!"
              rows={2}
              className={inp + ' resize-none'}
            />
          </div>
          <button
            type="submit"
            disabled={inviteMutation.isPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
          >
            {inviteMutation.isPending ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" /> Sending...
              </>
            ) : (
              <>
                <Mail className="w-4 h-4" /> Send Invitation
              </>
            )}
          </button>
        </form>
      </motion.div>
    </div>
  )
}