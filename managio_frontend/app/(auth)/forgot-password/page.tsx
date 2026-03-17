'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Loader2, Mail, ArrowLeft } from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({ email: z.string().email('Enter a valid email address') })
type FormData = z.infer<typeof schema>

export default function ForgotPasswordPage() {
  const { forgotPassword, isForgotPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.5 }} className="w-full max-w-md">
      <div className="glass rounded-2xl p-8 border border-white/8">
        <div className="mb-7">
          <div className="w-12 h-12 rounded-2xl bg-indigo-500/20 flex items-center justify-center mb-4">
            <Mail className="w-6 h-6 text-indigo-400" />
          </div>
          <h1 className="text-2xl font-display font-700 text-white mb-2">Forgot password?</h1>
          <p className="text-sm text-white/45 leading-relaxed">
            No worries — enter your email and we'll send you a reset link if the account exists.
          </p>
        </div>

        <form onSubmit={handleSubmit(d => forgotPassword(d.email))} className="space-y-5">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Email address</label>
            <input
              {...register('email')}
              type="email"
              placeholder="you@example.com"
              autoFocus
              className="w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/70 transition-all"
            />
            {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isForgotPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/30"
          >
            {isForgotPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Sending...</> : 'Send Reset Link'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <Link href="/login" className="inline-flex items-center gap-2 text-sm text-white/45 hover:text-white/70 transition-colors">
            <ArrowLeft className="w-4 h-4" /> Back to sign in
          </Link>
        </div>
      </div>
    </motion.div>
  )
}