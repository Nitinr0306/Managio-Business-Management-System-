'use client'

import { motion, useScroll, useTransform, AnimatePresence } from 'framer-motion'
import { useRef, useState, useEffect } from 'react'
import Link from 'next/link'
import {
  Users, CreditCard, BarChart3, Shield, Zap, Globe,
  ChevronRight, Star, ArrowRight, Check, Menu, X,
  Dumbbell, Calendar, TrendingUp, Bell, Lock, Sparkles
} from 'lucide-react'

const NAV_LINKS = [
  { label: 'Features', href: '#features' },
  { label: 'How it Works', href: '#how-it-works' },
  { label: 'Pricing', href: '#pricing' },
  { label: 'Testimonials', href: '#testimonials' },
]

const STATS = [
  { value: '50K+', label: 'Active Members' },
  { value: '2.5K+', label: 'Businesses' },
  { value: '₹120Cr+', label: 'Revenue Processed' },
  { value: '99.9%', label: 'Uptime' },
]

const FEATURES = [
  {
    icon: Users,
    title: 'Member Management',
    description: 'Track every member with detailed profiles, subscription history, payment records, and health notes. Import via CSV or add individually.',
    color: 'from-indigo-500 to-violet-500',
    glow: 'rgba(99,102,241,0.3)',
  },
  {
    icon: CreditCard,
    title: 'Payments & Revenue',
    description: 'Record payments across UPI, cash, card, and more. Real-time revenue dashboards with monthly breakdowns and trend analysis.',
    color: 'from-emerald-500 to-teal-500',
    glow: 'rgba(16,185,129,0.3)',
  },
  {
    icon: Calendar,
    title: 'Subscription Plans',
    description: 'Create flexible plans — daily, weekly, monthly, or yearly. Auto-track expirations and get alerts before members churn.',
    color: 'from-amber-500 to-orange-500',
    glow: 'rgba(245,158,11,0.3)',
  },
  {
    icon: Shield,
    title: 'Staff & Permissions',
    description: 'Role-based access control. Invite staff via email, assign granular permissions, track every action with full audit logs.',
    color: 'from-pink-500 to-rose-500',
    glow: 'rgba(236,72,153,0.3)',
  },
  {
    icon: BarChart3,
    title: 'Analytics Dashboard',
    description: 'Visual charts for revenue growth, member acquisition, subscription health, and payment method breakdown — all in one view.',
    color: 'from-cyan-500 to-blue-500',
    glow: 'rgba(6,182,212,0.3)',
  },
  {
    icon: Bell,
    title: 'Smart Alerts',
    description: 'Automated alerts for expiring subscriptions, overdue payments, and staff activity. Never miss a follow-up again.',
    color: 'from-purple-500 to-indigo-500',
    glow: 'rgba(168,85,247,0.3)',
  },
]

const TESTIMONIALS = [
  {
    name: 'Arjun Sharma',
    role: 'Owner, FitZone Gym, Mumbai',
    content: 'Managio transformed how we run our gym. We went from scattered Excel sheets to a fully automated system in one day. Revenue tracking alone saved us 5 hours a week.',
    rating: 5,
    avatar: 'AS',
  },
  {
    name: 'Priya Nair',
    role: 'Studio Director, Rhythm Dance Academy, Bangalore',
    content: "The staff permission system is brilliant. My managers can handle day-to-day without seeing financials. The audit trail gives me complete peace of mind.",
    rating: 5,
    avatar: 'PN',
  },
  {
    name: 'Vikram Mehta',
    role: 'Founder, IronCore Studios, Delhi',
    content: 'Subscription expiry alerts alone are worth the price. We recovered 40+ lapsing members last month just from the 7-day reminder list.',
    rating: 5,
    avatar: 'VM',
  },
]

