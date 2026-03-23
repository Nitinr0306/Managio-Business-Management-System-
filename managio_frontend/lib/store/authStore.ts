import { create } from 'zustand'
import { persist, createJSONStorage } from 'zustand/middleware'
import type { User, ActiveSubscriptionInfo } from '@/lib/types/auth'

export interface StaffContext {
  staffId: number
  staffPublicId?: string
  businessId: number
  businessPublicId?: string
  staffRole: string
  permissions: string[]
  canManageMembers: boolean
  canManagePayments: boolean
  canManageSubscriptions: boolean
  canViewReports: boolean
  canLogin: boolean
  department?: string
  designation?: string
  employeeId?: string
}

export interface BusinessContext {
  id: number
  publicId?: string
  name: string
  address?: string
  phone?: string
  email?: string
}

interface AuthState {
  user: User | null
  accessToken: string | null
  refreshToken: string | null
  userType: 'owner' | 'staff' | 'member' | null
  staffContext: StaffContext | null
  businessContext: BusinessContext | null
  activeSubscription: ActiveSubscriptionInfo | null
  isAuthenticated: boolean

  setAuth: (
    user: User,
    accessToken: string,
    refreshToken: string,
    userType?: 'owner' | 'staff' | 'member',
    staffCtx?: StaffContext | null,
    bizCtx?: BusinessContext | null,
    activeSub?: ActiveSubscriptionInfo | null,
  ) => void

  setTokens: (accessToken: string, refreshToken: string) => void
  setUser: (user: User) => void
  setStaffContext: (ctx: StaffContext | null) => void
  setBusinessContext: (ctx: BusinessContext | null) => void
  logout: () => void
}

const storage = createJSONStorage(() =>
  typeof window !== 'undefined'
    ? localStorage
    : { getItem: () => null, setItem: () => {}, removeItem: () => {} }
)

function setCookie(name: string, value: string, maxAge = 3600) {
  if (typeof document !== 'undefined') {
    document.cookie = `${name}=${value}; path=/; max-age=${maxAge}; SameSite=Lax`
  }
}
function deleteCookie(name: string) {
  if (typeof document !== 'undefined') {
    document.cookie = `${name}=; path=/; max-age=0`
  }
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      userType: null,
      staffContext: null,
      businessContext: null,
      activeSubscription: null,
      isAuthenticated: false,

      setAuth: (
        user, accessToken, refreshToken,
        userType = 'owner',
        staffCtx = null,
        bizCtx = null,
        activeSub = null,
      ) => {
        setCookie('access_token', accessToken, 3600)
        setCookie('auth_type', userType, 3600)
        if (staffCtx?.businessId) {
          setCookie('business_id', String(staffCtx.businessId), 3600)
        }
        set({
          user,
          accessToken,
          refreshToken,
          userType,
          isAuthenticated: true,
          staffContext: staffCtx,
          businessContext: bizCtx,
          activeSubscription: activeSub,
        })
      },

      setTokens: (accessToken, refreshToken) => {
        setCookie('access_token', accessToken, 3600)
        set({ accessToken, refreshToken })
      },

      setUser: (user) => set({ user }),
      setStaffContext: (staffContext) => set({ staffContext }),
      setBusinessContext: (businessContext) => set({ businessContext }),

      logout: () => {
        deleteCookie('access_token')
        deleteCookie('auth_type')
        deleteCookie('business_id')
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          userType: null,
          staffContext: null,
          businessContext: null,
          activeSubscription: null,
          isAuthenticated: false,
        })
      },
    }),
    {
      name: 'managio-auth',
      storage,
      partialize: (state) => ({
        user: state.user,
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        userType: state.userType,
        staffContext: state.staffContext,
        businessContext: state.businessContext,
        activeSubscription: state.activeSubscription,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
)