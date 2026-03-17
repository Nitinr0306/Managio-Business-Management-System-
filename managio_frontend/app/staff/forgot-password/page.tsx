'use client'

import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { ArrowLeft, Loader2, Mail, UserCog } from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({ email: z.string().email('Enter a valid email address') })
type FormData = z.infer<typeof schema>

export default function StaffForgotPasswordPage() {
  const { forgotPassword, isForgotPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  return (
    <div className="min-h-screen bg-[#070710] flex items-center justify-center p-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-violet-600/8 rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 left-1/4 w-72 h-72 bg-indigo-600/6 rounded-full blur-[100px]" />
      </div>

      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.5 }} className="w-full max-w-md relative z-10">
        <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
          <div className="flex items-center gap-3 mb-7">
            <div className="w-11 h-11 rounded-2xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center">
              <UserCog className="w-5.5 h-5.5 text-violet-400" />
            </div>
            <div>
              <h1 className="text-xl font-display font-700 text-white">Reset staff password</h1>
              <p className="text-xs text-white/45 mt-0.5">Use the email registered for this business</p>
            </div>
          </div>

          <form onSubmit={handleSubmit((d) => forgotPassword(d.email))} className="space-y-5">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Email address</label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-white/30" />
                <input
                  {...register('email')}
                  type="email"
                  placeholder="you@company.com"
                  autoComplete="email"
                  autoFocus
                  className="w-full bg-white/5 border border-white/10 rounded-xl pl-10 pr-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-violet-500/70 transition-all"
                />
              </div>
              {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isForgotPending}
              className="w-full flex items-center justify-center gap-2 py-3 bg-violet-600 hover:bg-violet-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-violet-600/30"
            >
              {isForgotPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Sending…</> : 'Send reset link'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link href="/staff/login" className="inline-flex items-center gap-2 text-sm text-white/45 hover:text-white/70 transition-colors">
              <ArrowLeft className="w-4 h-4" /> Back to staff sign in
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  )
}

