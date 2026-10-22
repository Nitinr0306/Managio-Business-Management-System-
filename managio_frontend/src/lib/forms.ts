import type { FieldValues, Path, UseFormSetError } from "react-hook-form";
import { ApiError, errorMessage } from "./api/errors";

/**
 * Shows an API failure on the form: field errors from a 400 land on the matching inputs, anything else
 * becomes the returned form-level message (null when every problem was attached to a field).
 */
export function applyApiError<T extends FieldValues>(
  error: unknown,
  setError: UseFormSetError<T>,
  fields: readonly Path<T>[],
): string | null {
  if (error instanceof ApiError && Object.keys(error.fieldErrors).length > 0) {
    const leftover: string[] = [];
    for (const [field, message] of Object.entries(error.fieldErrors)) {
      if ((fields as readonly string[]).includes(field)) {
        setError(field as Path<T>, { type: "server", message });
      } else {
        leftover.push(message);
      }
    }
    return leftover.length ? leftover.join(" ") : null;
  }
  return errorMessage(error);
}

/** Empty strings from inputs become undefined, so optional fields are left out of the request. */
export function blankToUndefined<T extends Record<string, unknown>>(values: T): { [K in keyof T]: T[K] | undefined } {
  const result = {} as { [K in keyof T]: T[K] | undefined };
  for (const key of Object.keys(values) as (keyof T)[]) {
    const value = values[key];
    result[key] = typeof value === "string" && value.trim() === "" ? undefined : value;
  }
  return result;
}
