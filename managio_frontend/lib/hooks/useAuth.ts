import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useRouter } from 'next/navigation'
import { toast } from 'sonner'
import { authApi } from '@/lib/api/auth'
import { useAuthStore } from '@/lib/store/authStore'
import { getUserDisplayName } from '@/lib/utils/formatters'
import { getErrorMessage, getErrorStatus, isNetworkError } from '@/lib/utils/errors'
import type { LoginRequest, RegisterRequest } from '@/lib/types/auth'
import type { StaffContext } from '@/lib/store/authStore'

export function useAuth() {
  const store = useAuthStore()
  const router = useRouter()
  const qc = useQueryClient()

  // ── Owner login ──────────────────────────────────────────────────────────
  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data) => {
      store.setAuth(data.user, data.accessToken, data.refreshToken, 'owner')
      const name = getUserDisplayName(data.user)
      toast.success(`Welcome back, ${name}!`)
      setTimeout(() => router.replace('/dashboard'), 100)
    },
    onError: (err: unknown) => {
      const status = getErrorStatus(err)
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (status === 423) {
        toast.error('Account locked. Please reset your password or contact support.')
      } else if (status === 401) {
        toast.error('Invalid email or password.')
      } else if (status === 429) {
        toast.error('Too many login attempts. Please wait 60 seconds and try again.')
      } else {
        toast.error(getErrorMessage(err, 'Login failed. Please try again.'))
      }
    },
  })

  // ── Staff login ──────────────────────────────────────────────────────────
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

      const bizCtx = data.business
        ? {
            id: data.business.id,
            name: data.business.name,
            address: data.business.address,
            phone: data.business.phone,
            email: data.business.email,
          }
        : null

      store.setAuth(data.user, data.accessToken, data.refreshToken, 'staff', staffCtx, bizCtx)
      toast.success(`Welcome, ${getUserDisplayName(data.user)}!`)
      setTimeout(() => router.replace('/staff/dashboard'), 100)
    },
    onError: (err: unknown) => {
      const status = getErrorStatus(err)
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (status === 401) {
        toast.error('Invalid credentials. Check your Business ID, email, and password.')
      } else if (status === 403) {
        toast.error('Your account does not have login access. Contact your manager.')
      } else if (status === 404) {
        toast.error('Business not found. Check your Business ID.')
      } else if (status === 423) {
        toast.error('Account locked. Contact your manager or reset your password.')
      } else if (status === 429) {
        toast.error('Too many login attempts. Please wait and try again.')
      } else {
        toast.error(getErrorMessage(err, 'Login failed. Please try again.'))
      }
    },
  })

  // ── Member login ─────────────────────────────────────────────────────────
  const memberLoginMutation = useMutation({
    mutationFn: authApi.memberLogin,
    onSuccess: (data) => {
      const memberAsUser = {
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
        memberAsUser,
        data.accessToken,
        data.refreshToken,
        'member',
        null,
        data.business ? { id: data.business.id, name: data.business.name } : null,
        data.activeSubscription ?? null
      )
      toast.success(`Welcome, ${data.member.fullName}!`)
      setTimeout(() => router.replace('/member/dashboard'), 100)
    },
    onError: (err: unknown) => {
      const status = getErrorStatus(err)
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (status === 401) {
        toast.error('Invalid phone/email or password.')
      } else if (status === 403) {
        toast.error('Your account has been deactivated. Contact your gym.')
      } else if (status === 429) {
        toast.error('Too many login attempts. Please wait and try again.')
      } else {
        toast.error(getErrorMessage(err, 'Login failed. Please try again.'))
      }
    },
  })

  // ── Register ─────────────────────────────────────────────────────────────
  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: () => {
      toast.success('Account created! Check your email to verify your account.', {
        duration: 6000,
      })
      router.push('/login')
    },
    onError: (err: unknown) => {
      const status = getErrorStatus(err)
      if (isNetworkError(err)) {
        toast.error('Cannot connect to server. Check your internet connection.')
      } else if (status === 409) {
        toast.error('An account with this email already exists. Try signing in.')
      } else if (status === 400) {
        toast.error(getErrorMessage(err, 'Please check your details and try again.'))
      } else {
        toast.error(getErrorMessage(err, 'Registration failed. Please try again.'))
      }
    },
  })

  // ── Logout ───────────────────────────────────────────────────────────────
  const logoutMutation = useMutation({
    mutationFn: authApi.logout,
    onSettled: () => {
      const currentUserType = store.userType
      store.logout()
      qc.clear()
      if (currentUserType === 'staff') router.replace('/staff/login')
      else if (currentUserType === 'member') router.replace('/member/login')
      else router.replace('/login')
    },
  })

  // ── Forgot password ──────────────────────────────────────────────────────
  const forgotPasswordMutation = useMutation({
    mutationFn: (email: string) => authApi.forgotPassword(email),
    onSuccess: () =>
      toast.success(
        'If that email exists, a reset link has been sent. Check your inbox.',
        { duration: 8000 }
      ),
    onError: () =>
      // Always show success to prevent email enumeration
      toast.success('If that email exists, a reset link has been sent.', {
        duration: 8000,
      }),
  })

  const memberForgotPasswordMutation = useMutation({
    mutationFn: (identifier: string) => authApi.memberForgotPassword(identifier),
    onSuccess: () =>
      toast.success(
        'If that account exists, a reset link has been sent. Check your inbox/SMS.',
        { duration: 8000 }
      ),
    onError: () =>
      toast.success('If that account exists, a reset link has been sent.', {
        duration: 8000,
      }),
  })

  // ── Change password ──────────────────────────────────────────────────────
  const changePasswordMutation = useMutation({
    mutationFn: ({
      currentPassword,
      newPassword,
    }: {
      currentPassword: string
      newPassword: string
    }) => authApi.changePassword(currentPassword, newPassword),
    onSuccess: () => {
      toast.success('Password updated successfully.')
      store.logout()
      qc.clear()
      router.replace('/login')
    },
    onError: (err: unknown) => {
      const status = getErrorStatus(err)
      if (status === 401) {
        toast.error('Current password is incorrect.')
      } else {
        toast.error(getErrorMessage(err, 'Failed to change password. Please try again.'))
      }
    },
  })

  // ── Current user ─────────────────────────────────────────────────────────
  const meQuery = useQuery({
    queryKey: ['me'],
    queryFn: authApi.getMe,
    enabled: store.isAuthenticated && store.userType !== 'member',
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error: unknown) => {
      const status = getErrorStatus(error)
      if (status === 401 || status === 403) return false
      return failureCount < 2
    },
  })

  return {
    user: store.user,
    userName: getUserDisplayName(store.user),
    isAuthenticated: store.isAuthenticated,
    userType: store.userType,
    staffContext: store.staffContext,
    businessContext: store.businessContext,

    // Mutations
    login: loginMutation.mutate,
    loginAsync: loginMutation.mutateAsync,
    staffLogin: staffLoginMutation.mutate,
    staffLoginAsync: staffLoginMutation.mutateAsync,
    memberLogin: memberLoginMutation.mutate,
    register: registerMutation.mutate,
    logout: logoutMutation.mutate,
    forgotPassword: forgotPasswordMutation.mutate,
    memberForgotPassword: memberForgotPasswordMutation.mutate,
    changePassword: changePasswordMutation.mutate,

    // Pending states
    isLoginPending: loginMutation.isPending,
    isStaffLoginPending: staffLoginMutation.isPending,
    isMemberLoginPending: memberLoginMutation.isPending,
    isRegisterPending: registerMutation.isPending,
    isLogoutPending: logoutMutation.isPending,
    isForgotPending: forgotPasswordMutation.isPending,
    isMemberForgotPending: memberForgotPasswordMutation.isPending,
    isChangePending: changePasswordMutation.isPending,

    // Data
    meQuery,

    // Permission helpers for staff (default true for owners)
    canManageMembers:       store.staffContext?.canManageMembers       ?? true,
    canManagePayments:      store.staffContext?.canManagePayments      ?? true,
    canManageSubscriptions: store.staffContext?.canManageSubscriptions ?? true,
    canViewReports:         store.staffContext?.canViewReports         ?? true,
  }
}