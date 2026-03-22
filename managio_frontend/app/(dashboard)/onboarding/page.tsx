'use client'

import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useRouter } from 'next/navigation'
import {
  Building2, Users, CreditCard, CheckCircle, ArrowRight, ArrowLeft,
  Dumbbell, Sparkles, Loader2, Check,
} from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useCreateBusiness } from '@/lib/hooks/useBusiness'
import { useBusinessStore } from '@/lib/store/businessStore'
import Link from 'next/link'

const STEPS = [
  { id: 1, label: 'Welcome', icon: Sparkles },
  { id: 2, label: 'Create Business', icon: Building2 },
  { id: 3, label: 'Business Type', icon: Dumbbell },
  { id: 4, label: "You're Set!", icon: CheckCircle },
] as const

const businessSchema = z.object({
  name: z.string().min(2, 'Business name must be at least 2 characters').max(200),
  type: z.enum([
    'GYM', 'FITNESS_STUDIO', 'YOGA_STUDIO', 'DANCE_STUDIO',
    'MARTIAL_ARTS', 'SWIMMING_POOL', 'SALON', 'SPA', 'RESTAURANT', 'RETAIL', 'OTHER',
  ]),
  city: z.string().optional(),
  phone: z.string().optional(),
  email: z.string().email().optional().or(z.literal('')),
})
type BusinessData = z.infer<typeof businessSchema>

const BUSINESS_TYPES = [
  { value: 'GYM', label: 'Gym', emoji: '🏋️', desc: 'Weight training & fitness' },
  { value: 'FITNESS_STUDIO', label: 'Fitness Studio', emoji: '💪', desc: 'Group classes & cardio' },
  { value: 'YOGA_STUDIO', label: 'Yoga Studio', emoji: '🧘', desc: 'Yoga & mindfulness' },
  { value: 'DANCE_STUDIO', label: 'Dance Studio', emoji: '💃', desc: 'Dance classes & events' },
  { value: 'MARTIAL_ARTS', label: 'Martial Arts', emoji: '🥋', desc: 'MMA, karate, boxing' },
  { value: 'SWIMMING_POOL', label: 'Swimming Pool', emoji: '🏊', desc: 'Aquatic fitness' },
  { value: 'SALON', label: 'Salon', emoji: '💇', desc: 'Hair & beauty services' },
  { value: 'SPA', label: 'Spa', emoji: '🛁', desc: 'Relaxation & wellness' },
  { value: 'OTHER', label: 'Other', emoji: '🏢', desc: 'Any other business' },
] as const

function StepIndicator({ current, total }: { current: number; total: number }) {
  return (
    <div className="flex items-center justify-center gap-2 mb-8">
      {Array.from({ length: total }).map((_, i) => (
        <div
          key={i}
          className={`h-1.5 rounded-full transition-all duration-300 ${
            i < current
              ? 'w-6 bg-indigo-500'
              : i === current
              ? 'w-8 bg-indigo-400'
              : 'w-4 bg-white/15'
          }`}
        />
      ))}
    </div>
  )
}

