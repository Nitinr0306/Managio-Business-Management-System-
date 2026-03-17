'use client'

import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2, UserPlus } from 'lucide-react'
import type { Member } from '@/lib/types/member'

const schema = z.object({
  firstName: z.string().min(1, 'First name is required').max(100),
  lastName: z.string().min(1, 'Last name is required').max(100),
  email: z.string().email('Valid email required').optional().or(z.literal('')),
  phone: z.string().optional(),
  address: z.string().optional(),
  dateOfBirth: z.string().optional(),
  gender: z.enum(['MALE', 'FEMALE', 'OTHER']).optional(),
  emergencyContactName: z.string().optional(),
  emergencyContactPhone: z.string().optional(),
  notes: z.string().optional(),
})
export type MemberFormData = z.infer<typeof schema>

interface MemberFormProps {
  defaultValues?: Partial<MemberFormData>
  onSubmit: (data: MemberFormData) => void
  loading?: boolean
  submitLabel?: string
}

export function MemberForm({ defaultValues, onSubmit, loading, submitLabel = 'Save Member' }: MemberFormProps) {
  const { register, handleSubmit, formState: { errors } } = useForm<MemberFormData>({
    resolver: zodResolver(schema),
    defaultValues,
  })

  const inp = 'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'
  const err = 'text-red-400 text-xs mt-1'

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Basic Info */}
      <div>
        <h3 className="text-sm font-display font-600 text-white/70 mb-4 pb-2 border-b border-white/5">Basic Information</h3>
        <div className="grid md:grid-cols-2 gap-4">
          <div>
            <label className={lbl}>First Name *</label>
            <input {...register('firstName')} placeholder="Arjun" className={inp} />
            {errors.firstName && <p className={err}>{errors.firstName.message}</p>}
          </div>
          <div>
            <label className={lbl}>Last Name *</label>
            <input {...register('lastName')} placeholder="Sharma" className={inp} />
            {errors.lastName && <p className={err}>{errors.lastName.message}</p>}
          </div>
          <div>
            <label className={lbl}>Gender</label>
            <select {...register('gender')} className={inp + ' cursor-pointer'}>
              <option value="">Select gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>
          <div>
            <label className={lbl}>Email</label>
            <input {...register('email')} type="email" placeholder="member@example.com" className={inp} />
            {errors.email && <p className={err}>{errors.email.message}</p>}
          </div>
          <div>
            <label className={lbl}>Phone</label>
            <input {...register('phone')} placeholder="+91 98765 43210" className={inp} />
          </div>
          <div>
            <label className={lbl}>Date of Birth</label>
            <input {...register('dateOfBirth')} type="date" className={inp} />
          </div>
          <div className="md:col-span-2">
            <label className={lbl}>Address</label>
            <input {...register('address')} placeholder="Street address" className={inp} />
          </div>
        </div>
      </div>

      {/* Emergency Contact */}
      <div>
        <h3 className="text-sm font-display font-600 text-white/70 mb-4 pb-2 border-b border-white/5">Emergency Contact</h3>
        <div className="grid md:grid-cols-2 gap-4">
          <div>
            <label className={lbl}>Contact Name</label>
            <input {...register('emergencyContactName')} placeholder="Emergency contact name" className={inp} />
          </div>
          <div>
            <label className={lbl}>Contact Phone</label>
            <input {...register('emergencyContactPhone')} placeholder="+91 98765 43210" className={inp} />
          </div>
        </div>
      </div>

      {/* Notes */}
      <div>
        <label className={lbl}>Notes</label>
        <textarea {...register('notes')} placeholder="Any special notes about this member..." rows={3} className={inp + ' resize-none'} />
      </div>

      <button
        type="submit"
        disabled={loading}
        className="flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
      >
        {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <UserPlus className="w-4 h-4" />}
        {loading ? 'Saving...' : submitLabel}
      </button>
    </form>
  )
}