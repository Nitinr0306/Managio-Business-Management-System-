'use client'
import { motion } from 'framer-motion'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import Link from 'next/link'
import { Eye, EyeOff, Loader2, LogIn } from 'lucide-react'
import { useState } from 'react'
import { useAuth } from '@/lib/hooks/useAuth'

const schema = z.object({
  email:    z.string().email('Enter a valid email address'),
  password: z.string().min(1,'Password is required'),
})
type FormData = z.infer<typeof schema>

export default function LoginPage() {
  const [showPwd, setShowPwd] = useState(false)
  const { login, isLoginPending } = useAuth()
  const { register, handleSubmit, formState: { errors } } = useForm<FormData>({ resolver: zodResolver(schema) })

  const inp = 'w-full bg-white/5 border border-white/10 rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/70 focus:bg-white/8 transition-all'

  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.5 }} className="w-full max-w-md">
      <div className="glass rounded-2xl p-8 border border-white/8 shadow-2xl">
        <div className="mb-7">
          <h1 className="text-2xl font-display font-700 text-white mb-1">Welcome back</h1>
          <p className="text-sm text-white/45">Sign in to your Managio account</p>
        </div>

        <form onSubmit={handleSubmit((d) => login(d))} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-white/60 mb-1.5">Email</label>
            <input {...register('email')} type="email" placeholder="you@example.com" autoComplete="email" className={inp} />
            {errors.email && <p className="text-red-400 text-xs mt-1.5">{errors.email.message}</p>}
          </div>

          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="text-xs font-medium text-white/60">Password</label>
              <Link href="/forgot-password" className="text-xs text-indigo-400 hover:text-indigo-300 transition-colors">
                Forgot password?
              </Link>
            </div>
            <div className="relative">
              <input {...register('password')} type={showPwd?'text':'password'} placeholder="••••••••" autoComplete="current-password" className={inp + ' pr-11'} />
              <button type="button" onClick={() => setShowPwd(!showPwd)} className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70 transition-colors">
                {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
            {errors.password && <p className="text-red-400 text-xs mt-1.5">{errors.password.message}</p>}
          </div>

          <button
            type="submit"
            disabled={isLoginPending}
            className="w-full flex items-center justify-center gap-2 py-3 mt-1 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 disabled:cursor-not-allowed text-white font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/30"
          >
            {isLoginPending ? <><Loader2 className="w-4 h-4 animate-spin" /> Signing in...</> : <><LogIn className="w-4 h-4" /> Sign In</>}
          </button>
        </form>

        <div className="mt-6 pt-5 border-t border-white/6 text-center text-sm text-white/45">
          Don't have an account?{' '}
          <Link href="/register" className="text-indigo-400 hover:text-indigo-300 font-medium transition-colors">
            Create one free
          </Link>
        </div>

        <div className="mt-4 flex justify-center gap-6 text-xs text-white/25">
          <Link href="/staff/login" className="hover:text-white/50 transition-colors">Staff Login</Link>
          <Link href="/member/login" className="hover:text-white/50 transition-colors">Member Login</Link>
        </div>
      </div>
    </motion.div>
  )
}