'use client'

import { useParams, useSearchParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { ArrowLeft, Edit, Mail, Phone, MapPin, Calendar, CreditCard, TrendingUp, Activity, ShieldOff, ShieldCheck } from 'lucide-react'
import { motion } from 'framer-motion'
import { useState } from 'react'
import {
  useMember,
  useMemberStats,
  useMemberSubscriptions,
  useMemberPayments,
  useUpdateMember,
  useDisableMemberPortal,
  useEnableMemberPortal,
} from '@/lib/hooks/useMembers'
import { MemberForm } from '@/components/members/MemberForm'
import { LoadingSpinner } from '@/components/shared/EmptyState'
import { StatsCard } from '@/components/shared/StatsCard'
import { formatCurrency, formatDate, formatRelative } from '@/lib/utils/formatters'
import { cn } from '@/lib/utils/cn'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'

export default function MemberDetailPage() {
  const params = useParams<{ id: string; memberId: string }>()
  const businessId = params.id
  const memberId = params.memberId

  const searchParams = useSearchParams()
  const router = useRouter()
  const isEditing = searchParams.get('edit') === '1'
  const [activeTab, setActiveTab] = useState<'overview' | 'subscriptions' | 'payments'>('overview')

  const { data: member, isLoading } = useMember(businessId, memberId)
  const { data: stats } = useMemberStats(businessId, memberId)
  const { data: subscriptions } = useMemberSubscriptions(businessId, memberId)
  const { data: payments } = useMemberPayments(businessId, memberId)
  const updateMutation = useUpdateMember(businessId, memberId)
  const disablePortal = useDisableMemberPortal()
  const enablePortal = useEnableMemberPortal()
  const [confirmDisable, setConfirmDisable] = useState(false)
  const [confirmEnable, setConfirmEnable] = useState(false)

  if (isLoading) return <LoadingSpinner />
  if (!member) return <div className="text-white/50 text-sm">Member not found</div>

  if (isEditing) {
    return (
      <div className="max-w-3xl mx-auto">
        <div className="mb-8">
          <Link
            href={`/businesses/${businessId}/members/${memberId}`}
            className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
          >
            <ArrowLeft className="w-4 h-4" /> Back to profile
          </Link>
          <h1 className="text-2xl font-display font-700">Edit Member</h1>
        </div>
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
              router.push(`/businesses/${businessId}/members/${memberId}`)
            }}
            loading={updateMutation.isPending}
            submitLabel="Update Member"
          />
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-6">
        <Link
          href={`/businesses/${businessId}/members`}
          className="inline-flex items-center gap-2 text-sm text-white/50 hover:text-white/80 transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" /> Back to members
        </Link>

        {/* Header card */}
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-6 rounded-2xl border border-white/6 bg-white/[0.02] mb-6"
        >
          <div className="flex items-start justify-between gap-4">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 rounded-2xl bg-indigo-600/20 border border-indigo-500/20 flex items-center justify-center text-xl font-display font-700 text-indigo-300">
                {member.fullName.slice(0, 2).toUpperCase()}
              </div>
              <div>
                <h1 className="text-2xl font-display font-700 mb-1">{member.fullName}</h1>
                <div className="flex flex-wrap gap-3 text-sm text-white/45">
                  {member.email && (
                    <span className="flex items-center gap-1">
                      <Mail className="w-3 h-3" />{member.email}
                    </span>
                  )}
                  {member.phone && (
                    <span className="flex items-center gap-1">
                      <Phone className="w-3 h-3" />{member.phone}
                    </span>
                  )}
                  {member.address && (
                    <span className="flex items-center gap-1">
                      <MapPin className="w-3 h-3" />{member.address}
                    </span>
                  )}
                </div>
                <div className="flex items-center gap-2 mt-2">
                  <span className={cn(
                    'text-xs px-2.5 py-1 rounded-full font-medium',
                    member.status === 'ACTIVE'
                      ? 'bg-emerald-500/15 text-emerald-400'
                      : 'bg-red-500/15 text-red-400'
                  )}>
                    {member.status}
                  </span>
                  <span className="text-xs text-white/35 flex items-center gap-1">
                    <Calendar className="w-3 h-3" /> Joined {formatDate(member.createdAt)}
                  </span>
                </div>
              </div>
            </div>
            <Link
              href={`/businesses/${businessId}/members/${memberId}?edit=1`}
              className="flex items-center gap-2 px-4 py-2 border border-white/10 rounded-xl text-sm text-white/70 hover:text-white hover:bg-white/5 transition-all"
            >
              <Edit className="w-3.5 h-3.5" /> Edit
            </Link>
            <button
              onClick={() => setConfirmDisable(true)}
              className="flex items-center gap-2 px-4 py-2 border border-red-500/20 rounded-xl text-sm text-red-300 hover:text-red-200 hover:bg-red-500/10 transition-all"
            >
              <ShieldOff className="w-3.5 h-3.5" /> Disable Portal
            </button>
            <button
              onClick={() => setConfirmEnable(true)}
              className="flex items-center gap-2 px-4 py-2 border border-emerald-500/20 rounded-xl text-sm text-emerald-300 hover:text-emerald-200 hover:bg-emerald-500/10 transition-all"
            >
              <ShieldCheck className="w-3.5 h-3.5" /> Enable Portal
            </button>
          </div>
        </motion.div>

        {/* Stats */}
        {stats && (
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <StatsCard title="Total Subscriptions" value={stats.totalSubscriptions} icon={CreditCard} accent="indigo" index={0} />
            <StatsCard title="Active Subs" value={stats.activeSubscriptions} icon={Activity} accent="emerald" index={1} />
            <StatsCard title="Completed Subs" value={stats.completedSubscriptions} icon={TrendingUp} accent="cyan" index={2} />
            <StatsCard title="Total Paid" value={formatCurrency(stats.totalAmountPaid)} icon={TrendingUp} accent="amber" index={3} />
          </div>
        )}

        {/* Tabs */}
        <div className="flex items-center gap-1 bg-white/4 border border-white/8 rounded-xl p-1 mb-5 w-fit">
          {(['overview', 'subscriptions', 'payments'] as const).map(tab => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                'px-4 py-2 rounded-lg text-sm font-medium capitalize transition-all',
                activeTab === tab ? 'bg-indigo-600 text-white' : 'text-white/50 hover:text-white/80'
              )}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Tab content */}
        {activeTab === 'overview' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="grid md:grid-cols-2 gap-4">
            <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] space-y-3">
              <h3 className="text-sm font-display font-600 text-white/70">Personal Details</h3>
              {[
                { label: 'Date of Birth', value: member.dateOfBirth ? formatDate(member.dateOfBirth) : '—' },
                { label: 'Gender', value: member.gender || '—' },
                { label: 'Address', value: member.address || '—' },
              ].map(d => (
                <div key={d.label} className="flex justify-between text-sm">
                  <span className="text-white/40">{d.label}</span>
                  <span className="text-white/70">{d.value}</span>
                </div>
              ))}
            </div>
            <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] space-y-3">
              <h3 className="text-sm font-display font-600 text-white/70">Emergency Contact</h3>
              {[
                { label: 'Name', value: member.emergencyContactName || '—' },
                { label: 'Phone', value: member.emergencyContactPhone || '—' },
              ].map(d => (
                <div key={d.label} className="flex justify-between text-sm">
                  <span className="text-white/40">{d.label}</span>
                  <span className="text-white/70">{d.value}</span>
                </div>
              ))}
              {member.notes && (
                <>
                  <h3 className="text-sm font-display font-600 text-white/70 pt-2 border-t border-white/5">Notes</h3>
                  <p className="text-sm text-white/50">{member.notes}</p>
                </>
              )}
            </div>
          </motion.div>
        )}

        {activeTab === 'subscriptions' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            {!subscriptions?.length ? (
              <div className="py-12 text-center text-sm text-white/30">No subscriptions yet</div>
            ) : (
              <div className="space-y-3">
                {subscriptions.map(sub => (
                  <div key={sub.id} className="flex items-center justify-between p-4 rounded-xl border border-white/6 bg-white/[0.02]">
                    <div>
                      <div className="text-sm font-medium text-white/80">{sub.planName}</div>
                      <div className="text-xs text-white/40 mt-0.5">
                        {formatDate(sub.startDate)} → {formatDate(sub.endDate)}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {sub.amount != null && (
                        <span className="text-sm font-display font-600 text-amber-400">{formatCurrency(sub.amount)}</span>
                      )}
                      <span className={cn(
                        'text-xs px-2.5 py-1 rounded-full',
                        sub.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' :
                        sub.status === 'EXPIRED' ? 'bg-red-500/15 text-red-400' :
                        'bg-white/5 text-white/40'
                      )}>
                        {sub.status}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </motion.div>
        )}

        {activeTab === 'payments' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
            {!payments?.length ? (
              <div className="py-12 text-center text-sm text-white/30">No payments yet</div>
            ) : (
              <div className="space-y-3">
                {payments.map(pay => (
                  <div key={pay.id} className="flex items-center justify-between p-4 rounded-xl border border-white/6 bg-white/[0.02]">
                    <div>
                      <div className="text-sm font-medium text-white/80">{pay.planName || 'Manual Payment'}</div>
                      <div className="text-xs text-white/40 mt-0.5">
                        {formatRelative(pay.createdAt)} • {pay.paymentMethod}
                      </div>
                    </div>
                    <span className="text-sm font-display font-600 text-emerald-400">{formatCurrency(pay.amount)}</span>
                  </div>
                ))}
              </div>
            )}
          </motion.div>
        )}
      </div>

      <ConfirmDialog
        open={confirmDisable}
        onClose={() => setConfirmDisable(false)}
        onConfirm={async () => {
          await disablePortal.mutateAsync(memberId)
          setConfirmDisable(false)
        }}
        title="Disable Member Portal"
        description="This member will no longer be able to log in to the member portal."
        confirmLabel="Disable"
        loading={disablePortal.isPending}
      />

      <ConfirmDialog
        open={confirmEnable}
        onClose={() => setConfirmEnable(false)}
        onConfirm={async () => {
          await enablePortal.mutateAsync(memberId)
          setConfirmEnable(false)
        }}
        title="Enable Member Portal"
        description="This member will be able to log in to the member portal again."
        confirmLabel="Enable"
        loading={enablePortal.isPending}
      />
    </div>
  )
}