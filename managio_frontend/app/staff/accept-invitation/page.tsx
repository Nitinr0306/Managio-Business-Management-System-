'use client'
import { Suspense, useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSearchParams, useRouter } from 'next/navigation'
import { Eye, EyeOff, Loader2, CheckCircle, Dumbbell, AlertCircle } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import Link from 'next/link'
import { staffApi } from '@/lib/api/staff'
import { getErrorMessage } from '@/lib/utils/errors'

const schema = z.object({
  firstName:       z.string().min(1, 'First name required').max(100),
  lastName:        z.string().min(1, 'Last name required').max(100),
  password:        z.string()
    .min(8, 'Minimum 8 characters')
    .regex(/[A-Z]/, 'Must have an uppercase letter')
    .regex(/[0-9]/, 'Must have a number'),
  confirmPassword: z.string(),
}).refine(d => d.password === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})
type FormData = z.infer<typeof schema>

function AcceptForm() {
  const [showPwd, setShowPwd] = useState(false)
  const [accepted, setAccepted] = useState(false)
  const searchParams = useSearchParams()
  const token = searchParams.get('token') || ''
  const router = useRouter()

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const mutation = useMutation({
    mutationFn: ({ firstName, lastName, password }: Pick<FormData, 'firstName'|'lastName'|'password'>) =>
      staffApi.acceptInvitation({ token, firstName, lastName, password }),
    onSuccess: () => {
      setAccepted(true)
      toast.success('Account created! Redirecting to login…')
      setTimeout(() => router.replace('/staff/login'), 2000)
    },
    onError: (err: unknown) =>
      toast.error(getErrorMessage(err, 'Failed to accept invitation. The link may have expired.')),
  })

  if (accepted) {
    return (
      <div className="text-center py-8">
        <div className="w-16 h-16 rounded-full bg-emerald-500/15 flex items-center justify-center mx-auto mb-4">
          <CheckCircle className="w-8 h-8 text-emerald-400" />
        </div>
        <h2 className="text-xl font-display font-700 mb-2">Welcome to the team!</h2>
        <p className="text-white/40 text-sm">Your account is set up. Redirecting to login…</p>
      </div>
    )
  }

  if (!token) {
    return (
      <div className="text-center py-8">
        <AlertCircle className="w-10 h-10 text-red-400 mx-auto mb-3" />
        <h2 className="text-lg font-display font-700 text-white/80 mb-2">Invalid Invitation</h2>
        <p className="text-sm text-white/40 mb-5">
          This invitation link is missing or invalid.<br />
          Ask your manager to resend it.
        </p>
        <Link href="/staff/login" className="text-sm text-indigo-400 hover:text-indigo-300 transition-colors">
          Go to staff login →
        </Link>
      </div>
    )
  }

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-violet-500/70 transition-all'
  const lbl = 'block text-xs font-medium text-white/60 mb-1.5'
  const errCls = 'text-red-400 text-xs mt-1'

  return (
    <form onSubmit={handleSubmit(
    (d) => {
      mutation.mutate(d)
    }
  )} className="space-y-4">
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label className={lbl}>First Name <span className="text-red-400">*</span></label>
          <input {...register('firstName')} placeholder="First" className={inp} />
          {errors.firstName && <p className={errCls}>{errors.firstName.message}</p>}
        </div>
        <div>
          <label className={lbl}>Last Name <span className="text-red-400">*</span></label>
          <input {...register('lastName')} placeholder="Last" className={inp} />
          {errors.lastName && <p className={errCls}>{errors.lastName.message}</p>}
        </div>
      </div>
      <div>
        <label className={lbl}>Create Password <span className="text-red-400">*</span></label>
        <div className="relative">
          <input
            {...register('password')}
            type={showPwd ? 'text' : 'password'}
            placeholder="Min 8 chars, uppercase & number"
            className={inp + ' pr-11'}
          />
          <button type="button" onClick={() => setShowPwd(!showPwd)}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70">
            {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        {errors.password && <p className={errCls}>{errors.password.message}</p>}
      </div>
      <div>
        <label className={lbl}>Confirm Password <span className="text-red-400">*</span></label>
        <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inp} />
        {errors.confirmPassword && <p className={errCls}>{errors.confirmPassword.message}</p>}
      </div>
      <button type="submit" disabled={mutation.isPending}
        className="w-full flex items-center justify-center gap-2 py-3 bg-violet-600 hover:bg-violet-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-violet-600/30">
        {mutation.isPending
          ? <><Loader2 className="w-4 h-4 animate-spin" /> Setting up…</>
          : 'Accept & Join Team'
        }
      </button>
    </form>
  )
}

export default function AcceptInvitationPage() {
  return (
    <div className="min-h-screen bg-[#070710] flex items-center justify-center p-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-violet-600/8 rounded-full blur-[120px]" />
      </div>
      <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="w-full max-w-md relative z-10">
        <Link href="/" className="flex items-center gap-2.5 mb-8 justify-center">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg">
            <Dumbbell className="w-4 h-4 text-white" />
          </div>
          <span className="text-lg font-display font-700 text-white tracking-tight">Managio</span>
        </Link>

        <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
          <div className="mb-7">
            <h1 className="text-2xl font-display font-700 text-white mb-1">Accept Invitation</h1>
            <p className="text-sm text-white/40">Set up your staff account to get started</p>
          </div>
          <Suspense fallback={<div className="py-4 text-center text-white/30 text-sm">Loading…</div>}>
            <AcceptForm />
          </Suspense>
        </div>
      </motion.div>
    </div>
  )
}