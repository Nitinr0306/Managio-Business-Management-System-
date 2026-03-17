'use client'
import { useState } from 'react'
import { Download, Loader2 } from 'lucide-react'

export function ExportButton({ onExport, label = 'Export CSV' }: { onExport: () => Promise<void>; label?: string }) {
  const [loading, setLoading] = useState(false)
  return (
    <button
      onClick={async () => { setLoading(true); try { await onExport() } finally { setLoading(false) } }}
      disabled={loading}
      className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-white/10 text-sm text-white/70 hover:text-white hover:bg-white/5 transition-all disabled:opacity-60"
    >
      {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Download className="w-4 h-4" />}
      {label}
    </button>
  )
}