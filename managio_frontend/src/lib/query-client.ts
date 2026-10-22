import { QueryClient } from "@tanstack/react-query";
import { ApiError } from "./api/errors";

/**
 * Server state lives in TanStack Query. Client errors (4xx) are not retried: asking again gives the same
 * answer. Server and network errors get two quick retries.
 */
export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: true,
        retry: (failureCount, error) => {
          if (error instanceof ApiError && error.status >= 400 && error.status < 500) return false;
          return failureCount < 2;
        },
      },
      mutations: { retry: false },
    },
  });
}
