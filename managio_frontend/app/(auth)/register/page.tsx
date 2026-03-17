'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, UserPlus, Check } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  firstName:       z.string().min(1,'Required').max(100),
  lastName:        z.string().min(1,'Required').max(100),
  email:           z.string().email('Enter a valid email'),
  password:        z.string().min(8,'Min 8 characters')
    .regex(/[A-Z]/,'Must contain uppercase')
    .regex(/[a-z]/,'Must contain lowercase')
    .regex(/[0-9]/,'Must contain a number')
    .regex(/[!@#$%^&*()\-_=+{};:,<.>]/,'Must contain a special character'),
  confirmPassword: z.string(),
}).refine(d => d.password === d.confirmPassword, { message:'Passwords do not match', path:['confirmPassword'] })
type FormData = z.infer<typeof schema>

const PERKS = ['14-day free trial','Unlimited member imports','Real-time analytics','Priority support']

export default function RegisterPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { register: regAuth, isRegisterPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/70 transition-all'

  return (
    <div className="w-full max-w-4xl grid md:grid-cols-2 gap-6 items-center">
      <motion.div initial={{ opacity:0, x:-20 }} animate={{ opacity:1, x:0 }} transition={{ duration:0.6 }} className="hidden md:flex flex-col justify-center p-8">
        <h2 className="text-3xl font-display font-800 mb-4">
          Start managing<br />
          <span className="gradient-text">smarter today</span>
        </h2>
        <p className="text-white/45 text-sm mb-7 leading-relaxed">
          Join 2,500+ business owners who've streamlined operations with Managio.
        </p>
        <ul className="space-y-3">
          {PERKS.map(p => (
            <li key={p} className="flex items-center gap-3 text-sm text-white/65">
              <div className="w-5 h-5 rounded-full bg-emerald-500/20 flex items-center justify-center flex-shrink-0">
                <Check className="w-3 h-3 text-emerald-400" />
              </div>
              {p}
            </li>
          ))}
        </ul>
      </motion.div>

      <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.5, delay:0.1 }}>
        <div className="glass rounded-2xl p-8 border border-white/8">
          <div className="mb-6">
            <h1 className="text-2xl font-display font-700 text-white mb-1">Create account</h1>
            <p className="text-xs text-white/45">No credit card required</p>
          </div>

          <form onSubmit={handleSubmit(({ firstName, lastName, email, password }) =>
            regAuth({ firstName, lastName, email, password })
          )} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">First Name</label>
                <input {...register('firstName')} placeholder="Arjun" className={inp} />
                {errors.firstName && <p className="text-red-400 text-xs mt-1">{errors.firstName.message}</p>}
              </div>
              <div>
                <label className="block text-xs font-medium text-white/60 mb-1.5">Last Name</label>
                <input {...register('lastName')} placeholder="Sharma" className={inp} />
                {errors.lastName && <p className="text-red-400 text-xs mt-1">{errors.lastName.message}</p>}
              </div>
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Email</label>
              <input {...register('email')} type="email" placeholder="you@example.com" className={inp} />
              {errors.email && <p className="text-red-400 text-xs mt-1">{errors.email.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Password</label>
              <div className="relative">
                <input {...register('password')} type={showPwd?'text':'password'} placeholder="Min 8 chars with upper, lower, digit & symbol" className={inp + ' pr-11'} />
                <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70">
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && <p className="text-red-400 text-xs mt-1">{errors.password.message}</p>}
            </div>

            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Confirm Password</label>
              <input {...register('confirmPassword')} type="password" placeholder="••••••••" className={inp} />
              {errors.confirmPassword && <p className="text-red-400 text-xs mt-1">{errors.confirmPassword.message}</p>}
            </div>

            <button
              type="submit"
              disabled={isRegisterPending}
              className="w-full flex items-center justify-center gap-2 py-3 mt-1 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/30"
            >
              {isRegisterPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Creating...</> : <><UserPlus className="w-4 h-4" /> Create Account</>}
            </button>
          </form>

          <div className="mt-5 pt-5 border-t border-white/6 text-center text-sm text-white/45">
            Already have an account?{' '}
            <Link href="/login" className="text-indigo-400 hover:text-indigo-300 font-medium">Sign in</Link>
          </div>
        </div>
      </motion.div>
    </div>
  )
}