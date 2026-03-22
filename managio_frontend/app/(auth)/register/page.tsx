'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, UserPlus, Check, ArrowRight, Zap, Users, BarChart3, Headphones } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  firstName:       z.string().min(1, 'Required').max(100),
  lastName:        z.string().min(1, 'Required').max(100),
  email:           z.string().email('Enter a valid email'),
  password:        z.string().min(8, 'Min 8 characters')
    .regex(/[A-Z]/, 'Must contain uppercase')
    .regex(/[a-z]/, 'Must contain lowercase')
    .regex(/[0-9]/, 'Must contain a number')
    .regex(/[!@#$%^&*()\-_=+{};:,<.>]/, 'Must contain a special character'),
  confirmPassword: z.string(),
}).refine(d => d.password === d.confirmPassword, { message: 'Passwords do not match', path: ['confirmPassword'] })
type FormData = z.infer<typeof schema>

const PERKS = [
  { icon: Zap, text: '14-day free trial', desc: 'No credit card needed' },
  { icon: Users, text: 'Unlimited members', desc: 'Scale without limits' },
  { icon: BarChart3, text: 'Real-time analytics', desc: 'Data-driven insights' },
  { icon: Headphones, text: 'Priority support', desc: '24/7 expert help' },
]

export default function RegisterPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { register: regAuth, isRegisterPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inputClass = 'w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-3.5 text-sm text-white placeholder-white/20 focus:outline-none focus:border-indigo-500/50 focus:bg-white/[0.06] focus:ring-1 focus:ring-indigo-500/20 transition-all duration-300'

  return (
    <div className="w-full max-w-5xl grid lg:grid-cols-5 gap-8 items-center">
      {/* ── Left — Value proposition ──────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, x: -30 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        className="hidden lg:flex lg:col-span-2 flex-col justify-center"
      >
        <div className="mb-8">
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 mb-6">
            <Zap className="w-3 h-3 text-indigo-400" />
            <span className="text-[10px] font-semibold text-indigo-400 uppercase tracking-wider">Free to start</span>
          </div>
          <h2 className="text-3xl lg:text-4xl font-display font-800 leading-tight mb-4">
            Start managing{' '}
            <span className="bg-gradient-to-r from-indigo-400 via-violet-400 to-fuchsia-400 bg-clip-text text-transparent">
              smarter today
            </span>
          </h2>
          <p className="text-white/35 text-sm leading-relaxed max-w-xs">
            Join 2,500+ business owners who&apos;ve streamlined their operations with Managio.
          </p>
        </div>

        <div className="space-y-3">
          {PERKS.map((perk, i) => (
            <motion.div
              key={perk.text}
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.3 + i * 0.1, duration: 0.5 }}
              className="flex items-center gap-4 p-3 rounded-xl bg-white/[0.02] border border-white/[0.04] hover:bg-white/[0.04] hover:border-white/[0.08] transition-all duration-300 group"
            >
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-indigo-500/20 to-violet-500/20 flex items-center justify-center flex-shrink-0 group-hover:from-indigo-500/30 group-hover:to-violet-500/30 transition-all">
                <perk.icon className="w-4 h-4 text-indigo-400" />
              </div>
              <div>
                <p className="text-sm font-medium text-white/80">{perk.text}</p>
                <p className="text-[11px] text-white/30">{perk.desc}</p>
              </div>
            </motion.div>
          ))}
        </div>
      </motion.div>

      {/* ── Right — Form card ────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 24, scale: 0.98 }}
        animate={{ opacity: 1, y: 0, scale: 1 }}
        transition={{ duration: 0.6, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
        className="lg:col-span-3"
      >
        <div className="relative rounded-2xl border border-white/[0.06] bg-white/[0.02] backdrop-blur-xl shadow-2xl shadow-black/40 overflow-hidden">
          <div className="absolute inset-0 rounded-2xl bg-gradient-to-b from-white/[0.08] via-transparent to-transparent pointer-events-none" />
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-40 h-px bg-gradient-to-r from-transparent via-violet-400/50 to-transparent" />

          <div className="relative p-8 lg:p-10">
            <div className="mb-7">
              <h1 className="text-2xl font-display font-800 text-white mb-1.5">Create account</h1>
              <p className="text-xs text-white/35">No credit card required · Get started in 60 seconds</p>
            </div>

            <form
              onSubmit={handleSubmit(({ firstName, lastName, email, password }) =>
                regAuth({ firstName, lastName, email, password })
              )}
              className="space-y-4"
            >
              <div className="grid grid-cols-2 gap-3">
                <div className="space-y-1.5">
                  <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">First Name</label>
                  <input {...register('firstName')} placeholder="Arjun" className={inputClass} />
                  {errors.firstName && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.firstName.message}</p>}
                </div>
                <div className="space-y-1.5">
                  <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Last Name</label>
                  <input {...register('lastName')} placeholder="Sharma" className={inputClass} />
                  {errors.lastName && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.lastName.message}</p>}
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Email</label>
                <input {...register('email')} type="email" placeholder="you@example.com" className={inputClass} />
                {errors.email && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.email.message}</p>}
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Password</label>
                <div className="relative">
                  <input {...register('password')} type={showPwd ? 'text' : 'password'} placeholder="Min 8 chars with upper, lower, digit & symbol" className={inputClass + ' pr-12'} />
                  <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3.5 top-1/2 -translate-y-1/2 text-white/25 hover:text-white/60 transition-colors">
                    {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {errors.password && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.password.message}</p>}
              </div>

              <div className="space-y-1.5">
                <label className="block text-xs font-medium text-white/50 uppercase tracking-wider">Confirm Password</label>
                <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inputClass} />
                {errors.confirmPassword && <p className="text-red-400 text-xs flex items-center gap-1"><span className="w-1 h-1 rounded-full bg-red-400" />{errors.confirmPassword.message}</p>}
              </div>

              <button
                type="submit"
                disabled={isRegisterPending}
                className="relative w-full flex items-center justify-center gap-2.5 py-3.5 mt-2 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 disabled:cursor-not-allowed text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 hover:shadow-indigo-500/35 group overflow-hidden"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
                {isRegisterPending ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Creating...</>
                ) : (
                  <><UserPlus className="w-4 h-4" /> Create Account <ArrowRight className="w-3.5 h-3.5 ml-1 group-hover:translate-x-0.5 transition-transform" /></>
                )}
              </button>
            </form>

            <div className="mt-6 pt-5 border-t border-white/[0.04] text-center text-sm text-white/35">
              Already have an account?{' '}
              <Link href="/login" className="text-indigo-400 hover:text-indigo-300 font-semibold transition-colors">Sign in</Link>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}