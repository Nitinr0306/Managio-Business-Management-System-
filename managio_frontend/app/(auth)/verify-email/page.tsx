'use client'
import { Suspense, useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useSearchParams } from 'next/navigation'
import { CheckCircle, XCircle, Loader2 } from 'lucide-react'
import { authApi } from '@/lib/api/auth'
import Link from 'next/link'

function VerifyContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get('token')
  const [status, setStatus] = useState<'loading'|'success'|'error'>('loading')

  useEffect(() => {
    if (!token) { setStatus('error'); return }
    authApi.verifyEmail(token).then(() => setStatus('success')).catch(() => setStatus('error'))
  }, [token])

  return (
    <div className="text-center">
      {status === 'loading' && (
        <>
          <div className="w-16 h-16 rounded-full bg-indigo-500/15 flex items-center justify-center mx-auto mb-4">
            <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
          </div>
          <h2 className="text-xl font-display font-700 mb-2">Verifying your email…</h2>
          <p className="text-white/40 text-sm">Please wait a moment</p>
        </>
      )}
      {status === 'success' && (
        <>
          <div className="w-16 h-16 rounded-full bg-emerald-500/15 flex items-center justify-center mx-auto mb-4">
            <CheckCircle className="w-8 h-8 text-emerald-400" />
          </div>
          <h2 className="text-xl font-display font-700 mb-2">Email Verified!</h2>
          <p className="text-white/40 text-sm mb-6">Your account is now active. You can sign in.</p>
          <Link href="/login" className="inline-flex px-6 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all">
            Sign In
          </Link>
        </>
      )}
      {status === 'error' && (
        <>
          <div className="w-16 h-16 rounded-full bg-red-500/15 flex items-center justify-center mx-auto mb-4">
            <XCircle className="w-8 h-8 text-red-400" />
          </div>
          <h2 className="text-xl font-display font-700 mb-2">Verification Failed</h2>
          <p className="text-white/40 text-sm mb-6">Invalid or expired link. Try resending the verification email from your account.</p>
          <Link href="/login" className="inline-flex px-6 py-2.5 border border-white/10 text-white/70 hover:text-white text-sm font-medium rounded-xl transition-all">
            Back to Sign In
          </Link>
        </>
      )}
    </div>
  )
}

export default function VerifyEmailPage() {
  return (
    <motion.div initial={{ opacity:0, y:20 }} animate={{ opacity:1, y:0 }} className="w-full max-w-md">
      <div className="glass rounded-2xl p-10 border border-white/8">
        <Suspense fallback={<div className="text-white/30 text-sm text-center">Loading...</div>}>
          <VerifyContent />
        </Suspense>
      </div>
    </motion.div>
  )
}