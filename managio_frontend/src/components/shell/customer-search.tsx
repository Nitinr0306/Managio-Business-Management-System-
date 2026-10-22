"use client";

import { Search } from "lucide-react";
import { Popover } from "radix-ui";
import { useEffect, useId, useRef, useState } from "react";
import { MEMBERSHIP_STATE, useCustomers } from "@/features/customers/api";
import { useCustomerPanel } from "@/features/customers/panel-context";
import { useBusiness } from "@/features/business/context";
import { Pill } from "@/components/ui/pill";
import { Spinner } from "@/components/ui/spinner";
import { cn } from "@/lib/cn";
import { formatMoney } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";

/**
 * The always-visible customer search (D-001: borrowed from Variant B). Type a name, phone or #number and
 * press Enter to open that person's panel. "/" focuses it from anywhere.
 */
export function CustomerSearch() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const [text, setText] = useState("");
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);
  const listId = useId();
  const query = useDebounced(text.trim(), 180);
  const results = useCustomers({ q: query, size: 8, view: "ALL", sort: "NAME" }, query.length > 0);
  const items = query.length > 0 ? (results.data?.items ?? []) : [];

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement;
      const typing = target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName);
      if (event.key === "/" && !typing) {
        event.preventDefault();
        inputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  const choose = (customerId: number) => {
    panel.open(customerId);
    setOpen(false);
    setText("");
    inputRef.current?.blur();
  };

  return (
    <Popover.Root open={open && query.length > 0} onOpenChange={setOpen}>
      <Popover.Anchor asChild>
        <div className="relative w-full max-w-[520px]">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" aria-hidden />
          <input
            ref={inputRef}
            value={text}
            onChange={(event) => {
              setText(event.target.value);
              setActive(0);
              setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            onKeyDown={(event) => {
              if (event.key === "ArrowDown") {
                event.preventDefault();
                setActive((i) => Math.min(i + 1, Math.max(items.length - 1, 0)));
              } else if (event.key === "ArrowUp") {
                event.preventDefault();
                setActive((i) => Math.max(i - 1, 0));
              } else if (event.key === "Enter" && items[active]) {
                event.preventDefault();
                choose(items[active].id);
              } else if (event.key === "Escape") {
                setOpen(false);
              }
            }}
            role="combobox"
            aria-expanded={open && query.length > 0}
            aria-controls={listId}
            aria-autocomplete="list"
            aria-activedescendant={items[active] ? `${listId}-${items[active].id}` : undefined}
            aria-label={`Search ${business.labels.customers.toLowerCase()}`}
            placeholder={`Search ${business.labels.customers.toLowerCase()} by name, phone or #number`}
            className="h-9 w-full rounded-[var(--radius-control)] border border-line bg-surface pl-9 pr-10 text-[14px] placeholder:text-faint hover:border-line-strong focus:border-cobalt focus:outline-none focus:ring-2 focus:ring-cobalt/20"
          />
          <kbd className="pointer-events-none absolute right-2.5 top-1/2 hidden -translate-y-1/2 rounded border border-line px-1.5 text-[11px] text-faint sm:block">
            /
          </kbd>
        </div>
      </Popover.Anchor>
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={6}
          onOpenAutoFocus={(event) => event.preventDefault()}
          className="z-50 w-[var(--radix-popover-trigger-width)] min-w-[320px] rounded-[var(--radius-panel)] border border-line bg-surface p-1 shadow-pop animate-fade-in"
        >
          {results.isFetching && items.length === 0 ? (
            <div className="flex items-center gap-2 px-3 py-3 text-[13px] text-muted">
              <Spinner className="h-4 w-4" /> Searching…
            </div>
          ) : items.length === 0 ? (
            <p className="px-3 py-3 text-[13px] text-muted">
              Nobody matches “{query}”. Search by name, phone (3 or more digits) or #number.
            </p>
          ) : (
            <ul id={listId} role="listbox" aria-label="Matching customers">
              {items.map((customer, index) => (
                <li
                  key={customer.id}
                  id={`${listId}-${customer.id}`}
                  role="option"
                  aria-selected={index === active}
                  onMouseEnter={() => setActive(index)}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => choose(customer.id)}
                  className={cn(
                    "flex cursor-pointer items-center gap-3 rounded px-2.5 py-2",
                    index === active && "bg-canvas",
                  )}
                >
                  <span className="w-10 shrink-0 text-[12px] tabular-nums text-faint">#{customer.number}</span>
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-[13.5px] font-medium text-ink">{customer.fullName}</span>
                    <span className="block truncate text-[12px] text-muted">
                      {[customer.phone, customer.planName].filter(Boolean).join(" · ") || "No phone"}
                    </span>
                  </span>
                  {customer.balanceDue > 0 ? (
                    <span className="text-[12.5px] font-medium tabular-nums text-bad">{formatMoney(customer.balanceDue)} due</span>
                  ) : (
                    <Pill tone={MEMBERSHIP_STATE[customer.membershipState].tone}>{MEMBERSHIP_STATE[customer.membershipState].label}</Pill>
                  )}
                </li>
              ))}
            </ul>
          )}
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}
