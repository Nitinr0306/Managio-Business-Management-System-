import { AxiosError } from 'axios'

export interface ApiError {
  message: string
  status: number
  errorCode?: string
  details?: Record<string, string>
}

/**
 * Extracts a human-readable error message from any error type.
 * Handles Axios errors, standard Errors, and unknown objects.
 */
export function getErrorMessage(error: unknown, fallback = 'Something went wrong. Please try again.'): string {
  if (!error) return fallback

  // Axios error with response body
  if (isAxiosError(error)) {
    const data = error.response?.data as any
    const status = error.response?.status

    // Handle specific status codes — let backend messages pass through for 401
    if (status === 403) return 'You don\'t have permission to perform this action.'
    if (status === 429) return 'Too many requests. Please wait a moment and try again.'
    if (status === 503) return 'Service temporarily unavailable. Please try again later.'

    // Try to extract message from response body
    if (data?.message) return data.message
    if (data?.error) return data.error
    if (typeof data === 'string') return data

    // Network errors
    if (error.code === 'ECONNABORTED') return 'Request timed out. Check your connection.'
    if (error.code === 'ERR_NETWORK') return 'Network error. Check your connection.'
    if (!error.response) return 'Cannot connect to server. Check your internet connection.'
  }

  // Standard JS Error
  if (error instanceof Error) return error.message

  return fallback
}

/**
 * Extracts the machine-readable error code from backend responses.
 * Returns codes like "AUTH_003", "AUTH_004", "VAL_001", etc.
 */
export function getErrorCode(error: unknown): string | null {
  if (!isAxiosError(error)) return null
  return (error.response?.data as any)?.errorCode || null
}

/**
 * Extracts field-level validation errors from a 400 response.
 */
export function getFieldErrors(error: unknown): Record<string, string> | null {
  if (!isAxiosError(error)) return null
  const data = error.response?.data as any
  if (!data?.details || typeof data.details !== 'object') return null
  return data.details
}

/**
 * Type guard for Axios errors.
 */
export function isAxiosError(error: unknown): error is AxiosError {
  return (
    typeof error === 'object' &&
    error !== null &&
    'isAxiosError' in error &&
    (error as AxiosError).isAxiosError === true
  )
}

/**
 * Returns HTTP status code from an error, or null.
 */
export function getErrorStatus(error: unknown): number | null {
  if (!isAxiosError(error)) return null
  return error.response?.status ?? null
}

/**
 * Returns true if the error is a network/connection failure (no response).
 */
export function isNetworkError(error: unknown): boolean {
  if (!isAxiosError(error)) return false
  return !error.response
}

/**
 * Returns true if the error is a 404 Not Found.
 */
export function isNotFoundError(error: unknown): boolean {
  return getErrorStatus(error) === 404
}

/**
 * Returns true if the error is a 409 Conflict (duplicate resource).
 */
export function isConflictError(error: unknown): boolean {
  return getErrorStatus(error) === 409
}

/**
 * Returns true if the error is a 400 Validation Error.
 */
export function isValidationError(error: unknown): boolean {
  return getErrorStatus(error) === 400
}

/**
 * Returns a descriptive toast message for common API errors by entity type.
 */
export function getEntityErrorMessage(
  error: unknown,
  entity: string,
  action: 'create' | 'update' | 'delete' | 'load' | 'fetch'
): string {
  const status = getErrorStatus(error)

  if (isNetworkError(error)) return 'Network error — check your connection.'
  if (status === 409) {
    if (action === 'create') return `A ${entity} with that information already exists.`
  }
  if (status === 404) return `${entity} not found.`
  if (status === 403) return `You don't have permission to ${action} this ${entity}.`
  if (status === 401) return 'Session expired — please sign in again.'

  const msg = getErrorMessage(error)
  if (msg && msg !== 'Something went wrong. Please try again.') return msg

  const actionLabel = {
    create: 'create',
    update: 'update',
    delete: 'delete',
    load: 'load',
    fetch: 'fetch',
  }[action]

  return `Failed to ${actionLabel} ${entity}. Please try again.`
}