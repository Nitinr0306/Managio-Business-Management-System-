'use client'

import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion'
import { useRef, useState, useEffect } from 'react'
import Link from 'next/link'
import {
  Users, CreditCard, BarChart3, Shield, ChevronRight,
  ArrowRight, Check, Menu, X, Building2, Calendar,
  TrendingUp, Lock, Layers, ArrowUpRight, Zap
} from 'lucide-react'

/* ═══════════════════════════════════════════════════════════════
   LANDING PAGE — Product Truth, No Fabricated Data
   ═══════════════════════════════════════════════════════════════ */

const NAV_LINKS = [
  { label: 'Features', href: '#features' },
  { label: 'How it Works', href: '#how-it-works' },
]

const FEATURES = [
  {
    icon: Building2,
    title: 'Multi-Business Management',
    description: 'Manage multiple businesses from one account. Each business has its own members, staff, subscriptions, and financial data — completely isolated.',
    color: 'from-indigo-500 to-violet-500',
    glow: 'rgba(99,102,241,0.15)',
  },
  {
    icon: Users,
    title: 'Member Management',
    description: 'Track members with detailed profiles, contact info, status tracking, and subscription history. Add individually or import via CSV.',
    color: 'from-emerald-500 to-teal-500',
    glow: 'rgba(16,185,129,0.15)',
  },
  {
    icon: Shield,
    title: 'Staff & Role-Based Access',
    description: 'Invite staff via email, assign granular role-based permissions. Full audit trail tracks every action for accountability.',
    color: 'from-amber-500 to-orange-500',
    glow: 'rgba(245,158,11,0.15)',
  },
  {
    icon: Calendar,
    title: 'Subscription Plans',
    description: 'Create flexible plans with custom durations. Track active subscriptions, monitor expirations, and manage renewals.',
    color: 'from-pink-500 to-rose-500',
    glow: 'rgba(236,72,153,0.15)',
  },
  {
    icon: CreditCard,
    title: 'Payment Processing',
    description: 'Record payments across UPI, cash, card, and more. Full payment history with member attribution and method breakdowns.',
    color: 'from-cyan-500 to-blue-500',
    glow: 'rgba(6,182,212,0.15)',
  },
  {
    icon: BarChart3,
    title: 'Analytics Dashboard',
    description: 'Real-time business analytics: revenue trends, member activity, payment method distribution, and subscription health — driven by your actual data.',
    color: 'from-violet-500 to-purple-500',
    glow: 'rgba(139,92,246,0.15)',
  },
]

const ARCHITECTURE = [
  {
    step: '01',
    title: 'Create Your Business',
    desc: 'Sign up, then create one or more business profiles. Each operates independently with its own data.',
    icon: Building2,
  },
  {
    step: '02',
    title: 'Setup Your Structure',
    desc: 'Add members, invite staff with specific roles, and configure subscription plans tailored to your operations.',
    icon: Layers,
  },
  {
    step: '03',
    title: 'Operate & Grow',
    desc: 'Record payments, assign subscriptions, track expirations. Your dashboard reflects only real, live data.',
    icon: TrendingUp,
  },
]

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] } },
}

const stagger = {
  hidden: { opacity: 0 },
  visible: { opacity: 1, transition: { staggerChildren: 0.1 } },
}

