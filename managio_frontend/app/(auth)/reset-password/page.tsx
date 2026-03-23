'use client'
import { Suspense, useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useSearchParams, useRouter } from 'next/navigation'
import { Loader2, Lock, Eye, EyeOff, AlertCircle, ArrowRight, CheckCircle2, ArrowLeft } from 'lucide-react'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/lib/api/auth'
import { toast } from 'sonner'
import { getErrorMessage } from '@/lib/utils/errors'
import Link from 'next/link'

const schema = z.object({
  newPassword:     z.string().min(8, 'Min 8 characters').regex(/[A-Z]/, 'Uppercase required').regex(/[0-9]/, 'Number required'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, { message: 'Passwords do not match', path: ['confirmPassword'] })
type FormData = z.infer<typeof schema>

function ResetForm() {
  const [show, setShow] = useState(false)
  const [success, setSuccess] = useState(false)
  const searchParams = useSearchParams()
  const token = searchParams.get('token') || ''
  const subject = searchParams.get('subject')
  const router = useRouter()

  const mutation = useMutation({
    mutationFn: ({ newPassword }: { newPassword: string }) =>
      authApi.resetPassword({
        token,
        newPassword,
        subject: subject === 'member' ? 'member' : 'user',
      }),
    onSuccess: () => {
      setSuccess(true)
      toast.success('Password reset successfully!')
      setTimeout(() => router.push('/login'), 3000)
    },
    onError: (err: unknown) => toast.error(getErrorMessage(err, 'Reset failed. The link may have expired.')),
  })

  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inputClass = 'w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] focus:ring-1 focus:ring-indigo-500/20 transition-all duration-300'

  if (!token) {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="text-center py-4"
      >
        <div className="w-16 h-16 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto mb-5">
          <AlertCircle className="w-7 h-7 text-red-400" />
        </div>
        <h2 className="text-lg font-display font-700 text-white mb-2">Invalid Reset Link</h2>
        <p className="text-sm text-white/35 mb-6">
          This link is missing or invalid.<br />Please request a new password reset.
        </p>
        <Link
          href="/forgot-password"
          className="inline-flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-indigo-600 to-violet-600 text-white text-sm font-semibold rounded-xl hover:from-indigo-500 hover:to-violet-500 transition-all"
        >
          Request New Link <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </motion.div>
    )
  }

  if (success) {
    return (
      <motion.div
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="text-center py-4"
      >
        <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mx-auto mb-5">
          <CheckCircle2 className="w-7 h-7 text-emerald-400" />
        </div>
        <h2 className="text-xl font-display font-700 text-white mb-2">Password Reset! 🎉</h2>
        <p className="text-sm text-white/35 mb-2">Your password has been updated successfully.</p>
        <p className="text-xs text-white/20">Redirecting to login...</p>
      </motion.div>
    )
  }

  return (
    <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-5">
      <div className="space-y-1.5">
        <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">New Password</label>
        <div className="relative">
          <input {...register('newPassword')} type={show ? 'text' : 'password'} placeholder="Min 8 characters" className={inputClass + ' pr-12'} />
          <button type="button" onClick={() => setShow(!show)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-white/25 hover:text-white/60 transition-colors">
            {show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
          </button>
        </div>
        {errors.newPassword && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.newPassword.message}</p>}
      </div>

      <div className="space-y-1.5">
        <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Confirm Password</label>
        <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inputClass} />
        {errors.confirmPassword && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.confirmPassword.message}</p>}
      </div>

      <button
        type="submit"
        disabled={mutation.isPending}
        className="relative w-full flex items-center justify-center gap-2.5 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 group overflow-hidden"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
        {mutation.isPending ? (
          <><Loader2 className="w-4 h-4 animate-spin" /> Resetting...</>
        ) : (
          <>Reset Password <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" /></>
        )}
      </button>
    </form>
  )
}

export default function ResetPasswordPage() {
  return (
    <div className="w-full max-w-md">
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      >
        <div className="relative rounded-2xl border border-white/[0.06] bg-white/[0.02] backdrop-blur-xl shadow-2xl shadow-black/40 overflow-hidden">
          <div className="absolute inset-0 rounded-2xl bg-gradient-to-b from-white/[0.08] via-transparent to-transparent pointer-events-none" />
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-40 h-px bg-gradient-to-r from-transparent via-indigo-400/50 to-transparent" />

          <div className="relative p-8 lg:p-10">
            <div className="mb-7">
              <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500/20 to-violet-500/20 flex items-center justify-center mb-5 border border-indigo-500/10">
                <Lock className="w-6 h-6 text-indigo-400" />
              </div>
              <h1 className="text-2xl font-display font-800 text-white mb-1.5">Set new password</h1>
              <p className="text-sm text-white/35">Choose a strong password for your account.</p>
            </div>

            <Suspense fallback={<div className="text-white/20 text-sm text-center py-8">Loading...</div>}>
              <ResetForm />
            </Suspense>

            <div className="mt-8 text-center">
              <Link href="/login" className="inline-flex items-center gap-2 text-sm text-white/30 hover:text-white/60 transition-colors">
                <ArrowLeft className="w-4 h-4" /> Back to sign in
              </Link>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}