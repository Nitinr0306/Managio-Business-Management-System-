'use client'

import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowLeft, Loader2, Mail, Dumbbell } from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({ identifier: z.string().min(1, 'Enter your phone number or email') })
type FormData = z.infer<typeof schema>

export default function MemberForgotPasswordPage() {
  const { memberForgotPassword, isMemberForgotPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  return (
    <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }} className="w-full max-w-md mx-auto p-6">
      <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
        <div className="flex items-center gap-3 mb-7">
          <div className="w-12 h-12 rounded-2xl bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center">
            <Dumbbell className="w-6 h-6 text-emerald-400" />
          </div>
          <div>
            <h1 className="text-xl font-display font-700 text-white">Reset your password</h1>
            <p className="text-xs text-white/45 mt-0.5">We’ll send a reset link if your member account exists</p>
          </div>
        </div>

        <form onSubmit={handleSubmit((d) => memberForgotPassword(d.identifier))} className="space-y-5">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Phone or email</label>
            <div className="relative">
              <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
              <input
                {...register('identifier')}
                placeholder="+91 98765 43210 or email"
                autoComplete="username"
                autoFocus
                className="w-full bg-white/5 border border-white/10 rounded-xl pl-10 pr-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-emerald-500/70 transition-all"
              />
            </div>
            {errors.identifier && <p className="text-red-400 text-xs mt-1.5">{errors.identifier.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isMemberForgotPending}
            className="w-full flex items-center justify-center gap-2 py-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/30"
          >
            {isMemberForgotPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Sending…</> : 'Send reset link'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <Link href="/member/login" className="inline-flex items-center gap-2 text-sm text-white/45 hover:text-white/70 transition-colors">
            <ArrowLeft className="w-4 h-4" /> Back to member sign in
          </Link>
        </div>
      </div>
    </motion.div>
  )
}

