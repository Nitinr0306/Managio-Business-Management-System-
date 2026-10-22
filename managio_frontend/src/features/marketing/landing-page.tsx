import {
  ArrowRight,
  BookOpen,
  Briefcase,
  ChartColumn,
  Dumbbell,
  GraduationCap,
  Inbox,
  Lock,
  Mail,
  ReceiptIndianRupee,
  School,
  ScrollText,
  Scissors,
  Search,
  ShieldCheck,
  Stethoscope,
  Store,
  Users,
  Wallet,
} from "lucide-react";
import Link from "next/link";
import type { ReactNode } from "react";
import { Logo } from "@/components/brand/logo";
import { SiteFooter } from "@/components/site/site-chrome";
import { SITE } from "@/lib/site";

/* Example figures for the illustration, labelled "Example" on screen. They are not real customers. */
const PORTFOLIO = [
  { name: "Ironwood Fitness", type: "Gym", revenue: "₹3,84,500", ending: 23, owed: "₹41,200", bars: [2, 5, 3, 6, 4, 7, 5, 2, 3, 1, 4, 2, 3, 2] },
  { name: "Glow Studio", type: "Salon", revenue: "₹1,92,300", ending: 6, owed: "₹8,900", bars: [1, 2, 1, 3, 2, 1, 2, 1, 0, 1, 2, 1, 1, 0] },
  { name: "Bright Minds", type: "Coaching centre", revenue: "₹2,47,000", ending: 14, owed: "₹62,000", bars: [3, 1, 4, 2, 6, 3, 2, 1, 2, 3, 1, 2, 4, 1] },
];

const TYPES = [
  { icon: Dumbbell, name: "Gyms and studios", words: "Members, membership plans, batches, check-ins" },
  { icon: Scissors, name: "Salons and spas", words: "Clients, packages, products and stock" },
  { icon: GraduationCap, name: "Coaching centres", words: "Students, courses, batches, attendance" },
  { icon: School, name: "Schools and academies", words: "Students, fee plans in instalments, classes" },
  { icon: Stethoscope, name: "Clinics", words: "Patients, packages, services" },
  { icon: Store, name: "Shops", words: "Customers, products, stock, counter sales" },
];

/**
 * Structured data for search engines, limited to what the page itself shows: what Managio is, that it runs in the
 * browser, and that it is free. No ratings or reviews, because there are none to show.
 */
const STRUCTURED_DATA = {
  "@context": "https://schema.org",
  "@graph": [
    { "@type": "WebSite", "@id": `${SITE.url}/#website`, name: "Managio", url: SITE.url, inLanguage: "en-IN" },
    {
      "@type": "SoftwareApplication",
      name: "Managio",
      url: SITE.url,
      applicationCategory: "BusinessApplication",
      operatingSystem: "Web browser",
      description: SITE.description,
      offers: { "@type": "Offer", price: "0", priceCurrency: "INR" },
    },
  ],
};

