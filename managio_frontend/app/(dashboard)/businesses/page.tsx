'use client'
import { motion } from 'framer-motion'
import Link from 'next/link'
import { Building2, Plus, MapPin, Phone, Mail, ArrowRight, Trash2, Edit, Users, CreditCard } from 'lucide-react'
import { useMyBusinesses, useDeleteBusiness } from '@/lib/hooks/useBusiness'
import { useBusinessStore } from '@/lib/store/businessStore'
import { PageHeader } from '@/components/shared/PageHeader'
import { EmptyState, CardSkeleton } from '@/components/shared/EmptyState'
import { ConfirmDialog } from '@/components/shared/ConfirmDialog'
import { useState } from 'react'
import type { Business } from '@/lib/types/business'
import { cn } from '@/lib/utils/cn'

const TYPE_COLORS: Record<string, string> = {
  GYM:'bg-indigo-500/15 text-indigo-400', FITNESS_STUDIO:'bg-violet-500/15 text-violet-400',
  YOGA_STUDIO:'bg-emerald-500/15 text-emerald-400', DANCE_STUDIO:'bg-pink-500/15 text-pink-400',
  MARTIAL_ARTS:'bg-red-500/15 text-red-400', SWIMMING_POOL:'bg-cyan-500/15 text-cyan-400',
}

function BusinessCard({ business }: { business: Business }) {
  const { setCurrentBusiness } = useBusinessStore()
  const [confirmDelete, setConfirmDelete] = useState(false)
  const deleteMutation = useDeleteBusiness()

  return (
    <>
      <motion.div
        initial={{ opacity:0, y:16 }} animate={{ opacity:1, y:0 }} whileHover={{ y:-3 }}
        transition={{ duration:0.3 }}
        className="relative p-5 rounded-2xl border border-white/6 bg-white/[0.02] hover:bg-white/[0.04] hover:border-white/10 transition-all duration-300"
      >
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className="w-11 h-11 rounded-xl bg-indigo-600/20 border border-indigo-500/20 flex items-center justify-center text-sm font-display font-700 text-indigo-300">
              {business.name.slice(0,2).toUpperCase()}
            </div>
            <div>
              <h3 className="font-display font-600 text-white">{business.name}</h3>
              <span className={cn('text-xs px-2 py-0.5 rounded-full', TYPE_COLORS[business.type] || 'bg-white/5 text-white/40')}>
                {business.type.replace(/_/g,' ')}
              </span>
            </div>
          </div>
          <span className={cn('text-xs px-2 py-1 rounded-full', business.status === 'ACTIVE' ? 'bg-emerald-500/15 text-emerald-400' : 'bg-red-500/15 text-red-400')}>
            {business.status}
          </span>
        </div>

        {business.description && <p className="text-xs text-white/35 mb-3 line-clamp-2">{business.description}</p>}

        <div className="space-y-1.5 mb-4">
          {business.city && (
            <div className="flex items-center gap-2 text-xs text-white/40">
              <MapPin className="w-3 h-3" />{[business.city,business.state,business.country].filter(Boolean).join(', ')}
            </div>
          )}
          {business.phone && <div className="flex items-center gap-2 text-xs text-white/40"><Phone className="w-3 h-3" />{business.phone}</div>}
          {business.email && <div className="flex items-center gap-2 text-xs text-white/40"><Mail className="w-3 h-3" />{business.email}</div>}
        </div>

        <div className="flex items-center gap-1.5 text-xs text-white/30 mb-4">
          <Users className="w-3.5 h-3.5" />{business.memberCount} members
          <span className="mx-1 opacity-40">·</span>
          <CreditCard className="w-3.5 h-3.5" />{business.staffCount} staff
        </div>

        <div className="flex items-center gap-2">
          <Link
            href={`/businesses/${business.id}`}
            onClick={() => setCurrentBusiness(business)}
            className="flex-1 flex items-center justify-center gap-2 py-2.5 bg-indigo-600/20 hover:bg-indigo-600/30 text-indigo-300 text-sm font-medium rounded-xl transition-all"
          >
            Open <ArrowRight className="w-3.5 h-3.5" />
          </Link>
          <Link href={`/businesses/${business.id}/edit`} className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-white/80 hover:bg-white/5 transition-all">
            <Edit className="w-3.5 h-3.5" />
          </Link>
          <button
            onClick={() => setConfirmDelete(true)}
            className="w-9 h-9 flex items-center justify-center rounded-xl border border-white/8 text-white/40 hover:text-red-400 hover:bg-red-500/10 hover:border-red-500/20 transition-all"
          >
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      </motion.div>

      <ConfirmDialog
        open={confirmDelete}
        onClose={() => setConfirmDelete(false)}
        onConfirm={() => { deleteMutation.mutate(business.id); setConfirmDelete(false) }}
        title="Delete Business"
        description={`Are you sure you want to delete "${business.name}"? All associated data will be permanently removed. This cannot be undone.`}
        confirmLabel="Delete Business"
        loading={deleteMutation.isPending}
      />
    </>
  )
}

export default function BusinessesPage() {
  const { data: businesses, isLoading } = useMyBusinesses()

  return (
    <div>
      <PageHeader
        title="My Businesses"
        description="Manage all your businesses from one place"
        icon={Building2}
        actions={
          <Link href="/businesses/new" className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25">
            <Plus className="w-4 h-4" /> New Business
          </Link>
        }
      />

      {isLoading ? (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
          {Array.from({ length:3 }).map((_,i) => <CardSkeleton key={i} />)}
        </div>
      ) : !businesses?.length ? (
        <EmptyState
          icon={Building2}
          title="No businesses yet"
          description="Create your first business to start managing members, staff, and payments."
          action={
            <Link href="/businesses/new" className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-medium rounded-xl transition-all">
              <Plus className="w-4 h-4" /> Create Business
            </Link>
          }
        />
      ) : (
        <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-5">
          {businesses.map(biz => <BusinessCard key={biz.id} business={biz} />)}
        </div>
      )}
    </div>
  )
}