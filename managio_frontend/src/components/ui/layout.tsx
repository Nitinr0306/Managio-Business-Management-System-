import type { CSSProperties, HTMLAttributes, ReactNode, TdHTMLAttributes, ThHTMLAttributes } from "react";
import { cn } from "@/lib/cn";

/** Page title row: title, a line of context, and the page's actions on the right. */
export function PageHeader({ title, subtitle, actions }: { title: ReactNode; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0">
        <h1 className="text-[22px] font-semibold leading-7 text-ink text-balance">{title}</h1>
        {subtitle && <p className="mt-0.5 text-[13px] text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </header>
  );
}

/** A bordered white section. Only sections that are separate objects get one; not every block. */
export function Panel({
  title,
  actions,
  children,
  className,
  bodyClassName,
  ...props
}: Omit<HTMLAttributes<HTMLElement>, "title"> & { title?: ReactNode; actions?: ReactNode; bodyClassName?: string }) {
  return (
    <section className={cn("rounded-[var(--radius-panel)] border border-line bg-surface", className)} {...props}>
      {(title || actions) && (
        <div className="flex min-h-12 flex-wrap items-center justify-between gap-2 border-b border-line px-5 py-2.5">
          {title && <h2 className="text-[15px] font-semibold text-ink">{title}</h2>}
          {actions && <div className="flex items-center gap-2">{actions}</div>}
        </div>
      )}
      <div className={bodyClassName}>{children}</div>
    </section>
  );
}

/** A horizontal strip of key figures, divided by hairlines rather than separate cards. */
export function KpiStrip({ items }: { items: { label: string; value: ReactNode; note?: ReactNode; tone?: "ok" | "due" | "bad" | "muted" }[] }) {
  const tone = { ok: "text-ok", due: "text-due", bad: "text-bad", muted: "text-muted" };
  return (
    <section
      aria-label="Key figures"
      style={{ "--kpi-cols": items.length } as CSSProperties}
      className="grid grid-cols-2 overflow-hidden rounded-[var(--radius-panel)] border border-line bg-surface md:grid-cols-[repeat(var(--kpi-cols),minmax(0,1fr))]"
    >
      {items.map((item, i) => (
        <div
          key={item.label}
          className={cn(
            "border-line px-5 py-4",
            i % 2 === 1 && "border-l",
            i > 1 && "border-t md:border-t-0",
            i > 0 && "md:border-l",
            // An odd last figure fills its row on phones instead of sitting beside an empty half.
            items.length % 2 === 1 && i === items.length - 1 && "col-span-2 md:col-span-1",
          )}
        >
          <div className="text-[12.5px] text-muted">{item.label}</div>
          <div className="mt-1 text-[24px] font-semibold leading-8 tracking-tight text-ink">{item.value}</div>
          {item.note && <div className={cn("mt-0.5 text-[12px]", tone[item.tone ?? "muted"])}>{item.note}</div>}
        </div>
      ))}
    </section>
  );
}

export function EmptyState({ icon, title, body, action, className }: { icon?: ReactNode; title: ReactNode; body?: ReactNode; action?: ReactNode; className?: string }) {
  return (
    <div className={cn("flex flex-col items-center px-6 py-12 text-center", className)}>
      {icon && <div className="mb-3 grid h-10 w-10 place-items-center rounded-full bg-canvas text-faint">{icon}</div>}
      <p className="text-[15px] font-semibold text-ink">{title}</p>
      {body && <p className="mt-1 max-w-[46ch] text-[13.5px] text-muted text-pretty">{body}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cn("animate-pulse rounded bg-line/70", className)} aria-hidden />;
}

/* ── Tables: the primary way Runway shows records ─────────────────────────────────────────── */

/**
 * A table that scrolls sideways when it is wider than the screen. A table whose rows have nothing to focus
 * passes a `label`, so keyboard users can focus the scroll area and scroll it with the arrow keys.
 */
export function Table({ children, className, label }: { children: ReactNode; className?: string; label?: string }) {
  return (
    // relative: visually hidden header labels are absolutely positioned and would otherwise widen the page.
    <div
      className="relative overflow-x-auto focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-cobalt"
      {...(label ? { role: "region", "aria-label": label, tabIndex: 0 } : {})}
    >
      <table className={cn("w-full border-collapse text-[13.5px]", className)}>{children}</table>
    </div>
  );
}

export function THead({ children }: { children: ReactNode }) {
  return <thead className="text-left text-[12.5px] text-faint">{children}</thead>;
}

export function TH({ className, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return <th className={cn("h-9 whitespace-nowrap px-3 font-medium first:pl-5 last:pr-5", className)} {...props} />;
}

export function TR({ className, onClick, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={cn("border-t border-line", onClick && "cursor-pointer hover:bg-canvas/70", className)}
      onClick={onClick}
      {...props}
    />
  );
}

export function TD({ className, ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return <td className={cn("h-11 px-3 align-middle first:pl-5 last:pr-5", className)} {...props} />;
}

/** A definition list for record details: label on the left, value on the right. */
export function Details({ items }: { items: { label: string; value: ReactNode }[] }) {
  return (
    <dl className="divide-y divide-line">
      {items.map((item) => (
        <div key={item.label} className="grid grid-cols-[132px_1fr] gap-3 px-5 py-2.5 text-[13.5px]">
          <dt className="text-muted">{item.label}</dt>
          <dd className="min-w-0 break-words text-ink">{item.value ?? <span className="text-faint">—</span>}</dd>
        </div>
      ))}
    </dl>
  );
}
