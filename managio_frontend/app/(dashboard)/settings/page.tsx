'use client'

import { useState } from 'react'
import { motion } from 'framer-motion'
import {
  Settings,
  Bell,
  Shield,
  Globe,
  Moon,
  Sun,
  Monitor,
  Check,
  Lock,
  Loader2,
  Eye,
  EyeOff,
  AlertTriangle,
  Save,
} from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useTheme } from 'next-themes'
import { authApi } from '@/lib/api/auth'
import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { PageHeader } from '@/components/shared/PageHeader'
import { useAuth } from '@/lib/hooks/useAuth'
import { cn } from '@/lib/utils/cn'

const pwSchema = z
  .object({
    currentPassword: z.string().min(1, 'Required'),
    newPassword: z
      .string()
      .min(8, 'Minimum 8 characters')
      .regex(/[A-Z]/, 'Uppercase letter required')
      .regex(/[a-z]/, 'Lowercase letter required')
      .regex(/[0-9]/, 'Number required')
      .regex(/[!@#$%^&*()\-_=+{};:,<.>]/, 'Special character required'),
    confirmPassword: z.string(),
  })
  .refine((d) => d.newPassword === d.confirmPassword, {
    message: 'Passwords do not match',
    path: ['confirmPassword'],
  })
type PwData = z.infer<typeof pwSchema>

type TabKey = 'appearance' | 'notifications' | 'security' | 'privacy'

const TABS: { key: TabKey; label: string; icon: React.ElementType }[] = [
  { key: 'appearance', label: 'Appearance', icon: Monitor },
  { key: 'notifications', label: 'Notifications', icon: Bell },
  { key: 'security', label: 'Security', icon: Shield },
  { key: 'privacy', label: 'Privacy', icon: Lock },
]

function SectionCard({
  title,
  description,
  children,
}: {
  title: string
  description?: string
  children: React.ReactNode
}) {
  return (
    <div className="p-5 rounded-2xl border border-white/6 bg-white/[0.02] mb-4">
      <div className="mb-5 pb-4 border-b border-white/5">
        <h3 className="text-sm font-display font-600 text-white/80">{title}</h3>
        {description && <p className="text-xs text-white/40 mt-0.5">{description}</p>}
      </div>
      {children}
    </div>
  )
}

function Toggle({ checked, onChange }: { checked: boolean; onChange: () => void }) {
  return (
    <button
      onClick={onChange}
      className={cn(
        'relative w-10 h-5 rounded-full transition-colors duration-200 flex-shrink-0',
        checked ? 'bg-indigo-600' : 'bg-white/15'
      )}
    >
      <span
        className={cn(
          'absolute top-0.5 left-0.5 w-4 h-4 rounded-full bg-white transition-transform duration-200 shadow-sm',
          checked ? 'translate-x-5' : 'translate-x-0'
        )}
      />
    </button>
  )
}

export default function SettingsPage() {
  const { theme, setTheme } = useTheme()
  const { logout } = useAuth()
  const [activeTab, setActiveTab] = useState<TabKey>('appearance')
  const [showPwd, setShowPwd] = useState<Record<string, boolean>>({})
  const [notifications, setNotifications] = useState({
    emailSubscriptionExpiry: true,
    emailPaymentRecorded: true,
    emailNewMember: false,
    emailStaffActivity: false,
    pushEnabled: true,
    weeklyReport: true,
  })

  const pwForm = useForm<PwData>({ resolver: zodResolver(pwSchema) })

  const changePwMutation = useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
      authApi.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      toast.success('Password changed! Please sign in again.')
      pwForm.reset()
      setTimeout(() => logout(), 1500)
    },
    onError: (err: any) =>
      toast.error(err?.response?.data?.message || 'Failed to change password.'),
  })

  const inp =
    'w-full bg-white/4 border border-white/8 rounded-xl px-4 py-2.5 text-sm text-white placeholder-white/25 focus:outline-none focus:border-indigo-500/60 transition-all'

  const themeOptions = [
    { value: 'dark', label: 'Dark', icon: Moon },
    { value: 'light', label: 'Light', icon: Sun },
    { value: 'system', label: 'System', icon: Monitor },
  ]

  return (
    <div className="max-w-3xl mx-auto">
      <PageHeader title="Settings" description="Customize your Managio experience" icon={Settings} />

      {/* Tab pills */}
      <div className="flex items-center gap-1 flex-wrap bg-white/3 border border-white/6 rounded-2xl p-1.5 mb-6">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={cn(
              'flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium transition-all',
              activeTab === tab.key
                ? 'bg-indigo-600 text-white shadow-lg shadow-indigo-600/20'
                : 'text-white/50 hover:text-white/80 hover:bg-white/5'
            )}
          >
            <tab.icon className="w-3.5 h-3.5" />
            {tab.label}
          </button>
        ))}
      </div>

      {/* ── Appearance ─────────────────────────────────────────────────────── */}
      {activeTab === 'appearance' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <SectionCard
            title="Theme"
            description="Choose how Managio looks across your devices"
          >
            <div className="grid grid-cols-3 gap-3">
              {themeOptions.map((opt) => (
                <button
                  key={opt.value}
                  onClick={() => setTheme(opt.value)}
                  className={cn(
                    'flex flex-col items-center gap-2 p-4 rounded-xl border transition-all',
                    theme === opt.value
                      ? 'border-indigo-500/60 bg-indigo-500/10'
                      : 'border-white/8 bg-white/[0.02] hover:bg-white/[0.04]'
                  )}
                >
                  <opt.icon
                    className={cn(
                      'w-5 h-5',
                      theme === opt.value ? 'text-indigo-400' : 'text-white/40'
                    )}
                  />
                  <span
                    className={cn(
                      'text-xs font-medium',
                      theme === opt.value ? 'text-white' : 'text-white/50'
                    )}
                  >
                    {opt.label}
                  </span>
                  {theme === opt.value && (
                    <Check className="w-3 h-3 text-indigo-400" />
                  )}
                </button>
              ))}
            </div>
          </SectionCard>

          <SectionCard
            title="Language & Region"
            description="Set your preferred language and timezone"
          >
            <div className="grid md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-medium text-white/50 mb-1.5">Language</label>
                <select className={inp + ' cursor-pointer'}>
                  <option value="en">English</option>
                  <option value="hi">Hindi</option>
                  <option value="mr">Marathi</option>
                </select>
              </div>
              <div>
                <label className="block text-xs font-medium text-white/50 mb-1.5">Timezone</label>
                <select className={inp + ' cursor-pointer'}>
                  <option value="Asia/Kolkata">Asia/Kolkata (IST)</option>
                  <option value="UTC">UTC</option>
                </select>
              </div>
            </div>
            <p className="text-xs text-white/25 mt-3">
              Language preferences are saved locally and don't affect your account.
            </p>
          </SectionCard>
        </motion.div>
      )}

      {/* ── Notifications ──────────────────────────────────────────────────── */}
      {activeTab === 'notifications' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <SectionCard
            title="Email Notifications"
            description="Control which emails you receive from Managio"
          >
            <div className="space-y-4">
              {[
                {
                  key: 'emailSubscriptionExpiry' as const,
                  label: 'Subscription Expiry Alerts',
                  desc: 'Get notified when member subscriptions are about to expire',
                },
                {
                  key: 'emailPaymentRecorded' as const,
                  label: 'Payment Confirmations',
                  desc: 'Email when a payment is recorded for a member',
                },
                {
                  key: 'emailNewMember' as const,
                  label: 'New Member Added',
                  desc: 'Notification when a new member is added to any of your businesses',
                },
                {
                  key: 'emailStaffActivity' as const,
                  label: 'Staff Activity Reports',
                  desc: 'Weekly summary of staff actions and login activity',
                },
                {
                  key: 'weeklyReport' as const,
                  label: 'Weekly Business Report',
                  desc: 'Summary of members, revenue, and subscriptions every Monday',
                },
              ].map((item) => (
                <div
                  key={item.key}
                  className="flex items-center justify-between py-1"
                >
                  <div className="flex-1 pr-4">
                    <div className="text-sm text-white/80">{item.label}</div>
                    <div className="text-xs text-white/35 mt-0.5">{item.desc}</div>
                  </div>
                  <Toggle
                    checked={notifications[item.key]}
                    onChange={() =>
                      setNotifications((p) => ({ ...p, [item.key]: !p[item.key] }))
                    }
                  />
                </div>
              ))}
            </div>
          </SectionCard>
        </motion.div>
      )}

      {/* ── Security ───────────────────────────────────────────────────────── */}
      {activeTab === 'security' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <SectionCard
            title="Change Password"
            description="Update your account password. All active sessions will be terminated."
          >
            <form
              onSubmit={pwForm.handleSubmit((d) =>
                changePwMutation.mutate({
                  currentPassword: d.currentPassword,
                  newPassword: d.newPassword,
                })
              )}
              className="space-y-4"
            >
              {[
                { field: 'currentPassword' as const, label: 'Current Password' },
                { field: 'newPassword' as const, label: 'New Password' },
                { field: 'confirmPassword' as const, label: 'Confirm New Password' },
              ].map(({ field, label }) => (
                <div key={field}>
                  <label className="block text-xs font-medium text-white/50 mb-1.5">
                    {label}
                  </label>
                  <div className="relative">
                    <input
                      {...pwForm.register(field)}
                      type={showPwd[field] ? 'text' : 'password'}
                      placeholder="••••••••"
                      className={inp + ' pr-11'}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPwd((p) => ({ ...p, [field]: !p[field] }))}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-white/40 hover:text-white/70 transition-colors"
                    >
                      {showPwd[field] ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                  {pwForm.formState.errors[field] && (
                    <p className="text-red-400 text-xs mt-1">
                      {pwForm.formState.errors[field]?.message}
                    </p>
                  )}
                </div>
              ))}
              <button
                type="submit"
                disabled={changePwMutation.isPending}
                className="flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white text-sm font-medium rounded-xl transition-all shadow-lg shadow-indigo-600/20"
              >
                {changePwMutation.isPending ? (
                  <><Loader2 className="w-4 h-4 animate-spin" /> Updating...</>
                ) : (
                  <><Save className="w-4 h-4" /> Update Password</>
                )}
              </button>
            </form>
          </SectionCard>

          <SectionCard
            title="Active Sessions"
            description="Manage where you're signed in"
          >
            <div className="space-y-3">
              <div className="flex items-center justify-between p-3 rounded-xl bg-white/[0.02] border border-white/5">
                <div>
                  <div className="text-sm text-white/80">Current Session</div>
                  <div className="text-xs text-white/35 mt-0.5">
                    This device • Active now
                  </div>
                </div>
                <span className="text-xs px-2 py-0.5 rounded-full bg-emerald-500/15 text-emerald-400">
                  Active
                </span>
              </div>
            </div>
            <button
              onClick={() => logout()}
              className="mt-4 flex items-center gap-2 px-4 py-2 rounded-xl border border-red-500/20 text-red-400/80 text-xs hover:bg-red-500/10 hover:text-red-400 transition-all"
            >
              <AlertTriangle className="w-3.5 h-3.5" />
              Sign out of all sessions
            </button>
          </SectionCard>
        </motion.div>
      )}

      {/* ── Privacy ────────────────────────────────────────────────────────── */}
      {activeTab === 'privacy' && (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
          <SectionCard
            title="Data & Privacy"
            description="Control how your data is used"
          >
            <div className="space-y-4">
              {[
                {
                  label: 'Usage Analytics',
                  desc: 'Help us improve Managio by sharing anonymous usage data',
                  defaultOn: true,
                },
                {
                  label: 'Error Reports',
                  desc: 'Automatically send error reports to our engineering team',
                  defaultOn: true,
                },
              ].map((item, i) => (
                <div key={i} className="flex items-center justify-between py-1">
                  <div className="flex-1 pr-4">
                    <div className="text-sm text-white/80">{item.label}</div>
                    <div className="text-xs text-white/35 mt-0.5">{item.desc}</div>
                  </div>
                  <Toggle checked={item.defaultOn} onChange={() => {}} />
                </div>
              ))}
            </div>
          </SectionCard>

          <div className="p-5 rounded-2xl border border-red-500/15 bg-red-500/5">
            <h3 className="text-sm font-display font-600 text-red-400 mb-1">Danger Zone</h3>
            <p className="text-xs text-white/40 mb-4">
              These actions are permanent and cannot be undone.
            </p>
            <button className="flex items-center gap-2 px-4 py-2 rounded-xl border border-red-500/25 text-red-400/80 text-xs hover:bg-red-500/10 hover:text-red-400 transition-all">
              <AlertTriangle className="w-3.5 h-3.5" />
              Delete My Account
            </button>
          </div>
        </motion.div>
      )}
    </div>
  )
}