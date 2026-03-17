'use client'
import { Suspense, useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSearchParams, useRouter } from 'next/navigation'
import { Loader2, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/lib/api/auth'
import { toast } from 'sonner'

const schema = z.object({
  newPassword:     z.string().min(8,'Min 8 characters').regex(/[A-Z]/,'Uppercase required').regex(/[0-9]/,'Number required'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, { message:'Passwords do not match', path:['confirmPassword'] })
type FormData = z.infer<typeof schema>

function ResetForm() {
  const [show, setShow] = useState(false)
  const searchParams = useSearchParams()
  const token = searchParams.get('token') || ''
  const router = useRouter()

  const mutation = useMutation({
    mutationFn: ({ newPassword }: { newPassword: string }) =>
      authApi.resetPassword({ token, newPassword }),
    onSuccess: () => { toast.success('Password reset successfully! Please sign in.'); router.push('/login') },
    onError: (err: any) => toast.error(err?.response?.data?.message || 'Reset failed. The link may have expired.'),
  })

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/70 transition-all'

  if (!token) {
    return (
      <div className="text-center">
        <AlertCircle className="w-8 h-8 text-red-400 mx-auto mb-3" />
        <p className="text-white/50 text-sm">Invalid or missing reset token.<br />Please request a new password reset link.</p>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-4">
      <div>
        <label className="block text-xs font-medium text-white/60 mb-1.5">New Password</label>
        <div className="relative">
          <input {...register('newPassword')} type={show?'text':'password'} placeholder="Min 8 characters" className={inp + ' pr-11'} />
          <button type="button" onClick={() => setShow(!show)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40">
            {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        {errors.newPassword && <p className="text-red-400 text-xs mt-1">{errors.newPassword.message}</p>}
      </div>
      <div>
        <label className="block text-xs font-medium text-white/60 mb-1.5">Confirm Password</label>
        <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inp} />
        {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
      </div>
      <button
        type="submit"
        disabled={mutation.isPending}
        className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all"
      >
        {mutation.isPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Resetting...</> : 'Reset Password'}
      </button>
    </form>
  )
}

export default function ResetPasswordPage() {
  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="w-full max-w-md">
      <div className="glass rounded-2xl p-8 border border-white/8">
        <div className="mb-6">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/20 flex items-center justify-center mb-4">
            <Lock className="w-6 h-6 text-indigo-400" />
          </div>
          <h1 className="text-2xl font-display font-700 text-white mb-1">Set new password</h1>
          <p className="text-sm text-white/45">Choose a strong password for your account.</p>
        </div>
        <Suspense fallback={<div className="text-white/30 text-sm">Loading...</div>}>
          <ResetForm />
        </Suspense>
      </div>
    </motion.div>
  )
}