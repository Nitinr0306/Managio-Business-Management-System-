'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Loader2, UserPlus, Eye, EyeOff } from 'lucide-react'
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/lib/api/auth'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { getErrorMessage } from '@/lib/utils/errors'

const schema = z.object({
  businessId: z.coerce.number({ invalid_type_error: 'Business ID must be a number' }).int().positive('Business ID is required'),
  firstName:  z.string().min(1, 'First name required').max(100),
  lastName:   z.string().min(1, 'Last name required').max(100),
  email:      z.string().email('Valid email required').optional().or(z.literal('')),
  phone:      z.string().min(10, 'Valid phone number required').optional().or(z.literal('')),
  password:   z.string().min(6, 'Minimum 6 characters'),
  confirmPassword: z.string(),
})
.refine(d => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})
.refine(d => d.email || d.phone, {
  message: 'Either email or phone number is required',
  path: ['email'],
})
type FormData = z.infer<typeof schema>

export default function MemberRegisterPage() {
  const [showPwd, setShowPwd] = useState(false)
  const router = useRouter()

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({
    resolver: zodResolver(schema),
  })

  const mutation = useMutation({
    mutationFn: (d: FormData) =>
      authApi.memberRegister({
        businessId: d.businessId,
        firstName:  d.firstName,
        lastName:   d.lastName,
        email:      d.email  || undefined,
        phone:      d.phone  || undefined,
        password:   d.password,
      }),
    onSuccess: () => {
      toast.success('Account created! Please sign in.')
      router.push('/member/login')
    },
    onError: (err: unknown) =>
      toast.error(getErrorMessage(err, 'Registration failed. Please try again.')),
  })

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-emerald-500/70 transition-all'
  const lbl = 'block text-xs font-medium text-white/60 mb-1.5'
  const err = 'text-red-400 text-xs mt-1'

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-73px)] p-6">
      <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.5 }} className="w-full max-w-md">
        <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
          <div className="mb-6">
            <h1 className="text-2xl font-display font-700 text-white mb-1">Join as Member</h1>
            <p className="text-xs text-white/45">Ask your gym for the Business ID</p>
          </div>

          <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-4">
            <div>
              <label className={lbl}>Business ID <span className="text-red-400">*</span></label>
              <input {...register('businessId')} placeholder="e.g. 42" className={inp} />
              {errors.businessId && <p className={err}>{errors.businessId.message}</p>}
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className={lbl}>First Name <span className="text-red-400">*</span></label>
                <input {...register('firstName')} placeholder="Arjun" className={inp} />
                {errors.firstName && <p className={err}>{errors.firstName.message}</p>}
              </div>
              <div>
                <label className={lbl}>Last Name <span className="text-red-400">*</span></label>
                <input {...register('lastName')} placeholder="Sharma" className={inp} />
                {errors.lastName && <p className={err}>{errors.lastName.message}</p>}
              </div>
            </div>

            <div>
              <label className={lbl}>Email</label>
              <input {...register('email')} type="email" placeholder="arjun@example.com" className={inp} />
              {errors.email && <p className={err}>{errors.email.message}</p>}
            </div>

            <div>
              <label className={lbl}>Phone</label>
              <input {...register('phone')} placeholder="+91 98765 43210" className={inp} />
              {errors.phone && <p className={err}>{errors.phone.message}</p>}
            </div>

            <div>
              <label className={lbl}>Password <span className="text-red-400">*</span></label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPwd ? 'text' : 'password'}
                  placeholder="Minimum 6 characters"
                  className={inp + ' pr-11'}
                />
                <button type="button" onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70">
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className={err}>{errors.password.message}</p>}
            </div>

            <div>
              <label className={lbl}>Confirm Password <span className="text-red-400">*</span></label>
              <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inp} />
              {errors.confirmPassword && <p className={err}>{errors.confirmPassword.message}</p>}
            </div>

            <button type="submit" disabled={mutation.isPending}
              className="w-full flex items-center justify-center gap-2 py-3 mt-1 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/30">
              {mutation.isPending
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Creating account…</>
                : <><UserPlus className="w-4 h-4" /> Create Account</>
              }
            </button>
          </form>

          <div className="mt-5 pt-5 border-t border-white/6 text-center text-sm text-white/40">
            Already have an account?{' '}
            <Link href="/member/login" className="text-emerald-400 hover:text-emerald-300 font-medium transition-colors">
              Sign in
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  )
}