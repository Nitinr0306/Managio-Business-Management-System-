'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import { Settings, Lock, Loader2, Save, Eye, EyeOff } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useAuth } from '@/lib/hooks/useAuth'
import { authApi } from '@/lib/api/auth'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/PageHeader'
import { getInitials } from '@/lib/utils/formatters'

const pwSchema = z.object({
  currentPassword: z.string().min(1, 'Current password required'),
  newPassword: z
    .string()
    .min(8, 'Min 8 chars')
    .regex(/[A-Z]/, 'Uppercase required')
    .regex(/[0-9]/, 'Number required'),
  confirmPassword: z.string(),
}).refine(d => d.newPassword === d.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
})
type PwData = z.infer<typeof pwSchema>

export default function ProfilePage() {
  const { user } = useAuth()
  const [showPwd, setShowPwd] = useState(false)
  const [activeTab, setActiveTab] = useState<'profile' | 'security'>('profile')

  const pwForm = useForm<PwData>({ resolver: zodResolver(pwSchema) })

  const changePwMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authApi.changePassword(currentPassword, newPassword),
    onSuccess: () => { toast.success('Password changed successfully!'); pwForm.reset() },
    onError: (err: unknown) => {
      import('@/lib/utils/errors').then(({ getErrorMessage }) => {
        toast.error(getErrorMessage(err, 'Failed to change password. Check your current password.'))
      })
    },
  })

  const inp = 'w-full bg-white/[0.04] border border-white/[0.08] rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'
  const lbl = 'block text-xs font-medium text-white/50 mb-1.5'

  return (
    <div className="max-w-2xl mx-auto">
      <PageHeader title="Profile" description="Manage your account settings" icon={Settings} />

      {/* Tabs */}
      <div className="flex items-center gap-1 bg-white/[0.04] border border-white/[0.08] rounded-xl p-1 w-fit mb-6">
        {(['profile', 'security'] as const).map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`px-4 py-2 rounded-lg text-sm font-medium capitalize transition-all ${
              activeTab === tab ? 'bg-indigo-600 text-white' : 'text-white/50 hover:text-white/80'
            }`}
          >
            {tab}
          </button>
        ))}
      </div>

      {activeTab === 'profile' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-6 rounded-2xl border border-white/[0.06] bg-white/[0.02]">
          <div className="flex items-center gap-4 mb-6 pb-6 border-b border-white/[0.05]">
            <div className="w-16 h-16 rounded-2xl bg-indigo-600/20 border border-indigo-500/20 flex items-center justify-center text-xl font-display font-700 text-indigo-300">
              {user ? getInitials(user.fullName || `${user.firstName} ${user.lastName}` || user.email) : '?'}
            </div>
            <div>
              <div className="text-xl font-display font-700">{user?.fullName || `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim()}</div>
              <div className="text-sm text-white/45">{user?.email}</div>
              <div className="flex items-center gap-2 mt-1.5">
                <span className="text-xs px-2 py-0.5 rounded-full bg-indigo-500/15 text-indigo-400">
                  {user?.roles?.[0]?.replace('ROLE_', '') || 'USER'}
                </span>
                {user?.emailVerified && (
                  <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400">Verified</span>
                )}
              </div>
            </div>
          </div>

          <div className="space-y-4">
            {[
              { label: 'Full Name', value: user?.fullName || `${user?.firstName ?? ''} ${user?.lastName ?? ''}`.trim() },
              { label: 'Email Address', value: user?.email },
              { label: 'Account Role', value: user?.roles?.[0]?.replace('ROLE_', '') || 'USER' },
            ].map(field => (
              <div key={field.label}>
                <label className={lbl}>{field.label}</label>
                <div className="w-full bg-white/[0.02] border border-white/[0.06] rounded-xl px-4 py-2.5 text-sm text-white/60">
                  {field.value}
                </div>
              </div>
            ))}
          </div>
        </motion.div>
      )}

      {activeTab === 'security' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="p-6 rounded-2xl border border-white/[0.06] bg-white/[0.02]">
          <div className="flex items-center gap-2 mb-5">
            <Lock className="w-4 h-4 text-indigo-400" />
            <h3 className="text-sm font-display font-600 text-white/80">Change Password</h3>
          </div>
          <form
            onSubmit={pwForm.handleSubmit(d =>
              changePwMutation.mutate({ currentPassword: d.currentPassword, newPassword: d.newPassword })
            )}
            className="space-y-4"
          >
            <div>
              <label className={lbl}>Current Password</label>
              <input {...pwForm.register('currentPassword')} type="password" placeholder="••••••••" className={inp} />
              {pwForm.formState.errors.currentPassword && (
                <p className="text-red-400 text-xs mt-1">{pwForm.formState.errors.currentPassword.message}</p>
              )}
            </div>
            <div>
              <label className={lbl}>New Password</label>
              <div className="relative">
                <input
                  {...pwForm.register('newPassword')}
                  type={showPwd ? 'text' : 'password'}
                  placeholder="Min 8 chars, uppercase, number"
                  className={inp + ' pr-11'}
                />
                <button
                  type="button"
                  onClick={() => setShowPwd(!showPwd)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40"
                >
                  {showPwd ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
              {pwForm.formState.errors.newPassword && (
                <p className="text-red-400 text-xs mt-1">{pwForm.formState.errors.newPassword.message}</p>
              )}
            </div>
            <div>
              <label className={lbl}>Confirm New Password</label>
              <input {...pwForm.register('confirmPassword')} type="password" placeholder="••••••••" className={inp} />
              {pwForm.formState.errors.confirmPassword && (
                <p className="text-red-400 text-xs mt-1">{pwForm.formState.errors.confirmPassword.message}</p>
              )}
            </div>
            <button
              type="submit"
              disabled={changePwMutation.isPending}
              className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/25"
            >
              {changePwMutation.isPending
                ? <><Loader2 className="w-4 h-4 animate-spin" /> Updating...</>
                : <><Save className="w-4 h-4" /> Update Password</>
              }
            </button>
          </form>
        </motion.div>
      )}
    </div>
  )
}