const PRICING = [
  {
    name: 'Starter',
    price: '₹999',
    period: '/month',
    description: 'Perfect for solo trainers and small studios',
    features: ['Up to 100 members', '1 business', '2 staff accounts', 'Basic analytics', 'Email support'],
    cta: 'Start Free Trial',
    highlighted: false,
  },
  {
    name: 'Growth',
    price: '₹2,999',
    period: '/month',
    description: 'Built for growing gyms and fitness chains',
    features: ['Unlimited members', '3 businesses', 'Unlimited staff', 'Advanced analytics', 'Priority support', 'Custom plans', 'CSV import/export', 'Audit logs'],
    cta: 'Start Free Trial',
    highlighted: true,
    badge: 'Most Popular',
  },
  {
    name: 'Enterprise',
    price: 'Custom',
    period: '',
    description: 'For franchise networks and large operators',
    features: ['Everything in Growth', 'Unlimited businesses', 'White-label option', 'API access', 'Dedicated support', 'Custom integrations', 'SLA guarantee'],
    cta: 'Contact Sales',
    highlighted: false,
  },
]

function CounterAnimation({ value, suffix = '' }: { value: string; suffix?: string }) {
  const [count, setCount] = useState('0')
  const [hasAnimated, setHasAnimated] = useState(false)
  const ref = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !hasAnimated) {
        setHasAnimated(true)
        const numMatch = value.match(/[\d.]+/)
        if (!numMatch) { setCount(value); return }
        const target = parseFloat(numMatch[0])
        const isDecimal = value.includes('.')
        const prefix = value.replace(/[\d.]+.*/, '')
        const rest = value.replace(/^[^0-9]*[\d.]+/, '')
        let start = 0
        const duration = 2000
        const step = target / (duration / 16)
        const timer = setInterval(() => {
          start += step
          if (start >= target) {
            setCount(value)
            clearInterval(timer)
          } else {
            setCount(prefix + (isDecimal ? start.toFixed(1) : Math.floor(start).toString()) + rest)
          }
        }, 16)
      }
    }, { threshold: 0.5 })
    if (ref.current) observer.observe(ref.current)
    return () => observer.disconnect()
  }, [value, hasAnimated])

  return <span ref={ref}>{count}</span>
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

  const stagger = {
    hidden: { opacity: 0 },
    visible: { opacity: 1, transition: { staggerChildren: 0.1 } },
  }
  const fadeUp = {
    hidden: { opacity: 0, y: 24 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] } },
  }

  return (
    <div className="min-h-screen bg-[#070710] text-white overflow-x-hidden">
      {/* Background mesh */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-indigo-600/10 rounded-full blur-[120px]" />
        <div className="absolute top-1/3 right-1/4 w-80 h-80 bg-violet-600/8 rounded-full blur-[100px]" />
        <div className="absolute bottom-1/4 left-1/3 w-72 h-72 bg-cyan-600/6 rounded-full blur-[100px]" />
      </div>

      {/* Navbar */}
      <motion.nav
        initial={{ y: -20, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ duration: 0.6 }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
          scrolled ? 'bg-[#070710]/90 backdrop-blur-xl border-b border-white/5' : ''
        }`}
      >
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Dumbbell className="w-4 h-4 text-white" />
            </div>
            <span className="text-lg font-display font-700 tracking-tight">Managio</span>
          </Link>

          {/* Desktop links */}
          <div className="hidden md:flex items-center gap-8">
            {NAV_LINKS.map(link => (
              <a
                key={link.href}
                href={link.href}
                className="text-sm text-white/60 hover:text-white transition-colors duration-200"
              >
                {link.label}
              </a>
            ))}
          </div>

          {/* CTAs */}
          <div className="hidden md:flex items-center gap-3">
            <Link href="/login" className="text-sm text-white/70 hover:text-white transition-colors px-4 py-2">
              Sign In
            </Link>
            <Link
              href="/register"
              className="text-sm font-medium bg-indigo-600 hover:bg-indigo-500 text-white px-5 py-2 rounded-lg transition-all duration-200 shadow-lg shadow-indigo-600/30"
            >
              Get Started Free
            </Link>
          </div>

          {/* Mobile menu button */}
          <button
            className="md:hidden text-white/70 hover:text-white"
            onClick={() => setMobileOpen(!mobileOpen)}
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
              className="md:hidden bg-[#070710]/95 backdrop-blur-xl border-b border-white/5 overflow-hidden"
            >
              <div className="px-6 py-4 space-y-3">
                {NAV_LINKS.map(link => (
                  <a key={link.href} href={link.href} className="block text-white/70 hover:text-white text-sm py-2">
                    {link.label}
                  </a>
                ))}
                <div className="pt-3 flex flex-col gap-2 border-t border-white/5">
                  <Link href="/login" className="text-center text-sm text-white/70 py-2">Sign In</Link>
                  <Link href="/register" className="text-center text-sm bg-indigo-600 text-white py-2.5 rounded-lg">Get Started</Link>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.nav>

      {/* Hero Section */}
      <section ref={heroRef} className="relative min-h-screen flex flex-col items-center justify-center pt-16 px-6">
        <motion.div style={{ y: heroY, opacity: heroOpacity }} className="relative z-10 max-w-5xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-indigo-500/30 bg-indigo-500/10 text-sm text-indigo-300 mb-8"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span>All-in-one Business Management Platform</span>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
            className="text-5xl md:text-7xl lg:text-8xl font-display font-800 leading-[1.05] tracking-tight mb-6"
          >
            Run Your Gym
            <br />
            <span className="relative">
              <span className="gradient-text">Like a Business</span>
              <motion.div
                initial={{ scaleX: 0 }}
                animate={{ scaleX: 1 }}
                transition={{ duration: 1, delay: 1, ease: [0.22, 1, 0.36, 1] }}
                className="absolute -bottom-2 left-0 right-0 h-1 bg-gradient-to-r from-indigo-500 to-violet-500 rounded-full origin-left"
              />
            </span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.4 }}
            className="text-lg md:text-xl text-white/50 max-w-2xl mx-auto mb-10 leading-relaxed"
          >
            Managio brings members, staff, subscriptions, and payments together in one powerful platform. Built for gyms, studios, and any growing small business.
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.5 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 mb-16"
          >
            <Link
              href="/register"
              className="group flex items-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition-all duration-300 shadow-2xl shadow-indigo-600/40 hover:shadow-indigo-500/50 hover:-translate-y-0.5"
            >
              Start Free — No Credit Card
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
            <Link
              href="#features"
              className="flex items-center gap-2 px-8 py-4 border border-white/10 hover:border-white/20 text-white/80 hover:text-white font-medium rounded-xl transition-all duration-200 hover:bg-white/5"
            >
              See Features
              <ChevronRight className="w-4 h-4" />
            </Link>
          </motion.div>

          {/* Dashboard Preview */}
          <motion.div
            initial={{ opacity: 0, y: 60, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            transition={{ duration: 1, delay: 0.7, ease: [0.22, 1, 0.36, 1] }}
            className="relative mx-auto max-w-5xl"
          >
            <div className="absolute -inset-px bg-gradient-to-r from-indigo-500/30 via-violet-500/20 to-cyan-500/20 rounded-2xl blur-sm" />
            <div className="relative glass-strong rounded-2xl overflow-hidden border border-white/10 shadow-2xl">
              {/* Fake browser bar */}
              <div className="flex items-center gap-2 px-4 py-3 bg-white/3 border-b border-white/5">
                <div className="w-3 h-3 rounded-full bg-red-500/60" />
                <div className="w-3 h-3 rounded-full bg-amber-500/60" />
                <div className="w-3 h-3 rounded-full bg-emerald-500/60" />
                <div className="flex-1 mx-4 bg-white/5 rounded-md h-6 flex items-center px-3">
                  <span className="text-xs text-white/30">app.managio.in/dashboard</span>
                </div>
              </div>
              {/* Fake dashboard UI */}
              <div className="p-6 bg-[#0a0a14]">
                <div className="grid grid-cols-4 gap-3 mb-4">
                  {[
                    { label: 'Total Members', value: '1,247', change: '+12%', color: 'text-indigo-400' },
                    { label: 'Active Subscriptions', value: '989', change: '+8%', color: 'text-emerald-400' },
                    { label: 'Monthly Revenue', value: '₹2.4L', change: '+23%', color: 'text-amber-400' },
                    { label: 'Expiring (7d)', value: '34', change: '-5%', color: 'text-pink-400' },
                  ].map((stat, i) => (
                    <motion.div
                      key={i}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 1 + i * 0.1 }}
                      className="bg-white/4 rounded-xl p-3 border border-white/5"
                    >
                      <div className="text-xs text-white/40 mb-1">{stat.label}</div>
                      <div className={`text-lg font-display font-700 ${stat.color}`}>{stat.value}</div>
                      <div className="text-xs text-emerald-400 mt-0.5">{stat.change}</div>
                    </motion.div>
                  ))}
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div className="col-span-2 bg-white/4 rounded-xl p-3 border border-white/5 h-28">
                    <div className="text-xs text-white/40 mb-2">Revenue Trend</div>
                    <div className="flex items-end gap-1 h-16">
                      {[40, 65, 55, 75, 60, 90, 80, 95, 85, 100, 88, 105].map((h, i) => (
                        <motion.div
                          key={i}
                          initial={{ scaleY: 0 }}
                          animate={{ scaleY: 1 }}
                          transition={{ delay: 1.3 + i * 0.05 }}
                          className="flex-1 bg-gradient-to-t from-indigo-600/60 to-indigo-400/40 rounded-t-sm origin-bottom"
                          style={{ height: `${h}%` }}
                        />
                      ))}
                    </div>
                  </div>
                  <div className="bg-white/4 rounded-xl p-3 border border-white/5 h-28">
                    <div className="text-xs text-white/40 mb-2">Payment Methods</div>
                    <div className="space-y-1.5 mt-3">
                      {[{ label: 'UPI', pct: 45, color: 'bg-indigo-500' }, { label: 'Cash', pct: 30, color: 'bg-amber-500' }, { label: 'Card', pct: 25, color: 'bg-emerald-500' }].map(m => (
                        <div key={m.label} className="flex items-center gap-2">
                          <div className="text-xs text-white/50 w-8">{m.label}</div>
                          <div className="flex-1 h-1.5 bg-white/5 rounded-full overflow-hidden">
                            <motion.div
                              initial={{ width: 0 }}
                              animate={{ width: `${m.pct}%` }}
                              transition={{ delay: 1.5, duration: 0.8 }}
                              className={`h-full ${m.color} rounded-full`}
                            />
                          </div>
                          <div className="text-xs text-white/40">{m.pct}%</div>
                        </div>
                      ))}
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
          <span className="text-xs text-white/30">Scroll to explore</span>
          <motion.div
            animate={{ y: [0, 6, 0] }}
            transition={{ repeat: Infinity, duration: 1.5 }}
            className="w-5 h-8 rounded-full border border-white/20 flex items-start justify-center pt-1.5"
          >
            <div className="w-1 h-2 bg-white/40 rounded-full" />
          </motion.div>
        </motion.div>
      </section>

      {/* Stats */}
      <section className="py-20 px-6 border-y border-white/5 bg-white/[0.01]">
        <div className="max-w-5xl mx-auto">
          <motion.div
            variants={stagger}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true }}
            className="grid grid-cols-2 md:grid-cols-4 gap-8"
          >
            {STATS.map((stat) => (
              <motion.div key={stat.label} variants={fadeUp} className="text-center">
                <div className="text-4xl md:text-5xl font-display font-800 gradient-text mb-2">
                  <CounterAnimation value={stat.value} />
                </div>
                <div className="text-sm text-white/50">{stat.label}</div>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* Features */}
      <section id="features" className="py-32 px-6">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <div className="inline-flex items-center gap-2 px-4 py-1.5 rounded-full border border-indigo-500/20 bg-indigo-500/8 text-sm text-indigo-300 mb-4">
              <Zap className="w-3.5 h-3.5" />
              Everything You Need
            </div>
            <h2 className="text-4xl md:text-6xl font-display font-800 mb-4">
              Built for Real
              <br />
              <span className="gradient-text">Business Owners</span>
            </h2>
            <p className="text-white/50 text-lg max-w-xl mx-auto">
              No fluff. Every feature was designed based on what gym owners and studio managers actually need.
            </p>
          </motion.div>

          <motion.div
            variants={stagger}
            initial="hidden"
            whileInView="visible"
            viewport={{ once: true, margin: '-50px' }}
            className="grid md:grid-cols-2 lg:grid-cols-3 gap-5"
          >
            {FEATURES.map((feature) => (
              <motion.div
                key={feature.title}
                variants={fadeUp}
                whileHover={{ y: -4, transition: { duration: 0.2 } }}
                className="group relative p-6 rounded-2xl border border-white/6 bg-white/[0.02] hover:bg-white/[0.04] transition-all duration-300 cursor-default overflow-hidden"
              >
                <div
                  className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl"
                  style={{ background: `radial-gradient(circle at 50% 0%, ${feature.glow}, transparent 70%)` }}
                />
                <div className={`relative w-10 h-10 rounded-xl bg-gradient-to-br ${feature.color} flex items-center justify-center mb-4 shadow-lg`}>
                  <feature.icon className="w-5 h-5 text-white" />
                </div>
                <h3 className="font-display font-600 text-lg mb-2 relative">{feature.title}</h3>
                <p className="text-white/50 text-sm leading-relaxed relative">{feature.description}</p>
              </motion.div>
            ))}
          </motion.div>
        </div>
      </section>

      {/* How it works */}
      <section id="how-it-works" className="py-32 px-6 border-y border-white/5">
        <div className="max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-6xl font-display font-800 mb-4">
              Up and Running
              <br />
              <span className="gradient-text">in 5 Minutes</span>
            </h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            {[
              { step: '01', title: 'Create Your Business', desc: 'Sign up and set up your gym or studio profile. Add your logo, contact info, and business type.', icon: Globe },
              { step: '02', title: 'Add Members & Plans', desc: 'Import existing members via CSV or add them one by one. Create subscription plans matching your pricing.', icon: Users },
              { step: '03', title: 'Track & Grow', desc: 'Record payments, assign subscriptions, invite staff, and watch your dashboard light up with insights.', icon: TrendingUp },
            ].map((item, i) => (
              <motion.div
                key={item.step}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.15 }}
                className="relative"
              >
                <div className="text-6xl font-display font-800 gradient-text opacity-20 mb-4">{item.step}</div>
                <div className="w-10 h-10 rounded-xl bg-indigo-500/20 flex items-center justify-center mb-4">
                  <item.icon className="w-5 h-5 text-indigo-400" />
                </div>
                <h3 className="text-xl font-display font-700 mb-2">{item.title}</h3>
                <p className="text-white/50 text-sm leading-relaxed">{item.desc}</p>
                {i < 2 && (
                  <div className="hidden md:block absolute top-16 -right-4 w-8 h-0.5 bg-gradient-to-r from-indigo-500/50 to-transparent" />
                )}
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Pricing */}
      <section id="pricing" className="py-32 px-6">
        <div className="max-w-6xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-6xl font-display font-800 mb-4">
              Simple, Transparent
              <br />
              <span className="gradient-text">Pricing</span>
            </h2>
            <p className="text-white/50 max-w-md mx-auto">Start free for 14 days. No credit card required. Cancel anytime.</p>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-6">
            {PRICING.map((plan, i) => (
              <motion.div
                key={plan.name}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className={`relative rounded-2xl p-6 border ${
                  plan.highlighted
                    ? 'border-indigo-500/50 bg-indigo-500/5 shadow-2xl shadow-indigo-500/20'
                    : 'border-white/8 bg-white/[0.02]'
                }`}
              >
                {plan.badge && (
                  <div className="absolute -top-3 left-1/2 -translate-x-1/2 px-3 py-1 bg-indigo-600 text-white text-xs font-medium rounded-full">
                    {plan.badge}
                  </div>
                )}
                <div className="mb-6">
                  <h3 className="text-xl font-display font-700 mb-1">{plan.name}</h3>
                  <p className="text-white/40 text-sm mb-4">{plan.description}</p>
                  <div className="flex items-baseline gap-1">
                    <span className="text-4xl font-display font-800 gradient-text">{plan.price}</span>
                    <span className="text-white/40 text-sm">{plan.period}</span>
                  </div>
                </div>
                <ul className="space-y-2.5 mb-8">
                  {plan.features.map(f => (
                    <li key={f} className="flex items-center gap-2.5 text-sm text-white/70">
                      <Check className="w-4 h-4 text-emerald-400 flex-shrink-0" />
                      {f}
                    </li>
                  ))}
                </ul>
                <Link
                  href="/register"
                  className={`block text-center py-3 rounded-xl font-medium text-sm transition-all duration-200 ${
                    plan.highlighted
                      ? 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30'
                      : 'border border-white/10 hover:border-white/20 text-white hover:bg-white/5'
                  }`}
                >
                  {plan.cta}
                </Link>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section id="testimonials" className="py-32 px-6 border-t border-white/5">
        <div className="max-w-6xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-4xl md:text-6xl font-display font-800 mb-4">
              Loved by
              <br />
              <span className="gradient-text">Business Owners</span>
            </h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-6">
            {TESTIMONIALS.map((t, i) => (
              <motion.div
                key={t.name}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="p-6 rounded-2xl border border-white/6 bg-white/[0.02]"
              >
                <div className="flex gap-0.5 mb-4">
                  {[...Array(t.rating)].map((_, j) => (
                    <Star key={j} className="w-4 h-4 text-amber-400 fill-amber-400" />
                  ))}
                </div>
                <p className="text-white/70 text-sm leading-relaxed mb-6">"{t.content}"</p>
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-full bg-indigo-600/30 flex items-center justify-center text-xs font-display font-700 text-indigo-300">
                    {t.avatar}
                  </div>
                  <div>
                    <div className="text-sm font-medium">{t.name}</div>
                    <div className="text-xs text-white/40">{t.role}</div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-32 px-6">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="max-w-3xl mx-auto text-center relative"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-indigo-600/20 to-violet-600/20 rounded-3xl blur-3xl" />
          <div className="relative p-12 rounded-3xl border border-indigo-500/20 bg-indigo-500/5">
            <h2 className="text-4xl md:text-5xl font-display font-800 mb-4">
              Start Managing Smarter
              <br />
              <span className="gradient-text">Today</span>
            </h2>
            <p className="text-white/50 mb-8 max-w-md mx-auto">
              Join thousands of gym and studio owners who've simplified operations with Managio.
            </p>
            <div className="flex flex-col sm:flex-row gap-4 justify-center">
              <Link
                href="/register"
                className="group inline-flex items-center justify-center gap-2 px-8 py-4 bg-indigo-600 hover:bg-indigo-500 text-white font-medium rounded-xl transition-all shadow-2xl shadow-indigo-600/40"
              >
                Create Free Account
                <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
            <p className="text-xs text-white/30 mt-4">No credit card required • 14-day free trial • Cancel anytime</p>
          </div>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="border-t border-white/5 py-12 px-6">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-indigo-500 to-violet-600 flex items-center justify-center">
              <Dumbbell className="w-3.5 h-3.5 text-white" />
            </div>
            <span className="font-display font-700">Managio</span>
          </div>
          <div className="flex gap-6 text-sm text-white/40">
            <a href="#" className="hover:text-white/70 transition-colors">Privacy</a>
            <a href="#" className="hover:text-white/70 transition-colors">Terms</a>
            <a href="#" className="hover:text-white/70 transition-colors">Contact</a>
          </div>
          <div className="text-sm text-white/30">© 2024 Managio. All rights reserved.</div>
        </div>
      </footer>
    </div>
  )
}