export default function OnboardingPage() {
  const [step, setStep] = useState(0)
  const [selectedType, setSelectedType] = useState<BusinessData['type']>('GYM')
  const router = useRouter()
  const createMutation = useCreateBusiness()
  const { setCurrentBusiness } = useBusinessStore()

  const form = useForm<BusinessData>({
    resolver: zodResolver(businessSchema),
    defaultValues: { type: 'GYM' },
  })

  const next = () => setStep((s) => Math.min(s + 1, 3))
  const prev = () => setStep((s) => Math.max(s - 1, 0))

  const handleCreate = async () => {
    const values = form.getValues()
    values.type = selectedType
    const isValid = await form.trigger(['name'])
    if (!isValid) { setStep(1); return }
    try {
      const biz = await createMutation.mutateAsync({ ...values, type: selectedType })
      setCurrentBusiness(biz)
      next()
    } catch {}
  }

  const inp =
    'w-full bg-white/5 border border-white/[0.1] rounded-xl px-4 py-3 text-sm text-white placeholder-white/30 focus:outline-none focus:border-indigo-500/70 transition-all'

  const variants = {
    enter: { opacity: 0, x: 30 },
    center: { opacity: 1, x: 0 },
    exit: { opacity: 0, x: -30 },
  }

  return (
    <div className="min-h-screen bg-[#070710] flex items-center justify-center p-6">
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-indigo-600/8 rounded-full blur-[120px]" />
        <div className="absolute bottom-1/4 right-1/4 w-72 h-72 bg-violet-600/6 rounded-full blur-[100px]" />
      </div>

      <div className="relative z-10 w-full max-w-2xl">
        <StepIndicator current={step} total={4} />

        <AnimatePresence mode="wait">
          {/* ── Step 0: Welcome ─────────────────────────────────────────── */}
          {step === 0 && (
            <motion.div key="welcome" variants={variants} initial="enter" animate="center" exit="exit" transition={{ duration: 0.3 }} className="text-center">
              <div className="w-20 h-20 rounded-3xl bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center mx-auto mb-6 shadow-2xl shadow-indigo-500/40">
                <Dumbbell className="w-10 h-10 text-white" />
              </div>
              <h1 className="text-4xl font-display font-800 mb-3">
                Welcome to <span className="gradient-text">Managio</span>
              </h1>
              <p className="text-white/50 text-lg mb-3 max-w-md mx-auto leading-relaxed">
                You're a few steps away from having your business fully managed. Let's get started!
              </p>
              <p className="text-white/30 text-sm mb-10">
                This takes less than 2 minutes.
              </p>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <button
                  onClick={next}
                  className="inline-flex items-center justify-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-2xl transition-all shadow-2xl shadow-indigo-600/40 hover:-translate-y-0.5"
                >
                  Get Started
                  <ArrowRight className="w-4 h-4" />
                </button>
                <button
                  onClick={() => router.replace('/dashboard')}
                  className="inline-flex items-center justify-center px-8 py-4 border border-white/[0.1] text-white/60 hover:text-white hover:bg-white/[0.04] rounded-2xl transition-all"
                >
                  Skip for now
                </button>
              </div>
            </motion.div>
          )}

          {/* ── Step 1: Business Name & Contact ─────────────────────────── */}
          {step === 1 && (
            <motion.div key="name" variants={variants} initial="enter" animate="center" exit="exit" transition={{ duration: 0.3 }}>
              <div className="glass rounded-3xl p-8 border border-white/[0.08]">
                <div className="flex items-center gap-3 mb-6">
                  <div className="w-10 h-10 rounded-2xl bg-indigo-500/20 flex items-center justify-center">
                    <Building2 className="w-5 h-5 text-indigo-400" />
                  </div>
                  <div>
                    <h2 className="text-xl font-display font-700">Name your business</h2>
                    <p className="text-xs text-white/40 mt-0.5">You can change this anytime</p>
                  </div>
                </div>
                <div className="space-y-4">
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">
                      Business Name <span className="text-red-400">*</span>
                    </label>
                    <input
                      {...form.register('name')}
                      placeholder="e.g. FitZone Gym, Rhythm Dance Academy"
                      className={inp}
                      autoFocus
                    />
                    {form.formState.errors.name && (
                      <p className="text-red-400 text-xs mt-1">{form.formState.errors.name.message}</p>
                    )}
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">City</label>
                      <input {...form.register('city')} placeholder="Mumbai, Delhi..." className={inp} />
                    </div>
                    <div>
                      <label className="block text-xs font-medium text-white/60 mb-1.5">Phone</label>
                      <input {...form.register('phone')} placeholder="+91 98765 43210" className={inp} />
                    </div>
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-white/60 mb-1.5">Business Email</label>
                    <input {...form.register('email')} type="email" placeholder="info@yourbusiness.com" className={inp} />
                  </div>
                </div>
                <div className="flex gap-3 mt-6">
                  <button onClick={prev} className="flex items-center gap-2 px-5 py-3 border border-white/[0.1] text-white/60 hover:text-white hover:bg-white/[0.04] rounded-xl transition-all">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={() => form.trigger('name').then((ok) => ok && next())}
                    className="flex-1 flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition-all"
                  >
                    Continue <ArrowRight className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {/* ── Step 2: Business Type ──────────────────────────────────── */}
          {step === 2 && (
            <motion.div key="type" variants={variants} initial="enter" animate="center" exit="exit" transition={{ duration: 0.3 }}>
              <div className="glass rounded-3xl p-8 border border-white/[0.08]">
                <div className="mb-6">
                  <h2 className="text-xl font-display font-700 mb-1">What kind of business?</h2>
                  <p className="text-sm text-white/40">This helps us tailor your dashboard experience</p>
                </div>
                <div className="grid grid-cols-3 gap-2.5 mb-6">
                  {BUSINESS_TYPES.map((bt) => (
                    <button
                      key={bt.value}
                      onClick={() => setSelectedType(bt.value as BusinessData['type'])}
                      className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl border text-center transition-all ${
                        selectedType === bt.value
                          ? 'border-indigo-500/60 bg-indigo-500/10'
                          : 'border-white/[0.08] bg-white/[0.02] hover:bg-white/[0.04] hover:border-white/15'
                      }`}
                    >
                      <span className="text-xl">{bt.emoji}</span>
                      <span className={`text-xs font-medium ${selectedType === bt.value ? 'text-white' : 'text-white/60'}`}>
                        {bt.label}
                      </span>
                    </button>
                  ))}
                </div>
                <div className="flex gap-3">
                  <button onClick={prev} className="flex items-center gap-2 px-5 py-3 border border-white/[0.1] text-white/60 hover:text-white hover:bg-white/[0.04] rounded-xl transition-all">
                    <ArrowLeft className="w-4 h-4" /> Back
                  </button>
                  <button
                    onClick={handleCreate}
                    disabled={createMutation.isPending}
                    className="flex-1 flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-60 text-white font-medium rounded-xl transition-all"
                  >
                    {createMutation.isPending ? (
                      <><Loader2 className="w-4 h-4 animate-spin" /> Creating...</>
                    ) : (
                      <>Create Business <ArrowRight className="w-4 h-4" /></>
                    )}
                  </button>
                </div>
              </div>
            </motion.div>
          )}

          {/* ── Step 3: Done ──────────────────────────────────────────── */}
          {step === 3 && (
            <motion.div key="done" variants={variants} initial="enter" animate="center" exit="exit" transition={{ duration: 0.3 }} className="text-center">
              <motion.div
                initial={{ scale: 0, rotate: -15 }}
                animate={{ scale: 1, rotate: 0 }}
                transition={{ type: 'spring', stiffness: 300, damping: 20, delay: 0.1 }}
                className="w-20 h-20 rounded-3xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center mx-auto mb-6"
              >
                <CheckCircle className="w-10 h-10 text-emerald-400" />
              </motion.div>
              <h2 className="text-3xl font-display font-800 mb-3">You're all set! 🎉</h2>
              <p className="text-white/50 mb-8 max-w-sm mx-auto">
                Your business is ready. Start by adding members, creating subscription plans, or inviting staff.
              </p>
              <div className="grid sm:grid-cols-3 gap-3 max-w-md mx-auto mb-8">
                {[
                  { icon: Users, label: 'Add Members', href: `/businesses/${createMutation.data?.id}/members/new` },
                  { icon: CreditCard, label: 'Create Plans', href: `/businesses/${createMutation.data?.id}/subscriptions/plans` },
                  { icon: Building2, label: 'View Dashboard', href: '/dashboard' },
                ].map((item) => (
                  <Link
                    key={item.label}
                    href={item.href}
                    className="flex flex-col items-center gap-2 p-4 rounded-2xl border border-white/[0.08] bg-white/[0.02] hover:bg-white/[0.05] hover:border-white/15 transition-all"
                  >
                    <item.icon className="w-5 h-5 text-indigo-400" />
                    <span className="text-xs text-white/60">{item.label}</span>
                  </Link>
                ))}
              </div>
              <button
                onClick={() => router.replace('/dashboard')}
                className="inline-flex items-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-2xl transition-all shadow-xl shadow-indigo-600/30"
              >
                Go to Dashboard <ArrowRight className="w-4 h-4" />
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  )
}