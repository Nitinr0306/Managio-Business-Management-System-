'use client'

import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, UserPlus } from 'lucide-react'
import { motion } from 'framer-motion'
import { MemberForm, MemberFormData } from '@/components/members/MemberForm'
import { useCreateMember } from '@/lib/hooks/useMembers'
import { useAuthStore } from '@/lib/store/authStore'
import { PageHeader } from '@/components/shared/PageHeader'

export default function StaffNewMemberPage() {
  const router = useRouter()
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const createMutation = useCreateMember(businessId)

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-4">
        <Link
          href="/staff/members"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to members
        </Link>
      </div>

      <PageHeader
        title="Add Member"
        description="Create a new member profile"
        icon={UserPlus}
      />

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
            router.push('/staff/members')
          }}
          loading={createMutation.isPending}
          submitLabel="Add Member"
        />
      </motion.div>
    </div>
  )
}

