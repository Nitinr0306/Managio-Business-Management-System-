import type { Metadata, Viewport } from "next";
import { Public_Sans } from "next/font/google";
import { SITE } from "@/lib/site";
import { Providers } from "./providers";
import "./globals.css";

const publicSans = Public_Sans({
  variable: "--font-public-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  display: "swap",
});

export const metadata: Metadata = {
  metadataBase: new URL(SITE.url),
  title: { default: "Managio: free business management for gyms, salons, coaching centres and shops", template: "%s · Managio" },
  description: SITE.description,
  applicationName: "Managio",
  openGraph: {
    type: "website",
    siteName: "Managio",
    locale: "en_IN",
    title: "Managio: run every business you own from one screen",
    description: SITE.description,
  },
  twitter: {
    card: "summary_large_image",
    title: "Managio: run every business you own from one screen",
    description: SITE.description,
  },
  formatDetection: { telephone: false, email: false, address: false },
};

export const viewport: Viewport = {
  themeColor: "#f5f6f8",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="en-IN" className={`${publicSans.variable} h-full antialiased`}>
      <body className="min-h-full font-sans">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
