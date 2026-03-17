import axios, {
  AxiosError,
  AxiosRequestConfig,
  InternalAxiosRequestConfig,
} from 'axios'
import { useAuthStore } from '@/lib/store/authStore'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

export const apiClient = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
  timeout: 30_000,
})

let isRefreshing = false
let failedQueue: { resolve: (token: string) => void; reject: (e: unknown) => void }[] = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) =>
    error ? reject(error) : resolve(token!)
  )
  failedQueue = []
}

// ── Attach Bearer token ────────────────────────────────────────────────────
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = useAuthStore.getState().accessToken
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
  },
  (err) => Promise.reject(err)
)

// ── 401 → refresh token, retry original request ───────────────────────────
apiClient.interceptors.response.use(
  (res) => res,
  async (error: AxiosError) => {
    const original = error.config as AxiosRequestConfig & { _retry?: boolean }

    // Don't retry if no config or already retried
    if (!original || original._retry) return Promise.reject(error)

    // Don't auto-refresh for auth endpoints themselves
    const isAuthEndpoint =
      original.url?.includes('/auth/login') ||
      original.url?.includes('/auth/register') ||
      original.url?.includes('/auth/refresh') ||
      original.url?.includes('/members/auth/login') ||
      original.url?.includes('/members/auth/register') ||
      original.url?.includes('/staff/accept-invitation')

    if (error.response?.status !== 401 || isAuthEndpoint) {
      return Promise.reject(error)
    }

    if (isRefreshing) {
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      })
        .then((token) => {
          if (original.headers) original.headers.Authorization = `Bearer ${token}`
          return apiClient(original)
        })
        .catch((err) => Promise.reject(err))
    }

    original._retry = true
    isRefreshing = true

    const refreshToken = useAuthStore.getState().refreshToken
    if (!refreshToken) {
      isRefreshing = false
      processQueue(new Error('No refresh token'), null)
      useAuthStore.getState().logout()
      if (typeof window !== 'undefined') window.location.href = '/login'
      return Promise.reject(error)
    }

    try {
      const { data } = await axios.post(
        `${BASE_URL}/api/v1/auth/refresh`,
        {},
        { headers: { 'X-Refresh-Token': refreshToken } }
      )
      useAuthStore.getState().setTokens(data.accessToken, data.refreshToken)
      processQueue(null, data.accessToken)
      if (original.headers)
        original.headers.Authorization = `Bearer ${data.accessToken}`
      return apiClient(original)
    } catch (refreshErr) {
      processQueue(refreshErr, null)
      useAuthStore.getState().logout()
      if (typeof window !== 'undefined') {
        const userType = useAuthStore.getState().userType
        if (userType === 'staff') window.location.href = '/staff/login'
        else if (userType === 'member') window.location.href = '/member/login'
        else window.location.href = '/login'
      }
      return Promise.reject(refreshErr)
    } finally {
      isRefreshing = false
    }
  }
)

export default apiClient