import { describe, expect, it } from "vitest";
import { ApiError, errorMessage } from "./errors";

describe("ApiError", () => {
  it("keeps the problem's code, message and field errors", () => {
    const error = ApiError.fromProblem(400, {
      code: "validation.failed",
      detail: "2 fields need attention.",
      errors: { email: "Enter a valid email", password: "Use 8 to 72 characters" },
      requestId: "req-1",
    });
    expect(error.status).toBe(400);
    expect(error.is("validation.failed")).toBe(true);
    expect(error.message).toBe("2 fields need attention.");
    expect(error.fieldErrors.email).toBe("Enter a valid email");
    expect(error.requestId).toBe("req-1");
  });

  it("gives a plain message when the body isn't a problem", () => {
    expect(ApiError.fromProblem(502, "<html>Bad gateway</html>").message).toMatch(/our side/);
    expect(ApiError.fromProblem(502, undefined).code).toBe("server.error");
    expect(ApiError.fromProblem(404, null).message).toMatch(/couldn't find/);
    expect(ApiError.fromProblem(429, {}).message).toMatch(/Too many/);
  });

  it("describes a network failure", () => {
    const error = ApiError.network();
    expect(error.status).toBe(0);
    expect(error.code).toBe("network");
  });

  it("turns anything thrown into a message", () => {
    expect(errorMessage(new ApiError(422, "day.closed", "This day is closed."))).toBe("This day is closed.");
    expect(errorMessage(new Error("boom"))).toBe("boom");
    expect(errorMessage("nope")).toMatch(/Something went wrong/);
  });
});
