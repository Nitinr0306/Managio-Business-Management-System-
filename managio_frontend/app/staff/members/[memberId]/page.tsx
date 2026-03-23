'use client'

import { useSearchParams, useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft } from 'lucide-react'
import { useAuthStore } from '@/lib/store/authStore'
import {
  useMember,
  useMemberStats,
  useMemberSubscriptions,
  useMemberPayments,
  useUpdateMember,
} from '@/lib/hooks/useMembers'
import { MemberForm } from '@/components/members/MemberForm'
import { LoadingSpinner } from '@/components/shared/EmptyState'
import { StatsCard } from '@/components/shared/StatsCard'
import { PageHeader } from '@/components/shared/PageHeader'
import { formatCurrency } from '@/lib/utils/formatters'

export default function StaffMemberDetailPage() {
  const { memberId } = useParams<{ memberId: string }>()
  const businessId = useAuthStore((s) => (s.staffContext?.businessId ? String(s.staffContext.businessId) : ''))
  const canManageMembers = useAuthStore((s) => s.staffContext?.canManageMembers ?? false)

  const searchParams = useSearchParams()
  const router = useRouter()
  const isEditing = searchParams.get('edit') === '1'

  const { data: member, isLoading } = useMember(businessId, memberId)
  const { data: stats } = useMemberStats(businessId, memberId)
  const { data: subscriptions } = useMemberSubscriptions(businessId, memberId)
  const { data: payments } = useMemberPayments(businessId, memberId)
  const updateMutation = useUpdateMember(businessId, memberId)

  if (!canManageMembers) {
    return (
      <div className="max-w-2xl">
        <PageHeader title="Member" description="You don't have access to manage members." icon={ArrowLeft} />
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] text-sm text-white/50">
          Ask the owner to grant you member-management permissions.
        </div>
      </div>
    )
  }

  if (isLoading) return <LoadingSpinner />
  if (!member) return <div className="text-white/50 text-sm">Member not found</div>

  if (isEditing) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="mb-4">
          <Link
            href={`/staff/members/${memberId}`}
            className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" /> Back
          </Link>
        </div>
        <PageHeader title="Edit Member" description={member.fullName} icon={ArrowLeft} />
        <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]">
          <MemberForm
            defaultValues={{
              firstName: member.firstName,
              lastName: member.lastName,
              email: member.email,
              phone: member.phone,
              dateOfBirth: member.dateOfBirth,
              gender: member.gender as any,
              address: member.address,
              emergencyContactName: member.emergencyContactName,
              emergencyContactPhone: member.emergencyContactPhone,
              notes: member.notes,
            }}
            onSubmit={async (data) => {
              await updateMutation.mutateAsync(data)
              router.push(`/staff/members/${memberId}`)
            }}
            loading={updateMutation.isPending}
            submitLabel="Update Member"
          />
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-4">
        <Link
          href="/staff/members"
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" /> Back to members
        </Link>
      </div>

      <PageHeader title="Member" description={member.fullName} icon={ArrowLeft} />

      {stats && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
          <StatsCard title="Total Subscriptions" value={stats.totalSubscriptions} icon={ArrowLeft as any} accent="indigo" index={0} />
          <StatsCard title="Active Subs" value={stats.activeSubscriptions} icon={ArrowLeft as any} accent="emerald" index={1} />
          <StatsCard title="Completed Subs" value={stats.completedSubscriptions} icon={ArrowLeft as any} accent="cyan" index={2} />
          <StatsCard title="Total Paid" value={formatCurrency(stats.totalAmountPaid)} icon={ArrowLeft as any} accent="amber" index={3} />
        </div>
      )}

      <div className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]">
        <div className="flex items-start justify-between gap-4">
          <div>
            <div className="text-xl font-display font-700">{member.fullName}</div>
            <div className="text-sm text-white/45 mt-1">
              {member.email ?? '—'} • {member.phone ?? '—'}
            </div>
            <div className="mt-2">
              <span className="text-[10px] px-2 py-1 rounded-full bg-emerald-500/10 text-emerald-300/85 font-medium">
                {member.publicId || `MEM-${member.id}`}
              </span>
            </div>
          </div>
          <Link
            href={`/staff/members/${memberId}?edit=1`}
            className="px-4 py-2 rounded-xl border border-white/10 text-sm text-white/70 hover:text-white hover:bg-white/5 transition-all"
          >
            Edit
          </Link>
        </div>

        <div className="mt-6 grid md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl border border-white/6 bg-white/[0.01]">
            <div className="text-xs text-white/35 mb-2">Subscription history</div>
            <div className="text-sm text-white/60">
              {subscriptions?.length ? `${subscriptions.length} records` : 'No subscriptions yet'}
            </div>
          </div>
          <div className="p-4 rounded-xl border border-white/6 bg-white/[0.01]">
            <div className="text-xs text-white/35 mb-2">Payment history</div>
            <div className="text-sm text-white/60">
              {payments?.length ? `${payments.length} records` : 'No payments yet'}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

