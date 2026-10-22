"use client";

import { DropdownMenu } from "radix-ui";
import type { KeyboardEvent, ReactNode } from "react";
import { cn } from "@/lib/cn";

/** A menu of secondary actions behind a trigger (usually a "More" button). */
export function Menu({ trigger, children, align = "end" }: { trigger: ReactNode; children: ReactNode; align?: "start" | "end" }) {
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger asChild>{trigger}</DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align={align}
          sideOffset={6}
          className="z-50 min-w-[200px] rounded-[var(--radius-panel)] border border-line bg-surface p-1 shadow-pop animate-fade-in"
        >
          {children}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}

export function MenuItem({ children, onSelect, danger, disabled, icon }: { children: ReactNode; onSelect: () => void; danger?: boolean; disabled?: boolean; icon?: ReactNode }) {
  return (
    <DropdownMenu.Item
      disabled={disabled}
      onSelect={onSelect}
      className={cn(
        "flex h-8 cursor-pointer select-none items-center gap-2 rounded px-2 text-[13.5px] outline-none",
        "data-[highlighted]:bg-canvas data-[disabled]:cursor-not-allowed data-[disabled]:text-faint",
        danger ? "text-bad" : "text-ink",
      )}
    >
      {icon && <span className="text-faint">{icon}</span>}
      {children}
    </DropdownMenu.Item>
  );
}

export function MenuSeparator() {
  return <DropdownMenu.Separator className="my-1 h-px bg-line" />;
}

export function MenuLabel({ children }: { children: ReactNode }) {
  return <DropdownMenu.Label className="px-2 py-1.5 text-[12px] font-medium text-faint">{children}</DropdownMenu.Label>;
}

/**
 * Underlined tabs for switching views inside one page. The view below them is rendered by the page, so the tabs
 * carry no aria-controls. Arrow keys move between tabs, as screen-reader users expect.
 */
export function Tabs({ value, onValueChange, items }: {
  value: string;
  onValueChange: (value: string) => void;
  items: { value: string; label: ReactNode; count?: number }[];
}) {
  const move = (event: KeyboardEvent<HTMLButtonElement>, index: number) => {
    const step = event.key === "ArrowRight" ? 1 : event.key === "ArrowLeft" ? -1 : 0;
    if (!step) return;
    event.preventDefault();
    const next = items[(index + step + items.length) % items.length];
    onValueChange(next.value);
    const buttons = event.currentTarget.parentElement?.querySelectorAll<HTMLButtonElement>("[role=tab]");
    buttons?.[(index + step + items.length) % items.length]?.focus();
  };
  return (
    <div role="tablist" aria-label="Views" className="flex gap-5 overflow-x-auto border-b border-line">
      {items.map((item, index) => {
        const active = item.value === value;
        return (
          <button
            key={item.value}
            type="button"
            role="tab"
            aria-selected={active}
            tabIndex={active ? 0 : -1}
            onClick={() => onValueChange(item.value)}
            onKeyDown={(event) => move(event, index)}
            className={cn(
              "-mb-px flex h-10 items-center gap-1.5 whitespace-nowrap border-b-2 text-[13.5px] font-medium",
              active ? "border-cobalt text-ink" : "border-transparent text-muted hover:text-ink",
            )}
          >
            {item.label}
            {item.count != null && <span className="rounded-full bg-canvas px-1.5 text-[11.5px] tabular-nums text-muted">{item.count}</span>}
          </button>
        );
      })}
    </div>
  );
}

/** A compact segmented control for filters. */
export function Segmented<T extends string>({ value, onChange, options, label }: {
  value: T;
  onChange: (value: T) => void;
  options: { value: T; label: ReactNode; count?: number }[];
  label: string;
}) {
  return (
    <div role="radiogroup" aria-label={label} className="inline-flex flex-wrap gap-1 rounded-[var(--radius-control)] bg-black/[0.04] p-0.5">
      {options.map((option) => {
        const active = option.value === value;
        return (
          <button
            key={option.value}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(option.value)}
            className={cn(
              "inline-flex h-7 items-center gap-1.5 rounded-[5px] px-2.5 text-[13px] font-medium",
              active ? "bg-surface text-ink shadow-[0_0_0_1px_var(--color-line)]" : "text-muted hover:text-ink",
            )}
          >
            {option.label}
            {option.count != null && <span className="tabular-nums text-faint">{option.count}</span>}
          </button>
        );
      })}
    </div>
  );
}
