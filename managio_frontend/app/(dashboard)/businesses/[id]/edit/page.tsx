'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Building2 } from 'lucide-react'
import { motion } from 'framer-motion'
import { BusinessForm } from '@/components/business/BusinessForm'
import { useBusiness, useUpdateBusiness } from '@/lib/hooks/useBusiness'
import { LoadingSpinner } from '@/components/shared/EmptyState'

export default function EditBusinessPage() {
  const { id } = useParams<{ id: string }>()
  const router = useRouter()
  const { data: business, isLoading } = useBusiness(id)
  const updateMutation = useUpdateBusiness(id)

  if (isLoading) return <LoadingSpinner />

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <Link
          href={`/businesses/${id}`}
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to business
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center">
            <Building2 className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Edit Business</h1>
            <p className="text-sm text-white/45">{business?.name}</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]"
      >
        <BusinessForm
          defaultValues={business}
          onSubmit={async (data) => {
            await updateMutation.mutateAsync(data)
            router.push(`/businesses/${id}`)
          }}
          loading={updateMutation.isPending}
          submitLabel="Update Business"
        />
      </motion.div>
    </div>
  )
}