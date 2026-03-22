'use client'

export const dynamic = 'force-dynamic'

import { useRouter } from 'next/navigation'
import { Building2, ArrowLeft } from 'lucide-react'
import Link from 'next/link'
import { motion } from 'framer-motion'
import { BusinessForm } from '@/components/business/BusinessForm'
import { useCreateBusiness } from '@/lib/hooks/useBusiness'
import { useBusinessStore } from '@/lib/store/businessStore'

export default function NewBusinessPage() {
  const router = useRouter()
  const createMutation = useCreateBusiness()
  const { setCurrentBusiness } = useBusinessStore()

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <Link
          href="/businesses"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to businesses
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center">
            <Building2 className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Create New Business</h1>
            <p className="text-sm text-white/45">Set up your gym, studio, or small business</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/[0.06] bg-white/[0.02]"
      >
        <BusinessForm
          onSubmit={async (data) => {
            try {
              const biz = await createMutation.mutateAsync({ ...data, type: data.type ?? 'GYM' })
              setCurrentBusiness(biz)
              router.push(`/businesses/${biz.id}`)
            } catch {
              // error toast handled by useCreateBusiness onError
            }
          }}
          loading={createMutation.isPending}
          submitLabel="Create Business"
        />
      </motion.div>
    </div>
  )
}