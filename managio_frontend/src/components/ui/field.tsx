import {
  cloneElement,
  isValidElement,
  useId,
  type InputHTMLAttributes,
  type ReactElement,
  type ReactNode,
  type SelectHTMLAttributes,
  type TextareaHTMLAttributes,
} from "react";
import { Search } from "lucide-react";
import { cn } from "@/lib/cn";

const control =
  "w-full rounded-[var(--radius-control)] border border-line bg-surface px-3 text-[14px] text-ink placeholder:text-faint " +
  "transition-colors hover:border-line-strong focus:border-cobalt focus:outline-none focus:ring-2 focus:ring-cobalt/20 " +
  "disabled:bg-canvas disabled:text-faint aria-[invalid=true]:border-bad aria-[invalid=true]:focus:ring-bad/20";

export function Input({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cn(control, "h-9", className)} {...props} />;
}

/** A search box with its icon. Name it with aria-label; the placeholder says what it searches. */
export function SearchInput({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className={cn("relative", className)}>
      <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" aria-hidden />
      <input type="search" className={cn(control, "h-9 pl-9")} {...props} />
    </div>
  );
}

export function Textarea({ className, rows = 3, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea rows={rows} className={cn(control, "py-2 leading-relaxed", className)} {...props} />;
}

export function Select({ className, children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <select
      className={cn(
        control,
        "h-9 appearance-none bg-[url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='16' height='16' fill='none' stroke='%238A93A3' stroke-width='2' viewBox='0 0 24 24'%3E%3Cpath d='m6 9 6 6 6-6'/%3E%3C/svg%3E\")] bg-[right_0.6rem_center] bg-no-repeat pr-8",
        className,
      )}
      {...props}
    >
      {children}
    </select>
  );
}

type FieldProps = {
  label: ReactNode;
  /** The control. Its id, aria-invalid and aria-describedby are wired to the label, hint and error. */
  children: ReactElement<{ id?: string; "aria-invalid"?: boolean; "aria-describedby"?: string }>;
  hint?: ReactNode;
  error?: string;
  optional?: boolean;
  className?: string;
};

/** A labelled form control with its hint and error message, accessible by default. */
export function Field({ label, children, hint, error, optional, className }: FieldProps) {
  const generated = useId();
  const id = (isValidElement(children) && children.props.id) || generated;
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(" ") || undefined;
  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={id} className="text-[13px] font-medium text-ink">
        {label}
        {optional && <span className="ml-1 font-normal text-faint">(optional)</span>}
      </label>
      {cloneElement(children, { id, "aria-invalid": error ? true : undefined, "aria-describedby": describedBy })}
      {hint && !error && (
        <p id={hintId} className="text-[12.5px] text-muted">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="text-[12.5px] font-medium text-bad">
          {error}
        </p>
      )}
    </div>
  );
}

/** A checkbox with its label to the right. The hint describes it without becoming part of its name. */
export function Checkbox({ label, hint, className, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: ReactNode; hint?: ReactNode }) {
  const id = useId();
  const hintId = hint ? `${id}-hint` : undefined;
  return (
    <div className={cn("flex items-start gap-2.5", className)}>
      <input
        id={id}
        type="checkbox"
        aria-describedby={hintId}
        className="mt-0.5 h-4 w-4 shrink-0 rounded border-line-strong accent-cobalt"
        {...props}
      />
      <div className="text-[13.5px] leading-5">
        <label htmlFor={id} className="text-ink">
          {label}
        </label>
        {hint && (
          <p id={hintId} className="text-[12.5px] text-muted">
            {hint}
          </p>
        )}
      </div>
    </div>
  );
}

/** A form-level error, for problems that aren't about one field. */
export function FormError({ message }: { message?: string | null }) {
  if (!message) return null;
  return (
    <div role="alert" className="rounded-[var(--radius-control)] border border-bad/30 bg-bad-soft px-3 py-2 text-[13px] text-bad">
      {message}
    </div>
  );
}
