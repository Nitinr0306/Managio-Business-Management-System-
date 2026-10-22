/**
 * Every failed API call becomes an ApiError. The backend answers with RFC 9457 problem details carrying a
 * stable `code` and, for validation failures, an `errors` map of field to message (docs/API.md#errors).
 */
export type Problem = {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  code?: string;
  requestId?: string;
  errors?: Record<string, string>;
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: Record<string, string>;
  readonly requestId?: string;

  constructor(status: number, code: string, message: string, fieldErrors: Record<string, string> = {}, requestId?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.requestId = requestId;
  }

  static fromProblem(status: number, problem: unknown): ApiError {
    const p = (problem && typeof problem === "object" ? problem : {}) as Problem;
    if (p.code) {
      return new ApiError(status, p.code, p.detail ?? p.title ?? defaultMessage(status), p.errors ?? {}, p.requestId);
    }
    return new ApiError(status, status >= 500 ? "server.error" : "request.failed", defaultMessage(status));
  }

  static network(): ApiError {
    return new ApiError(0, "network", "Can't reach Managio right now. Check your connection and try again.");
  }

  is(code: string): boolean {
    return this.code === code;
  }
}

function defaultMessage(status: number): string {
  if (status === 401) return "Your session has ended. Sign in again.";
  if (status === 403) return "You don't have permission to do this.";
  if (status === 404) return "We couldn't find that. It may have been removed.";
  if (status === 429) return "Too many attempts. Wait a minute and try again.";
  if (status >= 500) return "Something went wrong on our side. Try again in a moment.";
  return "That didn't work. Check the details and try again.";
}

/** A message for any thrown value, for toasts and inline errors. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error && error.message) return error.message;
  return "Something went wrong. Try again.";
}
