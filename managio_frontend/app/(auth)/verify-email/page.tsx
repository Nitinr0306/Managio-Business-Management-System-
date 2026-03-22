'use client'

import { Suspense, useEffect, useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useSearchParams, useRouter } from 'next/navigation'
import { CheckCircle2, XCircle, Loader2, Mail, ArrowLeft, ArrowRight, RefreshCw } from 'lucide-react'
import { authApi } from '@/lib/api/auth'
import Link from 'next/link'
import { useAuth } from '@/lib/hooks/useAuth'
import { getErrorCode } from '@/lib/utils/errors'

function VerifyContent() {
  const searchParams = useSearchParams()
  const router = useRouter()

  const token = searchParams.get('token')
  const emailFromUrl = searchParams.get('email')
  const [storedEmail, setStoredEmail] = useState<string | null>(null)

  useEffect(() => {
    setStoredEmail(localStorage.getItem('pending_verification_email'))
  }, [])

  const email = emailFromUrl || storedEmail

  const { resendVerification, isResendPending } = useAuth()
  const [resent, setResent] = useState(false)

  const [status, setStatus] = useState<'loading' | 'success' | 'error' | 'no-token'>('loading')
  const [errorType, setErrorType] = useState<'expired' | 'outdated' | 'invalid' | null>(null)

  useEffect(() => {
    if (!token) {
      setStatus('no-token')
      return
    }

    authApi
      .verifyEmail(token)
      .then(() => {
        setStatus('success')
        localStorage.removeItem('pending_verification_email')
        setTimeout(() => router.replace('/login'), 3000)
      })
      .catch((err) => {
        const errorCode = getErrorCode(err)
        const msg = err?.response?.data?.message?.toLowerCase() || ''

        if (errorCode === 'AUTH_006' || msg.includes('expired')) {
          setErrorType('expired')
        } else if (errorCode === 'AUTH_007' || msg.includes('used')) {
          setErrorType('outdated')
        } else {
          setErrorType('invalid')
        }

        setStatus('error')
      })
  }, [token])

  const handleResend = () => {
    if (!email) return
    resendVerification(email)
    setResent(true)
    setTimeout(() => setResent(false), 30000)
  }

  return (
    <AnimatePresence mode="wait">
      {/* ───────── LOADING ───────── */}
      {status === 'loading' && (
        <motion.div
          key="loading"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="text-center py-6"
        >
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 rounded-full bg-indigo-500/10 animate-ping" />
            <div className="relative w-20 h-20 rounded-full bg-gradient-to-br from-indigo-500/15 to-violet-500/15 border border-indigo-500/20 flex items-center justify-center">
              <Loader2 className="w-8 h-8 text-indigo-400 animate-spin" />
            </div>
          </div>
          <h2 className="text-xl font-display font-700 text-white mb-2">Verifying your email…</h2>
          <p className="text-sm text-white/30">This will only take a moment</p>
        </motion.div>
      )}

      {/* ───────── SUCCESS ───────── */}
      {status === 'success' && (
        <motion.div
          key="success"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="text-center py-6"
        >
          <div className="relative w-20 h-20 mx-auto mb-6">
            <div className="absolute inset-0 rounded-full bg-emerald-500/10 animate-pulse" />
            <div className="relative w-20 h-20 rounded-full bg-gradient-to-br from-emerald-500/15 to-emerald-400/10 border border-emerald-500/20 flex items-center justify-center">
              <CheckCircle2 className="w-9 h-9 text-emerald-400" />
            </div>
          </div>
          <h2 className="text-xl font-display font-700 text-white mb-2">Email Verified! 🎉</h2>
          <p className="text-sm text-white/35 mb-2">Your account is now active and ready to use.</p>
          <p className="text-xs text-white/20">Redirecting to login…</p>

          {/* Progress bar */}
          <div className="mt-6 mx-auto w-48 h-1 rounded-full bg-white/5 overflow-hidden">
            <motion.div
              initial={{ width: 0 }}
              animate={{ width: '100%' }}
              transition={{ duration: 3, ease: 'linear' }}
              className="h-full bg-gradient-to-r from-emerald-500 to-emerald-400 rounded-full"
            />
          </div>
        </motion.div>
      )}

      {/* ───────── NO TOKEN — Pending verification ───────── */}
      {status === 'no-token' && (
        <motion.div
          key="no-token"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="text-center py-4"
        >
          <div className="w-20 h-20 rounded-full bg-gradient-to-br from-indigo-500/15 to-violet-500/15 border border-indigo-500/20 flex items-center justify-center mx-auto mb-6">
            <Mail className="w-8 h-8 text-indigo-400" />
          </div>

          <h2 className="text-xl font-display font-700 text-white mb-3">Check your inbox</h2>

          <p className="text-sm text-white/35 mb-1 leading-relaxed">
            We&apos;ve sent a verification link to:
          </p>

          {email && (
            <p className="text-white/70 text-sm font-medium mb-6 px-4 py-2 rounded-lg bg-white/[0.03] border border-white/[0.06] inline-block">
              {email}
            </p>
          )}

          {!email && (
            <p className="text-white/30 text-sm mb-6">your registered email address</p>
          )}

          <div className="space-y-3">
            <button
              onClick={handleResend}
              disabled={isResendPending || resent}
              className="relative w-full flex items-center justify-center gap-2.5 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 group overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
              {isResendPending ? (
                <><Loader2 className="w-4 h-4 animate-spin" /> Sending...</>
              ) : resent ? (
                <><CheckCircle2 className="w-4 h-4 text-emerald-300" /> Email sent!</>
              ) : (
                <><RefreshCw className="w-4 h-4" /> Resend Verification Email</>
              )}
            </button>

            <Link
              href="/login"
              className="inline-flex items-center gap-2 text-sm text-white/30 hover:text-white/60 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" /> Back to sign in
            </Link>
          </div>

          <div className="mt-8 pt-5 border-t border-white/[0.04]">
            <p className="text-[11px] text-white/15 leading-relaxed">
              Didn&apos;t receive the email? Check your spam folder or try resending.
            </p>
          </div>
        </motion.div>
      )}

      {/* ───────── ERROR STATES ───────── */}
      {status === 'error' && (
        <motion.div
          key="error"
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="text-center py-4"
        >
          <div className="w-20 h-20 rounded-full bg-gradient-to-br from-red-500/15 to-orange-500/10 border border-red-500/20 flex items-center justify-center mx-auto mb-6">
            <XCircle className="w-9 h-9 text-red-400" />
          </div>

          <h2 className="text-xl font-display font-700 text-white mb-2">
            {errorType === 'expired' && 'Link Expired'}
            {errorType === 'outdated' && 'Link Already Used'}
            {errorType === 'invalid' && 'Invalid Link'}
          </h2>

          <p className="text-sm text-white/35 mb-6 leading-relaxed max-w-xs mx-auto">
            {errorType === 'expired' && 'This verification link has expired. Request a new one below.'}
            {errorType === 'outdated' && 'This link has already been used. If you\'re having trouble, try resending.'}
            {errorType === 'invalid' && 'This verification link is not valid. Please request a new one.'}
          </p>

          {email && (
            <p className="text-white/50 text-xs mb-5 px-3 py-1.5 rounded-lg bg-white/[0.03] border border-white/[0.06] inline-block">
              {email}
            </p>
          )}

          <div className="space-y-3">
            <button
              onClick={handleResend}
              disabled={isResendPending || resent}
              className="relative w-full flex items-center justify-center gap-2.5 py-3.5 bg-gradient-to-r from-indigo-600 to-violet-600 hover:from-indigo-500 hover:to-violet-500 disabled:opacity-50 text-white font-semibold rounded-xl transition-all duration-300 shadow-lg shadow-indigo-600/25 group overflow-hidden"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/[0.07] to-white/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-700" />
              {isResendPending ? (
                <><Loader2 className="w-4 h-4 animate-spin" /> Sending...</>
              ) : resent ? (
                <><CheckCircle2 className="w-4 h-4 text-emerald-300" /> Email sent!</>
              ) : (
                <><RefreshCw className="w-4 h-4" /> Resend Verification Email</>
              )}
            </button>

            <Link
              href="/login"
              className="inline-flex items-center gap-2 text-sm text-white/30 hover:text-white/60 transition-colors"
            >
              <ArrowLeft className="w-4 h-4" /> Back to sign in
            </Link>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

export default function VerifyEmailPage() {
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
            <Suspense fallback={<div className="text-white/20 text-sm text-center py-10">Loading...</div>}>
              <VerifyContent />
            </Suspense>
          </div>
        </div>
      </motion.div>
    </div>
  )
}