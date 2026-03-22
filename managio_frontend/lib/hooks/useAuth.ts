'use client'

import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/store/authStore'
import { getUserDisplayName } from '@/lib/utils/formatters'
import {
  getErrorMessage,
  getErrorStatus,
  isNetworkError,
  getErrorCode,
} from '@/lib/utils/errors'
import type { StaffContext } from '@/lib/store/authStore'

// Machine-readable error codes from backend
const ERROR_CODES = {
  EMAIL_NOT_VERIFIED: 'AUTH_003',
  INVALID_CREDENTIALS: 'AUTH_004',
  ACCOUNT_LOCKED: 'AUTH_005',
  ACCOUNT_DISABLED: 'AUTH_008',
} as const

export function useAuth() {
  const store = useAuthStore()
  const router = useRouter()
  const qc = useQueryClient()

  // ───────────── CENTRAL ERROR HANDLER ─────────────
  const handleAuthError = (
    err: unknown,
    options?: {
      defaultMessage?: string
      invalidCredentialsMessage?: string
      loginEmail?: string
    }
  ) => {
    const status = getErrorStatus(err)
    const errorCode = getErrorCode(err)

    if (isNetworkError(err)) {
      toast.error('Cannot connect to server. Check your internet connection.')
      return
    }

    // Account locked (HTTP 423 or error code AUTH_005)
    if (status === 423 || errorCode === ERROR_CODES.ACCOUNT_LOCKED) {
      toast.error('Account locked due to too many failed attempts. Please try again later or reset your password.')
      return
    }

    if (status === 429) {
      toast.error('Too many attempts. Please wait and try again.')
      return
    }

    // Email not verified (error code AUTH_003)
    if (errorCode === ERROR_CODES.EMAIL_NOT_VERIFIED) {
      const email = options?.loginEmail || localStorage.getItem('pending_verification_email')

      toast.error('Email not verified. Check your inbox.')

      router.push(
        email
          ? `/verify-email?email=${encodeURIComponent(email)}`
          : '/verify-email'
      )
      return
    }

    // Invalid credentials (error code AUTH_004)
    if (errorCode === ERROR_CODES.INVALID_CREDENTIALS || status === 401) {
      toast.error(options?.invalidCredentialsMessage || 'Invalid credentials.')
      return
    }

    // Account disabled (error code AUTH_008)
    if (errorCode === ERROR_CODES.ACCOUNT_DISABLED) {
      toast.error('Your account has been disabled. Please contact support.')
      return
    }

    if (status === 403) {
      toast.error('You do not have permission to perform this action.')
      return
    }

    if (status === 404) {
      toast.error('Resource not found.')
      return
    }

    toast.error(getErrorMessage(err, options?.defaultMessage))
  }

  // ───────── OWNER LOGIN ─────────
  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      store.setAuth(data.user, data.accessToken, data.refreshToken, 'owner')
      localStorage.removeItem('pending_verification_email')
      toast.success(`Welcome back, ${getUserDisplayName(data.user)}!`)
      router.replace('/dashboard')
    },
    onError: (err, variables) => {
      // Store email for potential verification redirect
      if (variables?.email) {
        localStorage.setItem('pending_verification_email', variables.email)
      }
      handleAuthError(err, {
        invalidCredentialsMessage: 'Invalid email or password.',
        defaultMessage: 'Login failed.',
        loginEmail: variables?.email,
      })
    },
  })

  // ───────── STAFF LOGIN ─────────
  const staffLoginMutation = useMutation({
    mutationFn: authApi.staffLogin,
    onSuccess: (data) => {
      if (data.requiresTwoFactor) return

      const staffCtx: StaffContext = {
        staffId: data.staff.staffId,
        businessId: data.staff.businessId,
        staffRole: data.staff.role,
        permissions: data.staff.permissions || [],
        canManageMembers: data.staff.canManageMembers,
        canManagePayments: data.staff.canManagePayments,
        canManageSubscriptions: data.staff.canManageSubscriptions,
        canViewReports: data.staff.canViewReports,
        canLogin: data.staff.canLogin,
        department: data.staff.department,
        designation: data.staff.designation,
        employeeId: data.staff.employeeId,
      }

      store.setAuth(
        data.user,
        data.accessToken,
        data.refreshToken,
        'staff',
        staffCtx,
        data.business ?? null
      )

      toast.success(`Welcome, ${getUserDisplayName(data.user)}!`)
      router.replace('/staff/dashboard')
    },
    onError: (err, variables) =>
      handleAuthError(err, {
        invalidCredentialsMessage:
          'Invalid credentials. Check Business ID, email, and password.',
        defaultMessage: 'Login failed.',
        loginEmail: variables?.email,
      }),
  })

  // ───────── MEMBER LOGIN ─────────
  const memberLoginMutation = useMutation({
    mutationFn: authApi.memberLogin,
    onSuccess: (data) => {
      const user = {
        id: String(data.member.id),
        email: data.member.email || '',
        firstName: data.member.firstName,
        lastName: data.member.lastName,
        fullName: data.member.fullName,
        phoneNumber: data.member.phone,
        roles: ['ROLE_MEMBER'],
        emailVerified: true,
        enabled: true,
        accountLocked: false,
        accountStatus: 'ACTIVE',
        twoFactorEnabled: false,
        createdAt: data.member.memberSince,
        updatedAt: data.member.memberSince,
      }

      store.setAuth(
        user,
        data.accessToken,
        data.refreshToken,
        'member',
        null,
        data.business ?? null,
        data.activeSubscription ?? null
      )

      toast.success(`Welcome, ${data.member.fullName}!`)
      router.replace('/member/dashboard')
    },
    onError: (err) =>
      handleAuthError(err, {
        invalidCredentialsMessage: 'Invalid phone/email or password.',
        defaultMessage: 'Login failed.',
      }),
  })

  // ───────── REGISTER ─────────
  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (_, variables) => {
      localStorage.setItem('pending_verification_email', variables.email)

      toast.success('Account created! Check your email to verify your account.', {
        duration: 6000,
      })

      router.push('/login')
    },
    onError: (err) =>
      handleAuthError(err, { defaultMessage: 'Registration failed.' }),
  })

  // ───────── RESEND VERIFICATION ─────────
  const resendVerificationMutation = useMutation({
    mutationFn: (email: string) => authApi.resendVerification(email),
    onSuccess: () => {
      toast.success('Verification email sent. Check your inbox.')
    },
    onError: () => {
      // Don't reveal whether account exists — always show success message
      toast.success('If the account exists, a verification email has been sent.')
    },
  })

  // ───────── FORGOT PASSWORD ─────────
  const forgotPasswordMutation = useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
    onSuccess: () => {
      toast.success('If an account exists with that email, a reset link has been sent.', {
        duration: 6000,
      })
    },
    onError: (err) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
        return
      }
      // Always show success to prevent email enumeration
      toast.success('If an account exists with that email, a reset link has been sent.', {
        duration: 6000,
      })
    },
  })

  // ───────── MEMBER FORGOT PASSWORD ─────────
  const memberForgotPasswordMutation = useMutation({
    mutationFn: (identifier: string) => authApi.memberForgotPassword(identifier),
    onSuccess: () => {
      toast.success('If an account exists with that identifier, a reset link has been sent.', {
        duration: 6000,
      })
    },
    onError: (err) => {
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
        return
      }
      toast.success('If an account exists with that identifier, a reset link has been sent.', {
        duration: 6000,
      })
    },
  })

  // ───────── LOGOUT ─────────
  const logoutMutation = useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      const type = store.userType
      store.logout()
      qc.clear()

      if (type === 'staff') router.replace('/staff/login')
      else if (type === 'member') router.replace('/member/login')
      else router.replace('/login')
    },
  })

  return {
    user: store.user,
    isAuthenticated: store.isAuthenticated,
    userType: store.userType,
    staffContext: store.staffContext,

    login: loginMutation.mutate,
    staffLogin: staffLoginMutation.mutate,
    memberLogin: memberLoginMutation.mutate,
    register: registerMutation.mutate,
    logout: logoutMutation.mutate,
    resendVerification: resendVerificationMutation.mutate,
    forgotPassword: forgotPasswordMutation.mutateAsync,
    memberForgotPassword: memberForgotPasswordMutation.mutateAsync,

    isLoginPending: loginMutation.isPending,
    isStaffLoginPending: staffLoginMutation.isPending,
    isMemberLoginPending: memberLoginMutation.isPending,
    isRegisterPending: registerMutation.isPending,
    isResendPending: resendVerificationMutation.isPending,
    isForgotPending: forgotPasswordMutation.isPending,
    isMemberForgotPending: memberForgotPasswordMutation.isPending,
  }
}