export default function LandingPage() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [scrolled, setScrolled] = useState(false)
  const heroRef = useRef<HTMLDivElement>(null)
  const { scrollYProgress } = useScroll({ target: heroRef, offset: ['start start', 'end start'] })
  const heroY = useTransform(scrollYProgress, [0, 1], ['0%', '30%'])
  const heroOpacity = useTransform(scrollYProgress, [0, 0.8], [1, 0])

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20)
    window.addEventListener('scroll', onScroll)
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return (
    <div className="min-h-screen bg-[#0a0b14] text-white overflow-x-hidden">
      {/* Background mesh */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/4 w-[500px] h-[500px] bg-indigo-600/[0.07] rounded-full blur-[150px]" />
        <div className="absolute top-1/3 right-1/4 w-[400px] h-[400px] bg-violet-600/[0.05] rounded-full blur-[130px]" />
        <div className="absolute bottom-1/4 left-1/3 w-[350px] h-[350px] bg-cyan-600/[0.04] rounded-full blur-[120px]" />
        {/* Subtle grid */}
        <div
          className="absolute inset-0 opacity-[0.02]"
          style={{
            backgroundImage:
              'linear-gradient(rgba(255,255,255,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.06) 1px, transparent 1px)',
            backgroundSize: '80px 80px',
          }}
        />
      </div>

      {/* ── Navbar ───────────────────────────────────────────── */}
      <motion.nav
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6 }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled ? 'bg-[#0a0b14]/90 backdrop-blur-xl border-b border-white/[0.06]' : ''
        }`}
      >
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/25 group-hover:shadow-indigo-500/40 transition-shadow">
              <Building2 className="w-4 h-4 text-white" />
            </div>
            <span className="text-lg font-display font-700 tracking-tight">Managio</span>
          </Link>

          {/* Desktop links */}
          <div className="hidden md:flex items-center gap-8">
            {NAV_LINKS.map(link => (
              <a
                key={link.href}
                href={link.href}
                className="text-sm text-white/50 hover:text-white transition-colors duration-200"
              >
                {link.label}
              </a>
            ))}
          </div>

          {/* CTAs */}
          <div className="hidden md:flex items-center gap-3">
            <Link
              href="/login"
              className="text-sm text-white/60 hover:text-white transition-colors px-4 py-2 rounded-lg hover:bg-white/[0.04]"
            >
              Sign In
            </Link>
            <Link
              href="/register"
              className="text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2.5 rounded-xl transition-all duration-200 shadow-lg shadow-indigo-600/25 hover:shadow-indigo-500/35 hover:-translate-y-px"
            >
              Get Started
            </Link>
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden text-white/60 hover:text-white p-2 rounded-lg hover:bg-white/[0.04]"
            onClick={() => setMobileOpen(!mobileOpen)}
            aria-label="Toggle menu"
          >
            {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
          </button>
        </div>

        {/* Mobile menu */}
        <AnimatePresence>
          {mobileOpen && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
              className="md:hidden bg-[#0a0b14]/95 backdrop-blur-xl border-b border-white/[0.06] overflow-hidden"
            >
              <div className="px-6 py-4 space-y-1">
                {NAV_LINKS.map(link => (
                  <a
                    key={link.href}
                    href={link.href}
                    onClick={() => setMobileOpen(false)}
                    className="block text-white/60 hover:text-white text-sm py-2.5 rounded-lg px-3 hover:bg-white/[0.04] transition-all"
                  >
                    {link.label}
                  </a>
                ))}
                <div className="pt-3 flex flex-col gap-2 border-t border-white/[0.06]">
                  <Link href="/login" className="text-center text-sm text-white/60 py-2.5 rounded-lg hover:bg-white/[0.04]">
                    Sign In
                  </Link>
                  <Link href="/register" className="text-center text-sm bg-indigo-600 text-white py-2.5 rounded-xl font-medium">
                    Get Started
                  </Link>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.nav>

      {/* ── Hero Section ─────────────────────────────────────── */}
      <section ref={heroRef} className="relative min-h-screen flex flex-col items-center justify-center pt-16 px-6">
        <motion.div style={{ y: heroY, opacity: heroOpacity }} className="relative z-10 max-w-5xl mx-auto text-center">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-indigo-500/25 bg-indigo-500/[0.08] text-sm text-indigo-300 mb-8"
          >
            <Layers className="w-3.5 h-3.5" />
            <span>Multi-Tenant Business Management Platform</span>
          </motion.div>

          {/* Headline */}
          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
            className="text-4xl sm:text-5xl md:text-7xl lg:text-8xl font-display font-800 leading-[1.05] tracking-tight mb-6"
          >
            One Platform.
            <br />
            <span className="relative">
              <span className="gradient-text">Every Business.</span>
              <motion.div
                initial={{ scaleX: 0 }}
                animate={{ scaleX: 1 }}
                transition={{ duration: 1, delay: 1, ease: [0.22, 1, 0.36, 1] }}
                className="absolute -bottom-2 left-0 right-0 h-[3px] bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full origin-left"
              />
            </span>
          </motion.h1>

          {/* Supporting text */}
          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.4 }}
            className="text-base sm:text-lg md:text-xl text-white/45 max-w-2xl mx-auto mb-10 leading-relaxed"
          >
            Manage multiple businesses from one account. Members, staff with
            role-based access, subscriptions, payments, and analytics — all in
            one place.
          </motion.p>

          {/* CTAs */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.5 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16"
          >
            <Link
              href="/register"
              className="group flex items-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-2xl transition-all duration-300 shadow-2xl shadow-indigo-600/30 hover:shadow-indigo-500/40 hover:-translate-y-0.5"
            >
              Start Building
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
            <a
              href="#features"
              className="flex items-center gap-2 px-8 py-4 border border-white/10 hover:border-white/20 text-white/70 hover:text-white font-medium rounded-2xl transition-all duration-200 hover:bg-white/[0.03]"
            >
              Explore Features
              <ChevronRight className="w-4 h-4" />
            </a>
          </motion.div>

          {/* Dashboard preview (illustrative, not fake data) */}
          <motion.div
            initial={{ opacity: 0, y: 60, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 1, delay: 0.7, ease: [0.22, 1, 0.36, 1] }}
            className="relative mx-auto max-w-5xl"
          >
            <div className="absolute -inset-px bg-gradient-to-r from-indigo-500/20 via-violet-500/15 to-cyan-500/10 rounded-2xl blur-sm" />
            <div className="relative glass-strong rounded-2xl overflow-hidden shadow-2xl shadow-black/50">
              {/* Browser bar */}
              <div className="flex items-center gap-2 px-4 py-3 bg-white/[0.02] border-b border-white/[0.06]">
                <div className="flex gap-1.5">
                  <div className="w-2.5 h-2.5 rounded-full bg-white/10" />
                  <div className="w-2.5 h-2.5 rounded-full bg-white/10" />
                  <div className="w-2.5 h-2.5 rounded-full bg-white/10" />
                </div>
                <div className="flex-1 mx-4 bg-white/[0.04] rounded-lg h-6 flex items-center px-3">
                  <span className="text-[10px] text-white/20 font-mono">app.managio.in/dashboard</span>
                </div>
              </div>

              {/* Dashboard layout */}
              <div className="flex bg-[#0c0d18]">
                {/* Sidebar mock */}
                <div className="hidden sm:flex flex-col w-48 border-r border-white/[0.04] p-3 gap-1">
                  <div className="flex items-center gap-2 px-2 py-2 mb-2">
                    <div className="w-6 h-6 rounded-md bg-indigo-600/30" />
                    <div className="h-3 w-16 bg-white/10 rounded" />
                  </div>
                  {['Dashboard', 'Members', 'Staff', 'Payments', 'Plans'].map((label, i) => (
                    <div
                      key={label}
                      className={`flex items-center gap-2 px-2.5 py-2 rounded-lg ${
                        i === 0 ? 'bg-indigo-500/10 border border-indigo-500/15' : ''
                      }`}
                    >
                      <div className={`w-3.5 h-3.5 rounded ${i === 0 ? 'bg-indigo-500/40' : 'bg-white/[0.08]'}`} />
                      <span className={`text-[10px] ${i === 0 ? 'text-indigo-300' : 'text-white/25'}`}>{label}</span>
                    </div>
                  ))}
                </div>

                {/* Main content */}
                <div className="flex-1 p-4 sm:p-5">
                  {/* Stats row */}
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 sm:gap-3 mb-4">
                    {[
                      { label: 'Total Members', color: 'border-indigo-500/15' },
                      { label: 'Active Subs', color: 'border-emerald-500/15' },
                      { label: 'Revenue', color: 'border-amber-500/15' },
                      { label: 'Expiring', color: 'border-pink-500/15' },
                    ].map((stat, i) => (
                      <motion.div
                        key={stat.label}
                        initial={{ opacity: 0, y: 10 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 1 + i * 0.1 }}
                        className={`bg-white/[0.02] rounded-xl p-3 border ${stat.color}`}
                      >
                        <div className="text-[9px] text-white/30 mb-1">{stat.label}</div>
                        <div className="h-4 w-12 bg-white/[0.06] rounded" />
                      </motion.div>
                    ))}
                  </div>

                  {/* Charts area */}
                  <div className="grid grid-cols-3 gap-2 sm:gap-3">
                    <div className="col-span-2 bg-white/[0.02] rounded-xl p-3 border border-white/[0.04] h-24 sm:h-28">
                      <div className="text-[9px] text-white/30 mb-2">Revenue Trend</div>
                      <div className="flex items-end gap-[3px] h-14 sm:h-16">
                        {[35, 55, 45, 65, 50, 80, 70, 90, 75, 95, 82, 100].map((h, i) => (
                          <motion.div
                            key={i}
                            initial={{ scaleY: 0 }}
                            animate={{ scaleY: 1 }}
                            transition={{ delay: 1.3 + i * 0.04, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                            className="flex-1 bg-gradient-to-t from-indigo-600/40 to-indigo-400/20 rounded-t-sm origin-bottom"
                            style={{ height: `${h}%` }}
                          />
                        ))}
                      </div>
                    </div>
                    <div className="bg-white/[0.02] rounded-xl p-3 border border-white/[0.04] h-24 sm:h-28">
                      <div className="text-[9px] text-white/30 mb-2">Methods</div>
                      <div className="space-y-2 mt-3">
                        {[
                          { w: '60%', color: 'bg-indigo-500/40' },
                          { w: '40%', color: 'bg-amber-500/40' },
                          { w: '25%', color: 'bg-emerald-500/40' },
                        ].map((m, i) => (
                          <div key={i} className="flex items-center gap-2">
                            <div className="h-2 w-5 bg-white/[0.06] rounded" />
                            <div className="flex-1 h-1.5 bg-white/[0.04] rounded-full overflow-hidden">
                              <motion.div
                                initial={{ width: 0 }}
                                animate={{ width: m.w }}
                                transition={{ delay: 1.5 + i * 0.1, duration: 0.8 }}
                                className={`h-full ${m.color} rounded-full`}
                              />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        </motion.div>

        {/* Scroll indicator */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 2 }}
          className="absolute bottom-8 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2"
        >
          <span className="text-[11px] text-white/20 tracking-wider uppercase">Scroll</span>
          <motion.div
            animate={{ y: [0, 6, 0] }}
            transition={{ repeat: Infinity, duration: 1.5 }}
            className="w-5 h-8 rounded-full border border-white/15 flex items-start justify-center pt-1.5"
          >
            <div className="w-1 h-2 bg-white/30 rounded-full" />
          </motion.div>
        </motion.div>
      </section>

      {/* ── Architecture Overview ─────────────────────────────── */}
      <section className="py-24 px-6 border-y border-white/[0.04]">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-white/[0.08] bg-white/[0.03] text-[11px] text-white/40 uppercase tracking-wider mb-4">
              Architecture
            </div>
            <h2 className="text-3xl sm:text-4xl md:text-5xl font-display font-800 mb-3">
              How Managio Works
            </h2>
            <p className="text-white/40 text-sm sm:text-base max-w-lg mx-auto">
              One account. Multiple businesses. Complete operational control.
            </p>
          </motion.div>

          {/* Architecture diagram */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="relative max-w-3xl mx-auto mb-20"
          >
            <div className="p-6 sm:p-8 rounded-2xl border border-white/[0.06] bg-white/[0.02]">
              {/* Owner level */}
              <div className="text-center mb-6">
                <div className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-indigo-500/10 border border-indigo-500/20">
                  <Lock className="w-4 h-4 text-indigo-400" />
                  <span className="text-sm font-medium text-indigo-300">Owner Account</span>
                </div>
              </div>

              {/* Connection lines */}
              <div className="flex justify-center mb-4">
                <div className="w-px h-8 bg-gradient-to-b from-indigo-500/30 to-white/10" />
              </div>

              {/* Businesses */}
              <div className="grid sm:grid-cols-2 gap-3">
                {['Business A', 'Business B'].map((name, i) => (
                  <div key={name} className="rounded-xl border border-white/[0.06] bg-white/[0.02] p-4">
                    <div className="flex items-center gap-2 mb-3">
                      <div className={`w-7 h-7 rounded-lg ${i === 0 ? 'bg-violet-500/15' : 'bg-cyan-500/15'} flex items-center justify-center`}>
                        <Building2 className={`w-3.5 h-3.5 ${i === 0 ? 'text-violet-400' : 'text-cyan-400'}`} />
                      </div>
                      <span className="text-sm font-medium text-white/80">{name}</span>
                    </div>
                    <div className="grid grid-cols-2 gap-1.5">
                      {[
                        { label: 'Staff', icon: Shield },
                        { label: 'Members', icon: Users },
                        { label: 'Plans', icon: Calendar },
                        { label: 'Payments', icon: CreditCard },
                      ].map(item => (
                        <div key={item.label} className="flex items-center gap-1.5 px-2 py-1.5 rounded-lg bg-white/[0.03] text-[10px] text-white/40">
                          <item.icon className="w-2.5 h-2.5" />
                          {item.label}
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* ── Features ─────────────────────────────────────────── */}
      <section id="features" className="py-28 sm:py-32 px-6">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-indigo-500/20 bg-indigo-500/[0.06] text-sm text-indigo-300 mb-4">
              <Zap className="w-3.5 h-3.5" />
              Capabilities
            </div>
            <h2 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-display font-800 mb-4">
              Built for Real
              <br />
              <span className="gradient-text">Business Operations</span>
            </h2>
            <p className="text-white/40 text-sm sm:text-base lg:text-lg max-w-xl mx-auto">
              Every feature is functional and production-ready. No simulated data, no decorative UI.
            </p>
          </motion.div>

          <motion.div
            variants={stagger}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-50px' }}
            className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-5"
          >
            {FEATURES.map((feature) => (
              <motion.div
                key={feature.title}
                variants={fadeUp}
                whileHover={{ y: -4, transition: { duration: 0.2 } }}
                className="group relative p-5 sm:p-6 rounded-2xl border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.04] transition-all duration-300 cursor-default overflow-hidden"
              >
                <div
                  className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl"
                  style={{ background: `radial-gradient(circle at 50% 0%, ${feature.glow}, transparent 70%)` }}
                />
                <div className={`relative w-10 h-10 rounded-xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4 shadow-lg`}>
                  <feature.icon className="w-5 h-5 text-white" />
                </div>
                <h3 className="font-display font-600 text-base sm:text-lg mb-2 relative">{feature.title}</h3>
                <p className="text-white/40 text-sm leading-relaxed relative">{feature.description}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* ── How it Works ─────────────────────────────────────── */}
      <section id="how-it-works" className="py-28 sm:py-32 px-6 border-y border-white/[0.04]">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-display font-800 mb-4">
              Get Started in
              <br />
              <span className="gradient-text">Three Steps</span>
            </h2>
            <p className="text-white/40 text-sm sm:text-base max-w-md mx-auto">
              From signup to full operations in minutes.
            </p>
          </motion.div>

          <div className="grid sm:grid-cols-3 gap-6 sm:gap-8">
            {ARCHITECTURE.map((item, i) => (
              <motion.div
                key={item.step}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.15 }}
                className="relative"
              >
                <div className="text-5xl sm:text-6xl font-display font-800 gradient-text opacity-15 mb-4">{item.step}</div>
                <div className="w-10 h-10 rounded-xl bg-indigo-500/15 border border-indigo-500/20 flex items-center justify-center mb-4">
                  <item.icon className="w-5 h-5 text-indigo-400" />
                </div>
                <h3 className="text-lg sm:text-xl font-display font-700 mb-2">{item.title}</h3>
                <p className="text-white/40 text-sm leading-relaxed">{item.desc}</p>
                {i < 2 && (
                  <div className="hidden sm:block absolute top-14 -right-4 w-8 h-px bg-gradient-to-r from-indigo-500/30 to-transparent" />
                )}
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ── Who It's For ─────────────────────────────────────── */}
      <section className="py-28 sm:py-32 px-6">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-14"
          >
            <h2 className="text-3xl sm:text-4xl md:text-5xl font-display font-800 mb-4">
              Who Is <span className="gradient-text">Managio</span> For?
            </h2>
          </motion.div>

          <motion.div
            variants={stagger}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="grid sm:grid-cols-2 lg:grid-cols-3 gap-4"
          >
            {[
              {
                title: 'Gym & Fitness Owners',
                desc: 'Track members, manage subscription plans, record payments, and assign staff roles.',
              },
              {
                title: 'Multi-Location Operators',
                desc: 'Manage multiple businesses independently from a single account with isolated data.',
              },
              {
                title: 'Studios & Academies',
                desc: 'Handle student memberships, class subscriptions, and instructor permissions.',
              },
              {
                title: 'Clubs & Communities',
                desc: 'Organize members, manage dues, track payments, and delegate tasks to staff.',
              },
              {
                title: 'Subscription Businesses',
                desc: 'Create flexible plans, auto-track expirations, and monitor renewal health.',
              },
              {
                title: 'Service Providers',
                desc: 'Any business that manages members, subscriptions, and payments with team access.',
              },
            ].map((item) => (
              <motion.div
                key={item.title}
                variants={fadeUp}
                className="p-5 rounded-2xl border border-white/[0.06] bg-white/[0.02] hover:bg-white/[0.04] transition-all duration-200"
              >
                <h3 className="text-sm font-display font-600 text-white/80 mb-1.5">{item.title}</h3>
                <p className="text-xs text-white/35 leading-relaxed">{item.desc}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* ── CTA ──────────────────────────────────────────────── */}
      <section className="py-28 sm:py-32 px-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="max-w-3xl mx-auto text-center relative"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-indigo-600/15 to-violet-600/15 rounded-3xl blur-3xl" />
          <div className="relative p-8 sm:p-12 rounded-3xl border border-indigo-500/15 bg-indigo-500/[0.03]">
            <h2 className="text-3xl sm:text-4xl md:text-5xl font-display font-800 mb-4">
              Ready to Manage
              <br />
              <span className="gradient-text">Smarter?</span>
            </h2>
            <p className="text-white/40 text-sm sm:text-base mb-8 max-w-md mx-auto">
              Create your account and set up your first business in minutes.
            </p>
            <Link
              href="/register"
              className="group inline-flex items-center justify-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-2xl transition-all shadow-2xl shadow-indigo-600/30 hover:shadow-indigo-500/40 hover:-translate-y-0.5"
            >
              Create Free Account
              <ArrowUpRight className="w-4 h-4 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform" />
            </Link>
          </div>
        </motion.div>
      </section>

      {/* ── Footer ───────────────────────────────────────────── */}
      <footer className="border-t border-white/[0.04] py-12 px-6">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center">
              <Building2 className="w-3.5 h-3.5 text-white" />
            </div>
            <span className="font-display font-700">Managio</span>
          </div>
          <div className="flex gap-6 text-sm text-white/30">
            <a href="#" className="hover:text-white/60 transition-colors">Privacy</a>
            <a href="#" className="hover:text-white/60 transition-colors">Terms</a>
            <a href="#" className="hover:text-white/60 transition-colors">Contact</a>
          </div>
          <div className="text-sm text-white/20">© {new Date().getFullYear()} Managio</div>
        </div>
      </footer>
    </div>
  )
}