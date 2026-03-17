'use client'
import { useState } from 'react'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, LogIn, Dumbbell, AlertCircle } from 'lucide-react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  identifier: z.string().min(1, 'Enter your phone number or email'),
  password:   z.string().min(1, 'Password is required'),
})
type FormData = z.infer<typeof schema>

export default function MemberLoginPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { memberLogin, isMemberLoginPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-emerald-500/70 transition-all'

  return (
    <div className="flex items-center justify-center min-h-[calc(100vh-73px)] p-6">
      <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.5 }} className="w-full max-w-md">
        <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
          <div className="flex items-center gap-3 mb-7">
            <div className="w-12 h-12 rounded-2xl bg-emerald-500/15 border border-emerald-500/20 flex items-center justify-center">
              <Dumbbell className="w-6 h-6 text-emerald-400" />
            </div>
            <div>
              <h1 className="text-xl font-display font-700 text-white">Member Login</h1>
              <p className="text-xs text-white/45 mt-0.5">Access your membership portal</p>
            </div>
          </div>

          <form onSubmit={handleSubmit(d => memberLogin(d))} className="space-y-4">
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Phone or Email</label>
              <input {...register('identifier')} placeholder="+91 98765 43210 or email" autoComplete="username" className={inp} />
              {errors.identifier && (
                <p className="flex items-center gap-1 text-red-400 text-xs mt-1.5"><AlertCircle className="w-3 h-3"/>{errors.identifier.message}</p>
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-white/60 mb-1.5">Password</label>
              <div className="relative">
                <input {...register('password')} type={showPwd ? 'text' : 'password'} placeholder="••••••••" autoComplete="current-password" className={inp + ' pr-11'} />
                <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70">
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {errors.password && (
                <p className="flex items-center gap-1 text-red-400 text-xs mt-1.5"><AlertCircle className="w-3 h-3"/>{errors.password.message}</p>
              )}
            </div>
            <button type="submit" disabled={isMemberLoginPending}
              className="w-full flex items-center justify-center gap-2 py-3 mt-1 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all shadow-lg shadow-emerald-600/30">
              {isMemberLoginPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Signing in…</> : <><LogIn className="w-4 h-4" /> Sign In</>}
            </button>
          </form>

          <div className="mt-6 pt-5 border-t border-white/6 flex flex-col gap-3">
            <div className="text-center text-sm text-white/40">
              New member?{' '}
              <Link href="/member/register" className="text-emerald-400 hover:text-emerald-300 font-medium transition-colors">
                Create account
              </Link>
            </div>
            <div className="text-center text-xs text-white/35">
              <Link href="/member/forgot-password" className="hover:text-white/60 transition-colors">
                Forgot password?
              </Link>
            </div>
            <div className="flex justify-center gap-5 text-xs text-white/25">
              <Link href="/login" className="hover:text-white/50 transition-colors">Owner Login</Link>
              <span>·</span>
              <Link href="/staff/login" className="hover:text-white/50 transition-colors">Staff Login</Link>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}