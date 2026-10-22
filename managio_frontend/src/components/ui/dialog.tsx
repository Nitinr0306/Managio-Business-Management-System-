"use client";

import { X } from "lucide-react";
import { Dialog as Primitive } from "radix-ui";
import type { ReactNode } from "react";
import { cn } from "@/lib/cn";

type DialogProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  /** Usually a form component that renders DialogBody and DialogFooter; it mounts fresh every time the dialog opens. */
  children: ReactNode;
  size?: "sm" | "md" | "lg";
};

const widths = { sm: "max-w-[420px]", md: "max-w-[560px]", lg: "max-w-[760px]" };

/** A centred modal for one focused task: a form or a confirmation. */
export function Dialog({ open, onOpenChange, title, description, children, size = "md" }: DialogProps) {
  return (
    <Primitive.Root open={open} onOpenChange={onOpenChange}>
      <Primitive.Portal>
        <Primitive.Overlay className="fixed inset-0 z-40 bg-ink/30 animate-fade-in" />
        <Primitive.Content
          className={cn(
            "fixed left-1/2 top-[8vh] z-50 flex max-h-[84vh] w-[calc(100vw-32px)] -translate-x-1/2 flex-col",
            "rounded-[var(--radius-panel)] bg-surface shadow-pop animate-fade-in focus:outline-none",
            widths[size],
          )}
        >
          <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
            <div className="min-w-0">
              <Primitive.Title className="text-[16px] font-semibold text-ink">{title}</Primitive.Title>
              {description ? (
                <Primitive.Description className="mt-0.5 text-[13px] text-muted">{description}</Primitive.Description>
              ) : (
                <Primitive.Description className="sr-only">{title}</Primitive.Description>
              )}
            </div>
            <Primitive.Close className="-mr-1 rounded p-1 text-faint hover:bg-canvas hover:text-ink" aria-label="Close">
              <X className="h-4 w-4" />
            </Primitive.Close>
          </div>
          {children}
        </Primitive.Content>
      </Primitive.Portal>
    </Primitive.Root>
  );
}

/** The scrolling middle of a dialog. */
export function DialogBody({ children, className }: { children: ReactNode; className?: string }) {
  return <div className={cn("min-h-0 flex-1 overflow-y-auto px-5 py-4", className)}>{children}</div>;
}

/** The dialog's buttons, pinned below the body. */
export function DialogFooter({ children }: { children: ReactNode }) {
  return <div className="flex flex-wrap justify-end gap-2 border-t border-line px-5 py-3">{children}</div>;
}

type SheetProps = {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: ReactNode;
  description?: ReactNode;
  children: ReactNode;
  footer?: ReactNode;
};

/** A panel that slides in from the right, keeping the list behind it in view (the member panel). */
export function Sheet({ open, onOpenChange, title, description, children, footer }: SheetProps) {
  return (
    <Primitive.Root open={open} onOpenChange={onOpenChange}>
      <Primitive.Portal>
        <Primitive.Overlay className="fixed inset-0 z-40 bg-ink/20 animate-fade-in lg:bg-ink/10" />
        <Primitive.Content
          className="fixed inset-y-0 right-0 z-50 flex w-full max-w-[460px] flex-col border-l border-line bg-surface shadow-pop animate-slide-in focus:outline-none"
        >
          <div className="flex items-start justify-between gap-4 border-b border-line px-5 py-4">
            <div className="min-w-0">
              <Primitive.Title className="truncate text-[17px] font-semibold text-ink">{title}</Primitive.Title>
              {description ? (
                <Primitive.Description className="mt-0.5 text-[13px] text-muted">{description}</Primitive.Description>
              ) : (
                <Primitive.Description className="sr-only">{title}</Primitive.Description>
              )}
            </div>
            <Primitive.Close className="-mr-1 rounded p-1 text-faint hover:bg-canvas hover:text-ink" aria-label="Close">
              <X className="h-4 w-4" />
            </Primitive.Close>
          </div>
          <div className="min-h-0 flex-1 overflow-y-auto">{children}</div>
          {footer && <div className="flex flex-wrap gap-2 border-t border-line px-5 py-3">{footer}</div>}
        </Primitive.Content>
      </Primitive.Portal>
    </Primitive.Root>
  );
}
