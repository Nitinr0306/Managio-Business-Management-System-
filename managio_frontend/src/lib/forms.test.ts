import { describe, expect, it, vi } from "vitest";
import { ApiError } from "./api/errors";
import { applyApiError, blankToUndefined } from "./forms";

describe("applyApiError", () => {
  it("puts field errors on the matching inputs and returns the rest", () => {
    const setError = vi.fn();
    const error = new ApiError(400, "validation.failed", "2 fields need attention.", {
      email: "Enter a valid email",
      lines: "Line 2 needs a price",
    });
    const leftover = applyApiError<{ email: string; name: string }>(error, setError, ["email", "name"]);
    expect(setError).toHaveBeenCalledWith("email", { type: "server", message: "Enter a valid email" });
    expect(setError).toHaveBeenCalledTimes(1);
    expect(leftover).toBe("Line 2 needs a price");
  });

  it("returns nothing when every problem landed on a field", () => {
    const setError = vi.fn();
    const error = new ApiError(400, "validation.failed", "1 field needs attention.", { name: "Enter a name" });
    expect(applyApiError<{ name: string }>(error, setError, ["name"])).toBeNull();
  });

  it("returns the message for errors that aren't about fields", () => {
    const error = new ApiError(409, "customer.duplicate_phone", "Rohan (#12) already has this phone number.");
    expect(applyApiError(error, vi.fn(), [])).toBe("Rohan (#12) already has this phone number.");
  });
});

describe("blankToUndefined", () => {
  it("leaves out empty text so optional fields aren't sent", () => {
    expect(blankToUndefined({ name: "Asha", phone: "  ", count: 0 })).toEqual({ name: "Asha", phone: undefined, count: 0 });
  });
});
