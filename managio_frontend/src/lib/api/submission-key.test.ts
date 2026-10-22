import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useSubmissionKey } from "./submission-key";

describe("useSubmissionKey", () => {
  it("repeats one key until the action succeeds, then starts a new one", () => {
    const { result } = renderHook(() => useSubmissionKey());
    const first = result.current.header()["Idempotency-Key"];

    expect(first).toMatch(/^[0-9a-f]{32}$/);
    expect(result.current.header()["Idempotency-Key"]).toBe(first);

    result.current.renew();
    const second = result.current.header()["Idempotency-Key"];
    expect(second).toMatch(/^[0-9a-f]{32}$/);
    expect(second).not.toBe(first);
  });

  it("gives each form its own key", () => {
    const one = renderHook(() => useSubmissionKey()).result.current.header()["Idempotency-Key"];
    const two = renderHook(() => useSubmissionKey()).result.current.header()["Idempotency-Key"];
    expect(one).not.toBe(two);
  });
});
