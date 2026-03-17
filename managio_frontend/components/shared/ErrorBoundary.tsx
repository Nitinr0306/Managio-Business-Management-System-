'use client'

import React from 'react'
import { motion } from 'framer-motion'
import { AlertTriangle, RefreshCw, Home } from 'lucide-react'
import Link from 'next/link'

interface Props {
  children: React.ReactNode
  fallback?: React.ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

export class ErrorBoundary extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error('[ErrorBoundary]', error, info)
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback

      return (
        <div className="flex flex-col items-center justify-center min-h-[60vh] p-8 text-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="max-w-md"
          >
            <div className="w-20 h-20 rounded-3xl bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto mb-6">
              <AlertTriangle className="w-10 h-10 text-red-400" />
            </div>
            <h2 className="text-2xl font-display font-700 mb-3">Something went wrong</h2>
            <p className="text-white/50 text-sm mb-2 leading-relaxed">
              An unexpected error occurred. This has been logged and we'll look into it.
            </p>
            {process.env.NODE_ENV === 'development' && this.state.error && (
              <p className="text-xs text-red-400/70 font-mono mb-6 bg-red-500/5 border border-red-500/10 rounded-xl px-3 py-2 text-left">
                {this.state.error.message}
              </p>
            )}
            <div className="flex items-center justify-center gap-3 mt-8">
              <button
                onClick={() => this.setState({ hasError: false })}
                className="flex items-center gap-2 px-5 py-2.5 border border-white/10 text-sm text-white/70 hover:text-white hover:bg-white/5 rounded-xl transition-all"
              >
                <RefreshCw className="w-4 h-4" />
                Try Again
              </button>
              <Link
                href="/dashboard"
                className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all"
              >
                <Home className="w-4 h-4" />
                Go to Dashboard
              </Link>
            </div>
          </motion.div>
        </div>
      )
    }
    return this.props.children
  }
}

// ── Functional wrapper for async errors ─────────────────────────────────────
export function withErrorBoundary<P extends object>(
  Component: React.ComponentType<P>,
  fallback?: React.ReactNode
) {
  return function WrappedComponent(props: P) {
    return (
      <ErrorBoundary fallback={fallback}>
        <Component {...props} />
      </ErrorBoundary>
    )
  }
}