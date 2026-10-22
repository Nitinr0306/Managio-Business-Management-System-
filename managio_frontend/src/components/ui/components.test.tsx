import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RunwayChart } from "@/features/reports/runway-chart";
import { Button } from "./button";
import { Checkbox, Field, Input } from "./field";
import { Pagination } from "./pagination";
import { Pill } from "./pill";

describe("Field", () => {
  it("connects the label, hint and error to the control", () => {
    render(
      <Field label="Email" hint="We'll send the receipt here" error="Enter a valid email">
        <Input />
      </Field>,
    );
    const input = screen.getByLabelText("Email");
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(input).toHaveAccessibleDescription("Enter a valid email");
    expect(screen.queryByText("We'll send the receipt here")).not.toBeInTheDocument();
  });

  it("shows the hint when there is no error", () => {
    render(
      <Field label="Phone" hint="10 digits" optional>
        <Input />
      </Field>,
    );
    expect(screen.getByLabelText(/Phone/)).toHaveAccessibleDescription("10 digits");
    expect(screen.getByText("(optional)")).toBeInTheDocument();
  });
});

describe("Checkbox", () => {
  it("is named by its label and described by its hint", () => {
    render(<Checkbox label="Stock tracking" hint="Count stock for products" />);
    const box = screen.getByRole("checkbox", { name: "Stock tracking" });
    expect(box).toHaveAccessibleDescription("Count stock for products");
  });
});

describe("Button", () => {
  it("is busy and can't be pressed twice while loading", async () => {
    const onClick = vi.fn();
    render(<Button loading onClick={onClick}>Record payment</Button>);
    const button = screen.getByRole("button", { name: "Record payment" });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("aria-busy", "true");
    await userEvent.click(button);
    expect(onClick).not.toHaveBeenCalled();
  });
});

describe("Pill", () => {
  it("shows its label", () => {
    render(<Pill tone="bad">Overdue</Pill>);
    expect(screen.getByText("Overdue")).toBeInTheDocument();
  });
});

describe("Pagination", () => {
  it("shows the range and moves between pages", async () => {
    const onPage = vi.fn();
    render(<Pagination page={1} size={25} total={1212} onPage={onPage} />);
    expect(screen.getByText("26–50 of 1,212")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Next page" }));
    expect(onPage).toHaveBeenCalledWith(2);
  });

  it("is hidden when everything fits on one page", () => {
    const { container } = render(<Pagination page={0} size={25} total={12} onPage={vi.fn()} />);
    expect(container).toBeEmptyDOMElement();
  });
});

describe("RunwayChart", () => {
  const days = Array.from({ length: 30 }, (_, i) => ({
    date: `2026-10-${String(20 + i).padStart(2, "0")}`.replace(/-(3[2-9]|4\d)$/, "-31"),
    ending: i === 2 ? 3 : i === 10 ? 1 : 0,
    value: i === 2 ? 4500 : i === 10 ? 1500 : 0,
  }));

  it("describes the whole runway for screen readers", () => {
    render(<RunwayChart days={days} />);
    expect(screen.getByRole("img")).toHaveAccessibleName("Memberships ending each day over the next 30 days: 4 in total");
    expect(screen.getByText("Today")).toBeInTheDocument();
  });

  it("says so when nothing is ending", () => {
    render(<RunwayChart days={days.map((d) => ({ ...d, ending: 0, value: 0 }))} />);
    expect(screen.getByText("Nothing ends in the next 30 days without a renewal.")).toBeInTheDocument();
  });
});
