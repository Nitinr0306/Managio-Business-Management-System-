'use client'

import { useParams, useRouter } from 'next/navigation'
import { UserPlus } from 'lucide-react'
import { motion } from 'framer-motion'
import { MemberForm, MemberFormData } from '@/components/members/MemberForm'
import { useCreateMember } from '@/lib/hooks/useMembers'
import { PageHeader } from '@/components/shared/PageHeader'

export default function NewMemberPage() {
  const { id: businessId } = useParams<{ id: string }>()
  const router = useRouter()
  const createMutation = useCreateMember(businessId)

  return (
    <div className="max-w-3xl mx-auto">
      <PageHeader
        title="Add New Member"
        description="Fill in the member details below"
        icon={UserPlus}
        breadcrumbs={[
          { label: 'Members', href: `/businesses/${businessId}/members` },
          { label: 'New Member' },
        ]}
      />

      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ ease: [0.22, 1, 0.36, 1] }}
        className="p-6 rounded-2xl border border-white/[0.06] bg-[hsl(var(--card))]"
      >
        <MemberForm
          onSubmit={async (data: MemberFormData) => {
            try {
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
            } catch {
              // Error toast is handled by useCreateMember hook — stay on form for retry
            }
          }}
          loading={createMutation.isPending}
          submitLabel="Add Member"
        />
      </motion.div>
    </div>
  )
}