import type { Metadata } from 'next'
import { DM_Sans, Syne } from 'next/font/google'
import './globals.css'
import { Providers } from './providers'

const dmSans = DM_Sans({ subsets: ['latin'], variable: '--font-dm-sans', weight: ['300','400','500','600','700'] })
const syne   = Syne({ subsets: ['latin'], variable: '--font-syne', weight: ['400','500','600','700','800'] })

export const metadata: Metadata = {
  title: { template: '%s | Managio', default: 'Managio — Smart Business Management' },
  description: 'All-in-one gym, fitness studio & small business management. Members, staff, subscriptions, payments.',
  keywords: ['gym management','fitness studio','business management','members','subscriptions'],
  themeColor: '#070710',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className="dark" suppressHydrationWarning>
      <body className={`${dmSans.variable} ${syne.variable} font-sans`}>
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}