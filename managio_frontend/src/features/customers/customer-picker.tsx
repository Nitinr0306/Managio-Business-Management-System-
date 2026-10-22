"use client";

import { Search, X } from "lucide-react";
import { Popover } from "radix-ui";
import { useId, useState } from "react";
import { cn } from "@/lib/cn";
import { useDebounced } from "@/lib/use-debounced";
import { useBusiness } from "@/features/business/context";
import { useCustomers } from "./api";

export type PickedCustomer = { id: number; name: string; number?: number };

/** Pick one customer by typing a name, phone or #number. Used in forms (sales, tasks). */
export function CustomerPicker({ value, onChange, id, placeholder }: {
  value: PickedCustomer | null;
  onChange: (customer: PickedCustomer | null) => void;
  id?: string;
  placeholder?: string;
}) {
  const business = useBusiness();
  const [text, setText] = useState("");
  const [open, setOpen] = useState(false);
  const q = useDebounced(text.trim(), 180);
  const results = useCustomers({ q, size: 8, view: "ALL", sort: "NAME" }, q.length > 0);
  const listId = useId();

  if (value) {
    return (
      <div className="flex h-9 items-center justify-between rounded-[var(--radius-control)] border border-line bg-surface px-3 text-[14px]">
        <span className="truncate">
          {value.name}
          {value.number != null && <span className="ml-2 text-[12px] tabular-nums text-faint">#{value.number}</span>}
        </span>
        <button type="button" onClick={() => onChange(null)} className="rounded p-0.5 text-faint hover:text-ink" aria-label={`Remove ${value.name}`}>
          <X className="h-4 w-4" />
        </button>
      </div>
    );
  }

  const items = results.data?.items ?? [];
  return (
    <Popover.Root open={open && q.length > 0} onOpenChange={setOpen}>
      <Popover.Anchor asChild>
        <div className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" aria-hidden />
          <input
            id={id}
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setOpen(true);
            }}
            onFocus={() => setOpen(true)}
            role="combobox"
            aria-expanded={open && q.length > 0}
            aria-controls={listId}
            placeholder={placeholder ?? `Search ${business.labels.customers.toLowerCase()}`}
            className="h-9 w-full rounded-[var(--radius-control)] border border-line bg-surface pl-9 pr-3 text-[14px] placeholder:text-faint hover:border-line-strong focus:border-cobalt focus:outline-none focus:ring-2 focus:ring-cobalt/20"
          />
        </div>
      </Popover.Anchor>
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={4}
          onOpenAutoFocus={(e) => e.preventDefault()}
          className="z-[60] w-[var(--radix-popover-trigger-width)] rounded-[var(--radius-panel)] border border-line bg-surface p-1 shadow-pop"
        >
          {items.length === 0 ? (
            <p className="px-3 py-2.5 text-[13px] text-muted">{results.isFetching ? "Searching…" : `Nobody matches “${q}”`}</p>
          ) : (
            <ul id={listId} role="listbox">
              {items.map((c) => (
                <li key={c.id} role="option" aria-selected={false}>
                  <button
                    type="button"
                    onMouseDown={(e) => e.preventDefault()}
                    onClick={() => {
                      onChange({ id: c.id, name: c.fullName, number: c.number });
                      setText("");
                      setOpen(false);
                    }}
                    className={cn("flex w-full items-center gap-3 rounded px-2.5 py-2 text-left hover:bg-canvas")}
                  >
                    <span className="w-10 text-[12px] tabular-nums text-faint">#{c.number}</span>
                    <span className="min-w-0 flex-1 truncate text-[13.5px] font-medium">{c.fullName}</span>
                    {c.phone && <span className="text-[12px] tabular-nums text-muted">{c.phone}</span>}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}