export function LandingPage() {
  return (
    <div className="relative bg-canvas text-ink">
      <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(STRUCTURED_DATA).replace(/</g, "\\u003c") }} />
      <a href="#content" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-50 focus:rounded focus:bg-surface focus:px-3 focus:py-2">
        Skip to content
      </a>

      {/* The header overlays the dark hero, outside <main>, so it stays the page's banner landmark. */}
      <header className="absolute inset-x-0 top-0 z-10 text-white">
        <div className="mx-auto flex h-16 max-w-[1200px] items-center justify-between px-4 sm:px-8">
          <Link href="/" aria-label="Managio home" className="rounded-md">
            <Logo tone="on-dark" size="lg" />
          </Link>
          <nav aria-label="Main" className="flex items-center gap-1 sm:gap-2">
            <a href="#how" className="hidden rounded-md px-3 py-2 text-[14px] text-white/75 hover:text-white md:block">How it works</a>
            <a href="#types" className="hidden rounded-md px-3 py-2 text-[14px] text-white/75 hover:text-white md:block">Business types</a>
            <Link href="/sign-in" className="whitespace-nowrap rounded-md px-3 py-2 text-[14px] font-medium text-white/90 hover:text-white">Sign in</Link>
            {/* On phones the hero's own button, right below, does this job; the header keeps to one line. */}
            <Link href="/sign-up" className="hidden whitespace-nowrap rounded-md bg-white px-3.5 py-2 text-[14px] font-semibold text-ink hover:bg-white/90 sm:inline-block">
              Create an account
            </Link>
          </nav>
        </div>
      </header>

      <main id="content">
        <section className="relative overflow-hidden bg-ink text-white">
          <div aria-hidden className="landing-ledger" />
          <div aria-hidden className="pointer-events-none absolute -right-40 top-20 h-[520px] w-[520px] rounded-full bg-cobalt/35 blur-[120px]" />
          <div className="relative mx-auto grid max-w-[1200px] items-center gap-12 px-4 pb-20 pt-28 sm:px-8 lg:grid-cols-[1.02fr_1fr] lg:pb-28 lg:pt-36">
            <div>
              <h1 className="text-[44px] font-bold leading-[1.02] tracking-[-0.035em] text-balance sm:text-[60px]">
                Run every business you own from one screen.
              </h1>
              <p className="mt-6 max-w-[54ch] text-[17px] leading-relaxed text-white/75 text-pretty">
                Members, renewals, payments, expenses, staff and the day&apos;s cash, for gyms, salons, coaching centres, schools, clinics
                and shops. Know who is about to lapse, who still owes you, and what is waiting for your approval, before your morning tea.
              </p>
              <div className="mt-8 flex flex-wrap gap-3">
                <Link href="/sign-up" className="inline-flex h-12 items-center gap-2 rounded-lg bg-cobalt px-5 text-[15px] font-semibold text-white hover:bg-[#3a63dd]">
                  Create your account <ArrowRight className="h-4 w-4" aria-hidden />
                </Link>
                <Link href="/sign-in" className="inline-flex h-12 items-center rounded-lg border border-white/20 px-5 text-[15px] font-medium text-white hover:bg-white/5">
                  Sign in
                </Link>
              </div>
              <p className="mt-6 text-[14px] font-medium text-white/85">Free to use. Nothing to install: it runs in your browser, on a computer or a phone.</p>
              <p className="mt-2 text-[13.5px] text-white/60">Rupees with Indian grouping, UPI and cash, GST numbers on invoices, and your own time zone.</p>
            </div>

            <PortfolioIllustration />
          </div>
        </section>

        <section id="how" className="mx-auto max-w-[1200px] px-4 py-20 sm:px-8 lg:py-24">
          <div className="max-w-[640px]">
            <h2 className="text-[32px] font-bold leading-tight tracking-tight text-balance">The front desk, in three moves</h2>
            <p className="mt-3 text-[16px] leading-relaxed text-muted">
              Most of a busy day is the same three things. Managio puts each one a keystroke away, so the queue keeps moving.
            </p>
          </div>
          <ol className="mt-12 grid gap-6 md:grid-cols-3">
            {[
              { icon: Search, title: "Find anyone", body: "Type a name, the last digits of a phone, or a member number. Their plan, dues and history open beside the list." },
              { icon: ReceiptIndianRupee, title: "Do the one thing they need", body: "If they owe money, the button records the payment. If their plan is ending, it renews it with no lost days. The receipt is numbered for you." },
              { icon: BookOpen, title: "Count the cash and close the day", body: "Managio knows what the drawer should hold. Enter what you counted, and that day's cash is locked so nobody changes it later." },
            ].map((step, index) => (
              <li key={step.title} className="relative rounded-xl border border-line bg-surface p-6">
                <span className="text-[13px] font-semibold tabular-nums text-cobalt">Step {index + 1}</span>
                <step.icon className="mt-4 h-6 w-6 text-ink" aria-hidden />
                <h3 className="mt-3 text-[18px] font-semibold">{step.title}</h3>
                <p className="mt-2 text-[14.5px] leading-relaxed text-muted">{step.body}</p>
              </li>
            ))}
          </ol>
        </section>

        <section id="types" className="border-y border-line bg-surface">
          <div className="mx-auto grid max-w-[1200px] gap-12 px-4 py-20 sm:px-8 lg:grid-cols-[0.8fr_1.2fr] lg:py-24">
            <div>
              <h2 className="text-[32px] font-bold leading-tight tracking-tight text-balance">It speaks each business&apos;s language</h2>
              <p className="mt-3 text-[16px] leading-relaxed text-muted">
                Your gym has members on membership plans. Your coaching centre has students in batches. Pick the kind of business when
                you add it, and the words, roles and expense categories follow. Each business keeps its own settings.
              </p>
            </div>
            <ul className="grid gap-px overflow-hidden rounded-xl border border-line bg-line sm:grid-cols-2">
              {TYPES.map((type) => (
                <li key={type.name} className="flex gap-4 bg-surface p-5">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-cobalt-soft text-cobalt">
                    <type.icon className="h-5 w-5" aria-hidden />
                  </span>
                  <span>
                    <span className="block text-[15px] font-semibold">{type.name}</span>
                    <span className="mt-0.5 block text-[13.5px] text-muted">{type.words}</span>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="mx-auto max-w-[1200px] px-4 py-20 sm:px-8 lg:py-24">
          <div className="grid gap-12 lg:grid-cols-2">
            <div>
              <h2 className="text-[32px] font-bold leading-tight tracking-tight text-balance">For you, the owner</h2>
              <ul className="mt-8 space-y-6">
                <Feature icon={ChartColumn} title="All your businesses side by side">
                  This month&apos;s revenue, expenses and profit for each business, with what needs attention: memberships ending this week,
                  overdue dues, requests waiting on you.
                </Feature>
                <Feature icon={Inbox} title="Approvals without the WhatsApp thread">
                  Staff ask for expense claims, purchases, leave or a discount. You approve or say why not, and an approved claim becomes
                  the expense automatically.
                </Feature>
                <Feature icon={Wallet} title="Expenses and salaries that add up">
                  Record what you spend by category, prepare the month&apos;s salaries, and pay them in parts. Profit and loss is worked out
                  from the money that actually moved.
                </Feature>
              </ul>
            </div>
            <div>
              <h2 className="text-[32px] font-bold leading-tight tracking-tight text-balance">For your team and customers</h2>
              <ul className="mt-8 space-y-6">
                <Feature icon={Users} title="Everyone sees what their role needs">
                  Front desk, trainers, stylists and accountants each get a role with exactly the permissions you choose. One login works
                  across every business they are part of.
                </Feature>
                <Feature icon={Mail} title="Customers keep track themselves">
                  Invite a customer to the portal and they can see their plan, what they owe and their receipts. A reminder email goes out
                  three days before their plan ends.
                </Feature>
                <Feature icon={Briefcase} title="Tasks that repeat on their own">
                  Assign a task to anyone on the team. Daily, weekly and monthly tasks come back by themselves when they are done.
                </Feature>
              </ul>
            </div>
          </div>
        </section>

        <section className="border-t border-line bg-surface">
          <div className="mx-auto grid max-w-[1200px] gap-8 px-4 py-16 sm:px-8 md:grid-cols-3">
            <Trust icon={Lock} title="Each business is walled off">
              Staff only ever see the businesses they work in, and a customer only ever sees their own records.
            </Trust>
            <Trust icon={ScrollText} title="Nothing quietly disappears">
              Payments, invoices and expenses are voided with a reason instead of deleted, and every change lands in the activity log.
            </Trust>
            <Trust icon={ShieldCheck} title="Sign-ins are guarded">
              Repeated wrong passwords lock the account for a while, sessions refresh safely, and you can sign in with Google.
            </Trust>
          </div>
        </section>

        <section className="bg-ink text-white">
          <div className="mx-auto flex max-w-[1200px] flex-col items-start justify-between gap-6 px-4 py-16 sm:px-8 md:flex-row md:items-center">
            <div>
              <h2 className="text-[30px] font-bold leading-tight tracking-tight text-balance">Set up your first business in a few minutes.</h2>
              <p className="mt-2 text-[15.5px] text-white/70">Add a plan, add your first member, and take a payment. You can invite your team when you&apos;re ready.</p>
            </div>
            <Link href="/sign-up" className="inline-flex h-12 shrink-0 items-center gap-2 rounded-lg bg-white px-5 text-[15px] font-semibold text-ink hover:bg-white/90">
              Create your account <ArrowRight className="h-4 w-4" aria-hidden />
            </Link>
          </div>
        </section>

      </main>

      <SiteFooter className="border-t-0" />
    </div>
  );
}

function Feature({ icon: Icon, title, children }: { icon: typeof Users; title: string; children: ReactNode }) {
  return (
    <li className="flex gap-4">
      <span className="mt-0.5 grid h-9 w-9 shrink-0 place-items-center rounded-lg border border-line bg-surface text-cobalt">
        <Icon className="h-[18px] w-[18px]" aria-hidden />
      </span>
      <span>
        <span className="block text-[16px] font-semibold">{title}</span>
        <span className="mt-1 block max-w-[52ch] text-[14.5px] leading-relaxed text-muted">{children}</span>
      </span>
    </li>
  );
}

function Trust({ icon: Icon, title, children }: { icon: typeof Users; title: string; children: ReactNode }) {
  return (
    <div>
      <Icon className="h-5 w-5 text-ok" aria-hidden />
      <h3 className="mt-3 text-[15.5px] font-semibold">{title}</h3>
      <p className="mt-1.5 text-[14px] leading-relaxed text-muted">{children}</p>
    </div>
  );
}

/** The hero picture: the owner's portfolio, each business with its own renewal runway. */
function PortfolioIllustration() {
  return (
    <div
      role="img"
      aria-label="Example of the portfolio screen: three businesses with this month's revenue, memberships ending this week and money owed"
      className="relative"
    >
      <div className="rounded-2xl border border-white/10 bg-white p-1.5 text-ink shadow-[0_40px_80px_-30px_rgba(0,0,0,0.6)]">
        <div className="rounded-xl border border-line bg-canvas">
          <div className="flex items-center justify-between border-b border-line px-4 py-3">
            <div>
              <p className="flex items-center gap-2 text-[13px] font-semibold">
                Your businesses
                <span className="rounded-full bg-cobalt-soft px-2 py-0.5 text-[10.5px] font-semibold text-cobalt">Example</span>
              </p>
              <p className="text-[11.5px] text-muted">October so far</p>
            </div>
            <div className="text-right">
              <p className="text-[11.5px] text-muted">Profit this month</p>
              <p className="text-[17px] font-semibold tabular-nums text-ok">₹4,12,800</p>
            </div>
          </div>
          <ul className="divide-y divide-line">
            {PORTFOLIO.map((b, row) => (
              <li key={b.name} className="grid grid-cols-[1.3fr_1fr_auto] items-center gap-3 px-4 py-3">
                <div className="min-w-0">
                  <p className="truncate text-[13.5px] font-semibold">{b.name}</p>
                  <p className="text-[11.5px] text-muted">
                    {b.type} · <span className="tabular-nums">{b.revenue}</span>
                  </p>
                </div>
                <div className="flex h-8 items-end gap-[3px]" aria-hidden>
                  {b.bars.map((value, i) => (
                    <span
                      key={i}
                      className={`runway-grow w-full rounded-t-[2px] ${i < 7 ? "bg-due-fill" : "bg-line-strong"}`}
                      style={{ height: `${Math.max(10, value * 14)}%`, animationDelay: `${row * 120 + i * 30}ms` }}
                    />
                  ))}
                </div>
                <div className="text-right">
                  <p className="text-[12px] font-medium text-due tabular-nums">{b.ending} ending</p>
                  <p className="text-[11.5px] text-bad tabular-nums">{b.owed} owed</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </div>
      <div className="absolute -bottom-16 left-6 hidden rounded-xl border border-line bg-surface px-4 py-3 text-ink shadow-pop sm:block">
        <p className="text-[11.5px] text-muted">Waiting on you</p>
        <p className="text-[13.5px] font-semibold">3 expense claims · 1 leave request</p>
      </div>
    </div>
  );
}
