import { Slot } from "radix-ui";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/cn";
import { Spinner } from "./spinner";

const variants = {
  primary: "bg-cobalt text-white font-semibold hover:bg-cobalt-dark disabled:bg-cobalt/60",
  secondary: "border border-line bg-surface text-ink font-medium hover:bg-canvas disabled:text-faint",
  ghost: "text-muted font-medium hover:bg-black/[0.04] hover:text-ink disabled:text-faint",
  danger: "bg-bad text-white font-semibold hover:bg-[#a92e25] disabled:bg-bad/60",
  "danger-outline": "border border-bad/40 bg-surface text-bad font-medium hover:bg-bad-soft",
  link: "text-cobalt font-semibold hover:underline underline-offset-2 px-0 h-auto",
} as const;

const sizes = {
  sm: "h-8 px-3 text-[13px] gap-1.5",
  md: "h-9 px-3.5 text-[13.5px] gap-1.5",
  lg: "h-11 px-5 text-[15px] gap-2",
  icon: "h-8 w-8 justify-center p-0",
} as const;

export type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: keyof typeof variants;
  size?: keyof typeof sizes;
  loading?: boolean;
  /** Renders the child element (for example a Link) with button styling. */
  asChild?: boolean;
  icon?: ReactNode;
};

export function Button({
  variant = "secondary",
  size = "md",
  loading = false,
  asChild = false,
  icon,
  className,
  children,
  disabled,
  type,
  ...props
}: ButtonProps) {
  const classes = cn(
    "inline-flex shrink-0 items-center rounded-[var(--radius-control)] whitespace-nowrap transition-colors",
    "disabled:cursor-not-allowed",
    variants[variant],
    variant === "link" ? "" : sizes[size],
    className,
  );
  if (asChild) {
    return (
      <Slot.Root className={classes} {...props}>
        {children}
      </Slot.Root>
    );
  }
  return (
    <button type={type ?? "button"} className={classes} disabled={disabled || loading} aria-busy={loading || undefined} {...props}>
      {loading ? <Spinner className="h-4 w-4" /> : icon}
      {children}
    </button>
  );
}
