import apiClient from './axios'
import { useAuthStore } from '@/lib/store/authStore'
import type {
  AuthTokens,
  User,
  LoginRequest,
  RegisterRequest,
  StaffLoginRequest,
  MemberLoginRequest,
  MemberRegisterRequest,
  StaffLoginResponse,
  MemberLoginResponse,
} from '@/lib/types/auth'

const AUTH = '/api/v1/auth'
const MEMBER_AUTH = '/api/v1/members/auth'

export const authApi = {
  login: (data: LoginRequest) =>
    apiClient
      .post<AuthTokens & { user: User; lastLoginAt?: string; requiresTwoFactor?: boolean }>(
        `${AUTH}/login`,
        data
      )
      .then((r) => r.data),

  register: (data: RegisterRequest) =>
    apiClient.post<User>(`${AUTH}/register`, data).then((r) => r.data),

  logout: () => {
    const { refreshToken, accessToken } = useAuthStore.getState()
    return apiClient
      .post<void>(
        `${AUTH}/logout`,
        {},
        {
          headers: {
            'X-Refresh-Token': refreshToken || '',
            Authorization: accessToken ? `Bearer ${accessToken}` : '',
          },
        }
      )
      .then((r) => r.data)
      .catch(() => {
        // Logout should always succeed client-side even if server call fails
      })
  },

  refreshToken: (refreshToken: string) =>
    apiClient
      .post<AuthTokens>(`${AUTH}/refresh`, {}, { headers: { 'X-Refresh-Token': refreshToken } })
      .then((r) => r.data),

  getMe: () => apiClient.get<User>(`${AUTH}/me`).then((r) => r.data),

  verifyEmail: (token: string) =>
    apiClient
      .post<void>(`${AUTH}/verify-email`, null, { params: { token } })
      .then((r) => r.data),

  forgotPassword: (email: string) =>
    apiClient
      .post<void>(`${AUTH}/forgot-password`, null, { params: { email } })
      .then((r) => r.data),

  resetPassword: (data: { token: string; newPassword: string; subject?: 'user' | 'member' }) =>
    apiClient
      .post<void>(`${AUTH}/reset-password`, null, {
        params: { token: data.token, newPassword: data.newPassword, subject: data.subject },
      })
      .then((r) => r.data),

  changePassword: (oldPassword: string, newPassword: string) =>
    apiClient
      .post<void>(`${AUTH}/change-password`, null, { params: { oldPassword, newPassword } })
      .then((r) => r.data),

  staffLogin: (data: StaffLoginRequest): Promise<StaffLoginResponse> =>
    apiClient.post<StaffLoginResponse>(`${AUTH}/staff/login`, data).then((r) => r.data),

  memberLogin: (data: MemberLoginRequest): Promise<MemberLoginResponse> =>
    apiClient.post<MemberLoginResponse>(`${MEMBER_AUTH}/login`, data).then((r) => r.data),

  memberRegister: (data: MemberRegisterRequest): Promise<{
  requiresVerification: boolean
  email: string
}> =>
  apiClient.post(`${MEMBER_AUTH}/register`, data).then((r) => r.data),
  memberForgotPassword: (identifier: string) =>
    apiClient
      .post<void>(`${MEMBER_AUTH}/forgot-password`, null, { params: { identifier } })
      .then((r) => r.data),

  memberVerifyEmail: (token: string) =>
    apiClient
      .post<void>(`${MEMBER_AUTH}/verify-email`, null, { params: { token } })
      .then((r) => r.data),

  memberResendVerification: (email: string) =>
    apiClient
      .post<void>(`${MEMBER_AUTH}/resend-verification`, null, { params: { email } })
      .then((r) => r.data),

  memberResetPassword: (token: string, newPassword: string) =>
    apiClient
      .post<void>(`${MEMBER_AUTH}/reset-password`, null, { params: { token, newPassword } })
      .then((r) => r.data),

  memberChangePassword: (currentPassword: string, newPassword: string, confirmPassword: string) =>
    apiClient
      .post<void>(`${MEMBER_AUTH}/change-password`, { currentPassword, newPassword, confirmPassword })
      .then((r) => r.data),

  // ADD THIS inside authApi object

resendVerification: (email: string) =>
  apiClient
    .post<void>(`${AUTH}/resend-verification-email`, null, {
      params: { email },
    })
    .then((r) => r.data),
  
}