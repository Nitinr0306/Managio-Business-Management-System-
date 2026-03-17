'use client'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2, Building2 } from 'lucide-react'

const schema = z.object({
  name:        z.string().min(2, 'Business name must be at least 2 characters').max(200),
  type:        z.enum([
    'GYM','FITNESS_STUDIO','YOGA_STUDIO','DANCE_STUDIO',
    'MARTIAL_ARTS','SWIMMING_POOL','SALON','SPA','RESTAURANT','RETAIL','OTHER',
  ]).optional(),
  description: z.string().max(1000).optional(),
  address:     z.string().max(500).optional(),
  city:        z.string().max(100).optional(),
  state:       z.string().max(100).optional(),
  country:     z.string().max(100).optional(),
  phone:       z.string().max(20).optional(),
  email:       z.string().email('Valid email required').optional().or(z.literal('')),
})
export type BusinessFormData = z.infer<typeof schema>

const BUSINESS_TYPES = [
  { value: 'GYM',             label: 'Gym' },
  { value: 'FITNESS_STUDIO',  label: 'Fitness Studio' },
  { value: 'YOGA_STUDIO',     label: 'Yoga Studio' },
  { value: 'DANCE_STUDIO',    label: 'Dance Studio' },
  { value: 'MARTIAL_ARTS',    label: 'Martial Arts' },
  { value: 'SWIMMING_POOL',   label: 'Swimming Pool' },
  { value: 'SALON',           label: 'Salon' },
  { value: 'SPA',             label: 'Spa' },
  { value: 'RESTAURANT',      label: 'Restaurant' },
  { value: 'RETAIL',          label: 'Retail' },
  { value: 'OTHER',           label: 'Other' },
]

interface Props {
  defaultValues?: Partial<BusinessFormData>
  onSubmit: (data: BusinessFormData) => void | Promise<void>
  loading?: boolean
  submitLabel?: string
}

export function BusinessForm({ defaultValues, onSubmit, loading, submitLabel = 'Save Business' }: Props) {
  const { register, handleSubmit, formState: { errors } } = useForm<BusinessFormData>({
    resolver: zodResolver(schema),
    defaultValues,
  })

  const inp = 'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/55 mb-1.5'
  const err = 'text-red-400 text-xs mt-1'

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      {/* Core Info */}
      <div>
        <h3 className="text-xs font-semibold text-white/50 uppercase tracking-wider mb-4 pb-2 border-b border-white/5">
          Business Information
        </h3>
        <div className="grid md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className={lbl}>Business Name <span className="text-red-400">*</span></label>
            <input {...register('name')} placeholder="e.g. FitZone Gym, Rhythm Dance Academy" className={inp} />
            {errors.name && <p className={err}>{errors.name.message}</p>}
          </div>

          <div>
            <label className={lbl}>Business Type</label>
            <select {...register('type')} className={inp + ' cursor-pointer'}>
              <option value="">Select type…</option>
              {BUSINESS_TYPES.map(t => (
                <option key={t.value} value={t.value}>{t.label}</option>
              ))}
            </select>
          </div>

          <div>
            <label className={lbl}>Phone</label>
            <input {...register('phone')} placeholder="+91 98765 43210" className={inp} />
          </div>

          <div>
            <label className={lbl}>Business Email</label>
            <input {...register('email')} type="email" placeholder="info@yourbusiness.com" className={inp} />
            {errors.email && <p className={err}>{errors.email.message}</p>}
          </div>

          <div className="md:col-span-2">
            <label className={lbl}>Description</label>
            <textarea
              {...register('description')}
              rows={2}
              placeholder="Brief description of your business"
              className={inp + ' resize-none'}
            />
          </div>
        </div>
      </div>

      {/* Location */}
      <div>
        <h3 className="text-xs font-semibold text-white/50 uppercase tracking-wider mb-4 pb-2 border-b border-white/5">
          Location
        </h3>
        <div className="grid md:grid-cols-2 gap-4">
          <div className="md:col-span-2">
            <label className={lbl}>Address</label>
            <input {...register('address')} placeholder="Street address" className={inp} />
          </div>
          <div>
            <label className={lbl}>City</label>
            <input {...register('city')} placeholder="Mumbai, Delhi…" className={inp} />
          </div>
          <div>
            <label className={lbl}>State</label>
            <input {...register('state')} placeholder="Maharashtra" className={inp} />
          </div>
          <div>
            <label className={lbl}>Country</label>
            <input {...register('country')} placeholder="India" defaultValue="India" className={inp} />
          </div>
        </div>
      </div>

      <button
        type="submit"
        disabled={loading}
        className="flex items-center gap-2 px-6 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/20"
      >
        {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Building2 className="w-4 h-4" />}
        {loading ? 'Saving…' : submitLabel}
      </button>
    </form>
  )
}