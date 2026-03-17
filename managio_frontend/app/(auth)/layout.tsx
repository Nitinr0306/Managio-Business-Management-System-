import { Dumbbell } from 'lucide-react'
import Link from 'next/link'

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen bg-[#070710] flex flex-col">
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-indigo-600/8 rounded-full blur-[120px]" />
        <div className="absolute bottom-0 left-0 w-80 h-80 bg-violet-600/6 rounded-full blur-[100px]" />
      </div>
      <header className="relative z-10 px-6 py-5 border-b border-white/5">
        <Link href="/" className="inline-flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/25">
            <Dumbbell className="w-4 h-4 text-white" />
          </div>
          <span className="text-lg font-display font-700 text-white tracking-tight">Managio</span>
        </Link>
      </header>
      <main className="relative z-10 flex-1 flex items-center justify-center p-6">{children}</main>
      <footer className="relative z-10 py-4 text-center text-xs text-white/20">
        © {new Date().getFullYear()} Managio · All rights reserved
      </footer>
    </div>
  )
}