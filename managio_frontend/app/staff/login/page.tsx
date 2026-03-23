'use client'

import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, LogIn, Dumbbell, UserCog, AlertCircle } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  email: z.string().email('Valid email required'),
  password: z.string().min(1, 'Password required'),
  businessId: z.string()
    .min(1, 'Business ID required')
    .regex(/^([0-9]+|[0-9]{4}[A-Za-z]{4})$/, 'Use numeric ID or 4-digit+4-letter public ID (e.g. 1234ABCD)'),
})
type FormData = z.infer<typeof schema>

export default function StaffLoginPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { staffLogin, isStaffLoginPending } = useAuth()

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp =
    'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-violet-500/70 focus:bg-white/8 transition-all duration-200'

  return (
    <div className="min-h-screen bg-[#070710] flex items-center justify-center p-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 right-1/4 w-96 h-96 bg-violet-600/8 rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 left-1/4 w-72 h-72 bg-indigo-600/6 rounded-full blur-[100px]" />
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="w-full max-w-md relative z-10"
      >
        <Link href="/" className="flex items-center gap-2.5 mb-8 justify-center">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/30">
            <Dumbbell className="w-4 h-4 text-white" />
          </div>
          <span className="text-lg font-display font-700 text-white tracking-tight">Managio</span>
        </Link>

        <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
          <div className="flex items-center gap-3 mb-7">
            <div className="w-11 h-11 rounded-2xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center">
              <UserCog className="w-5.5 h-5.5 text-violet-400" />
            </div>
            <div>
              <h1 className="text-xl font-display font-700 text-white">Staff Portal</h1>
              <p className="text-xs text-white/45 mt-0.5">Sign in with your staff credentials</p>
            </div>
          </div>

          <form onSubmit={handleSubmit((d) => staffLogin(d))} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Business ID</label>
              <input
                {...register('businessId')}
                placeholder="Enter Business ID or Public ID (e.g. 1 or 1234ABCD)"
                className={inp}
              />
              {errors.businessId && (
                <p className="flex items-center gap-1 text-red-400 text-xs mt-1.5">
                  <AlertCircle className="w-3 h-3" />{errors.businessId.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">
                Email Address
              </label>
              <input
                {...register('email')}
                type="email"
                placeholder="you@company.com"
                autoComplete="email"
                className={inp}
              />
              {errors.email && (
                <p className="flex items-center gap-1 text-red-400 text-xs mt-1.5">
                  <AlertCircle className="w-3 h-3" />{errors.email.message}
                </p>
              )}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Password</label>
              <div className="relative">
                <input
                  {...register('password')}
                  type={showPwd ? 'text' : 'password'}
                  placeholder="••••••••"
                  autoComplete="current-password"
                  className={inp + ' pr-11'}
                />
                <button
                  type="button"
                  onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70 transition-colors"
                >
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="flex items-center gap-1 text-red-400 text-xs mt-1.5">
                  <AlertCircle className="w-3 h-3" />{errors.password.message}
                </p>
              )}
            </div>

            <button
              type="submit"
              disabled={isStaffLoginPending}
              className="w-full flex items-center justify-center gap-2 py-3 mt-1 bg-violet-600 hover:bg-violet-500 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-all duration-200 shadow-lg shadow-violet-600/30"
            >
              {isStaffLoginPending ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" /> Signing in...
                </>
              ) : (
                <>
                  <LogIn className="w-4 h-4" /> Staff Sign In
                </>
              )}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-white/6 flex items-center justify-center gap-5 text-xs text-white/35">
            <Link href="/login" className="hover:text-indigo-400 transition-colors">
              Owner Login
            </Link>
            <span className="text-white/15">•</span>
            <Link href="/member/login" className="hover:text-emerald-400 transition-colors">
              Member Login
            </Link>
            <span className="text-white/15">•</span>
            <Link href="/staff/forgot-password" className="hover:text-white/60 transition-colors">
              Forgot password
            </Link>
          </div>
        </div>
      </motion.div>
    </div>
  )
}