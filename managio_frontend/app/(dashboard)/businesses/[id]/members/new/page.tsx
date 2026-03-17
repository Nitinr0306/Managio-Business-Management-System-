'use client'

import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, UserPlus } from 'lucide-react'
import { motion } from 'framer-motion'
import { MemberForm, MemberFormData } from '@/components/members/MemberForm'
import { useCreateMember } from '@/lib/hooks/useMembers'

export default function NewMemberPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const router = useRouter()
  const createMutation = useCreateMember(businessId)

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-8">
        <Link
          href={`/businesses/${businessId}/members`}
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to members
        </Link>
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center">
            <UserPlus className="w-5 h-5 text-indigo-400" />
          </div>
          <div>
            <h1 className="text-2xl font-display font-700">Add New Member</h1>
            <p className="text-sm text-white/45">Fill in the member details below</p>
          </div>
        </div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]"
      >
        <MemberForm
          onSubmit={async (data: MemberFormData) => {
            await createMutation.mutateAsync({
              firstName: data.firstName,
              lastName: data.lastName,
              email: data.email || undefined,
              phone: data.phone || undefined,
              dateOfBirth: data.dateOfBirth || undefined,
              gender: data.gender || undefined,
              address: data.address || undefined,
              emergencyContactName: data.emergencyContactName || undefined,
              emergencyContactPhone: data.emergencyContactPhone || undefined,
              notes: data.notes || undefined,
            })
            router.push(`/businesses/${businessId}/members`)
          }}
          loading={createMutation.isPending}
          submitLabel="Add Member"
        />
      </motion.div>
    </div>
  )
}