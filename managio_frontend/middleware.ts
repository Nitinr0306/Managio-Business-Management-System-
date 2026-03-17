import { NextRequest, NextResponse } from 'next/server'

// Auth-protected route groups
const OWNER_ROUTES = ['/dashboard', '/businesses', '/profile', '/settings', '/notifications']
const STAFF_ROUTES = [
  '/staff/dashboard',
  '/staff/members',
  '/staff/payments',
  '/staff/subscriptions',
]
const MEMBER_ROUTES = ['/member/dashboard', '/member/subscription', '/member/payments', '/member/notifications', '/member/profile']
const PUBLIC_AUTH_ROUTES = [
  '/login',
  '/register',
  '/forgot-password',
  '/reset-password',
  '/verify-email',
  '/staff/login',
  '/member/login',
  '/member/register',
  '/staff/accept-invitation',
]

function getAuthCookies(request: NextRequest) {
  const token = request.cookies.get('access_token')?.value || null
  const authType = request.cookies.get('auth_type')?.value || null
  return { token, authType }
}

function isPathUnder(pathname: string, routes: string[]) {
  return routes.some((r) => pathname === r || pathname.startsWith(r + '/'))
}

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl
  const { token, authType } = getAuthCookies(request)

  const isPublicAuthRoute = isPathUnder(pathname, PUBLIC_AUTH_ROUTES)
  const isOwnerRoute = isPathUnder(pathname, OWNER_ROUTES)
  const isStaffRoute = isPathUnder(pathname, STAFF_ROUTES)
  const isMemberRoute = isPathUnder(pathname, MEMBER_ROUTES)

  const isAuthenticated = !!token

  // ── Redirect authenticated users away from auth pages ────────────────────────
  if (isPublicAuthRoute && isAuthenticated) {
    if (authType === 'staff') {
      return NextResponse.redirect(new URL('/staff/dashboard', request.url))
    }
    if (authType === 'member') {
      return NextResponse.redirect(new URL('/member/dashboard', request.url))
    }
    // Owner or unknown → dashboard
    return NextResponse.redirect(new URL('/dashboard', request.url))
  }

  // ── Protect staff routes ─────────────────────────────────────────────────────
  if (isStaffRoute) {
    if (!isAuthenticated || authType !== 'staff') {
      const url = new URL('/staff/login', request.url)
      url.searchParams.set('redirect', pathname)
      return NextResponse.redirect(url)
    }
  }

  // ── Protect member routes ────────────────────────────────────────────────────
  if (isMemberRoute) {
    if (!isAuthenticated || authType !== 'member') {
      const url = new URL('/member/login', request.url)
      url.searchParams.set('redirect', pathname)
      return NextResponse.redirect(url)
    }
  }

  // ── Protect owner/dashboard routes ──────────────────────────────────────────
  if (isOwnerRoute) {
    if (!isAuthenticated) {
      const url = new URL('/login', request.url)
      url.searchParams.set('redirect', pathname)
      return NextResponse.redirect(url)
    }
    // Redirect staff/member to their own dashboards if they land on owner routes
    if (authType === 'staff') {
      return NextResponse.redirect(new URL('/staff/dashboard', request.url))
    }
    if (authType === 'member') {
      return NextResponse.redirect(new URL('/member/dashboard', request.url))
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: [
    '/((?!api|_next/static|_next/image|favicon.ico|.*\\.).*)',
  ],
}