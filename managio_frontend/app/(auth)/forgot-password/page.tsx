'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Loader2, Mail, ArrowLeft, ArrowRight, CheckCircle2 } from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'
import { useState } from 'react'

const schema = z.object({ email: z.string().email('Enter a valid email address') })
type FormData = z.infer<typeof schema>

export default function ForgotPasswordPage() {
  const { forgotPassword, isForgotPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })
  const [submitted, setSubmitted] = useState(false)
  const [submittedEmail, setSubmittedEmail] = useState('')

  const onSubmit = async (data: FormData) => {
    setSubmittedEmail(data.email)
    try {
      await forgotPassword(data.email)
      setSubmitted(true)
    } catch {
      // Network errors are handled in the hook with toast
      // Non-network errors also show success toast (email enumeration protection)
      // Still show success state to prevent enumeration
      setSubmitted(true)
    }
  }

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
            {!submitted ? (
              <>
                {/* Header */}
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.15, duration: 0.5 }}
                  className="mb-8"
                >
                  <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-indigo-500/20 to-violet-500/20 flex items-center justify-center mb-5 border border-indigo-500/10">
                    <Mail className="w-6 h-6 text-indigo-400" />
                  </div>
                  <h1 className="text-2xl font-display font-800 text-white mb-2">Forgot password?</h1>
                  <p className="text-sm text-white/35 leading-relaxed">
                    No worries — enter your email and we&apos;ll send you a reset link if the account exists.
                  </p>
                </motion.div>

                {/* Form */}
                <motion.form
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  transition={{ delay: 0.25, duration: 0.5 }}
                  onSubmit={handleSubmit(onSubmit)}
                  className="space-y-5"
                >
                  <div className="space-y-1.5">
                    <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Email address</label>
                    <input
                      {...register('email')}
                      type="email"
                      placeholder="you@example.com"
                      autoFocus
                      className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] focus:ring-1 focus:ring-indigo-500/20 transition-all duration-300"
                    />
                    {errors.email && (
                      <p className="text-red-400 text-xs flex items-center gap-1">
                        <span className="w-1 h-1 rounded-full bg-red-400" />
                        {errors.email.message}
                      </p>
                    )}
                  </div>

                  <button
                    type="submit"
                    disabled={isForgotPending}
                    className="relative w-full flex items-center justify-center gap-2.5 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 group overflow-hidden"
                  >
                    <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
                    {isForgotPending ? (
                      <><Loader2 className="w-4 h-4 animate-spin" /> Sending...</>
                    ) : (
                      <>Send Reset Link <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-0.5 transition-transform" /></>
                    )}
                  </button>
                </motion.form>
              </>
            ) : (
              /* ── Success state ─── */
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                transition={{ duration: 0.5 }}
                className="text-center py-4"
              >
                <div className="w-16 h-16 rounded-full bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mx-auto mb-5">
                  <CheckCircle2 className="w-7 h-7 text-emerald-400" />
                </div>
                <h2 className="text-xl font-display font-700 text-white mb-2">Check your email</h2>
                <p className="text-sm text-white/35 mb-2 leading-relaxed">
                  If an account exists for <span className="text-white/60 font-medium">{submittedEmail}</span>, we&apos;ve sent a password reset link.
                </p>
                <p className="text-xs text-white/20 mb-6">The link will expire in 1 hour.</p>
                <button
                  onClick={() => setSubmitted(false)}
                  className="text-sm text-indigo-400 hover:text-indigo-300 font-medium transition-colors"
                >
                  Try a different email
                </button>
              </motion.div>
            )}

            {/* Footer */}
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