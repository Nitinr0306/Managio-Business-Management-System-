'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, LogIn, ArrowRight, Shield } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  email:    z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})
type FormData = z.infer<typeof schema>

export default function LoginPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { login, isLoginPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  return (
    <div className="w-full max-w-md">
      {/* ── Card ──────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      >
        <div className="relative rounded-2xl border border-white/[0.06] bg-white/[0.02] backdrop-blur-xl shadow-2xl shadow-black/40 overflow-hidden">
          {/* Gradient border glow */}
          <div className="absolute inset-0 rounded-2xl bg-gradient-to-b from-white/[0.08] via-transparent to-transparent pointer-events-none" />
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-40 h-px bg-gradient-to-r from-transparent via-indigo-400/50 to-transparent" />

          <div className="relative p-8 lg:p-10">
            {/* Header */}
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.15, duration: 0.5 }}
              className="mb-8"
            >
              <div className="flex items-center gap-2 mb-4">
                <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-emerald-500/10 border border-emerald-500/20">
                  <Shield className="w-3 h-3 text-emerald-400" />
                  <span className="text-[10px] font-medium text-emerald-400 uppercase tracking-wider">Secure Login</span>
                </div>
              </div>
              <h1 className="text-2xl lg:text-3xl font-display font-800 text-white mb-2">
                Welcome back
              </h1>
              <p className="text-sm text-white/40 leading-relaxed">
                Sign in to your Managio account to manage your business.
              </p>
            </motion.div>

            {/* Form */}
            <motion.form
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.25, duration: 0.5 }}
              onSubmit={handleSubmit((data) => login(data))}
              className="space-y-5"
            >
              {/* Email */}
              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Email</label>
                <div className="relative group">
                  <input
                    {...register('email')}
                    type="email"
                    placeholder="you@example.com"
                    autoComplete="email"
                    className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] focus:ring-1 focus:ring-indigo-500/20 transition-all duration-300"
                  />
                  <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-indigo-500/0 via-indigo-500/0 to-violet-500/0 group-focus-within:from-indigo-500/5 group-focus-within:to-violet-500/5 pointer-events-none transition-all duration-500" />
                </div>
                {errors.email && (
                  <motion.p initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="text-red-400 text-xs flex items-center gap-1">
                    <span className="w-1 h-1 rounded-full bg-red-400" />
                    {errors.email.message}
                  </motion.p>
                )}
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-medium text-white/50 uppercase tracking-wider">Password</label>
                  <Link
                    href="/forgot-password"
                    className="text-xs text-indigo-400/80 hover:text-indigo-300 transition-colors font-medium"
                  >
                    Forgot?
                  </Link>
                </div>
                <div className="relative group">
                  <input
                    {...register('password')}
                    type={showPwd ? 'text' : 'password'}
                    placeholder="••••••••"
                    autoComplete="current-password"
                    className="w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 pr-12 text-sm text-white placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] focus:ring-1 focus:ring-indigo-500/20 transition-all duration-300"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPwd(!showPwd)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-white/25 hover:text-white/60 transition-colors"
                  >
                    {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                  <div className="absolute inset-0 rounded-xl bg-gradient-to-r from-indigo-500/0 via-indigo-500/0 to-violet-500/0 group-focus-within:from-indigo-500/5 group-focus-within:to-violet-500/5 pointer-events-none transition-all duration-500" />
                </div>
                {errors.password && (
                  <motion.p initial={{ opacity: 0, y: -4 }} animate={{ opacity: 1, y: 0 }} className="text-red-400 text-xs flex items-center gap-1">
                    <span className="w-1 h-1 rounded-full bg-red-400" />
                    {errors.password.message}
                  </motion.p>
                )}
              </div>

              {/* Submit */}
              <button
                type="submit"
                disabled={isLoginPending}
                className="relative w-full flex items-center justify-center gap-2.5 py-3.5 mt-2 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 hover:shadow-indigo-500/35 group overflow-hidden"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
                {isLoginPending ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Signing in...</>
                ) : (
                  <><LogIn className="w-4 h-4" /> Sign In <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-0.5 transition-transform" /></>
                )}
              </button>
            </motion.form>

            {/* Divider + Links */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.35, duration: 0.5 }}
            >
              <div className="mt-8 pt-6 border-t border-white/[0.04]">
                <p className="text-center text-sm text-white/35">
                  Don&apos;t have an account?{' '}
                  <Link href="/register" className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors">
                    Create one free
                  </Link>
                </p>
              </div>

              <div className="mt-5 flex items-center justify-center gap-1">
                <div className="h-px flex-1 bg-gradient-to-r from-transparent to-white/[0.04]" />
                <span className="text-[10px] text-white/15 uppercase tracking-wider px-3 font-medium">or login as</span>
                <div className="h-px flex-1 bg-gradient-to-l from-transparent to-white/[0.04]" />
              </div>

              <div className="mt-4 flex justify-center gap-3">
                <Link
                  href="/staff/login"
                  className="px-5 py-2 rounded-lg bg-white/[0.03] border border-white/[0.06] text-xs text-white/40 hover:text-white/70 hover:bg-white/[0.06] hover:border-white/[0.12] transition-all duration-300"
                >
                  Staff Portal
                </Link>
                <Link
                  href="/member/login"
                  className="px-5 py-2 rounded-lg bg-white/[0.03] border border-white/[0.06] text-xs text-white/40 hover:text-white/70 hover:bg-white/[0.06] hover:border-white/[0.12] transition-all duration-300"
                >
                  Member Portal
                </Link>
              </div>
            </motion.div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}