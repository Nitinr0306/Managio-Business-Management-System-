'use client'
import { useState } from 'react'
import { Download, Loader2, Check } from 'lucide-react'
import { cn } from '@/lib/utils/cn'

export function ExportButton({
  onExport,
  label = 'Export CSV',
}: {
  onExport: () => Promise<void>
  label?: string
}) {
  const [loading, setLoading] = useState(false)
  const [done, setDone] = useState(false)

  const handleExport = async () => {
    setLoading(true)
    try {
      await onExport()
      setDone(true)
      setTimeout(() => setDone(false), 2000)
    } finally {
      setLoading(false)
    }
  }

  return (
    <button
      onClick={handleExport}
      disabled={loading}
      className={cn(
        'flex items-center gap-2 px-4 py-2.5 rounded-xl border text-sm font-medium transition-all disabled:opacity-60',
        done
          ? 'border-emerald-500/20 text-emerald-400 bg-emerald-500/[0.06]'
          : 'border-white/[0.08] text-white/60 hover:text-white/80 hover:bg-white/[0.04]'
      )}
    >
      {loading ? (
        <Loader2 className="w-4 h-4 animate-spin" />
      ) : done ? (
        <Check className="w-4 h-4" />
      ) : (
        <Download className="w-4 h-4" />
      )}
      {done ? 'Exported!' : label}
    </button>
  )
}