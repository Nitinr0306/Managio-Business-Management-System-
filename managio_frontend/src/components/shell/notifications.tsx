"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Bell } from "lucide-react";
import { useRouter } from "next/navigation";
import { Popover } from "radix-ui";
import { useState } from "react";
import { api, unwrap } from "@/lib/api/client";
import { cn } from "@/lib/cn";
import { formatDateTime } from "@/lib/format";

const countKey = ["me", "notifications", "count"] as const;
const listKey = ["me", "notifications", "list"] as const;

/** The bell: decisions on your requests and new requests waiting for you, across all your businesses (REQ-4). */
export function Notifications() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const count = useQuery({
    queryKey: countKey,
    queryFn: () => unwrap(api.GET("/api/v1/me/notifications/count")),
    refetchInterval: 60_000,
  });
  const list = useQuery({
    queryKey: listKey,
    queryFn: () => unwrap(api.GET("/api/v1/me/notifications", { params: { query: { limit: 20 } } })),
    enabled: open,
  });
  const markRead = useMutation({
    mutationFn: (ids?: number[]) => unwrap(api.POST("/api/v1/me/notifications/read", { body: { ids: ids ?? [] } })),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["me", "notifications"] }),
  });
  const unread = (count.data as { unread?: number } | undefined)?.unread ?? 0;

  return (
    <Popover.Root open={open} onOpenChange={setOpen}>
      <Popover.Trigger
        className="relative grid h-9 w-9 place-items-center rounded-[var(--radius-control)] text-muted hover:bg-black/[0.04] hover:text-ink"
        aria-label={unread > 0 ? `Notifications, ${unread} unread` : "Notifications"}
      >
        <Bell className="h-[18px] w-[18px]" aria-hidden />
        {unread > 0 && (
          <span className="absolute right-1 top-1 grid h-4 min-w-4 place-items-center rounded-full bg-bad px-1 text-[10px] font-bold text-white">
            {unread > 9 ? "9+" : unread}
          </span>
        )}
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Content
          align="end"
          sideOffset={6}
          className="z-50 w-[360px] max-w-[calc(100vw-24px)] rounded-[var(--radius-panel)] border border-line bg-surface shadow-pop animate-fade-in"
        >
          <div className="flex items-center justify-between border-b border-line px-4 py-2.5">
            <p className="text-[14px] font-semibold">Notifications</p>
            {unread > 0 && (
              <button type="button" className="text-[12.5px] font-medium text-cobalt hover:underline" onClick={() => markRead.mutate(undefined)}>
                Mark all read
              </button>
            )}
          </div>
          <div className="max-h-[420px] overflow-y-auto">
            {list.isPending ? (
              <p className="px-4 py-6 text-center text-[13px] text-muted">Loading…</p>
            ) : (list.data ?? []).length === 0 ? (
              <p className="px-4 py-8 text-center text-[13px] text-muted">Nothing new. Decisions on your requests show up here.</p>
            ) : (
              <ul className="divide-y divide-line">
                {(list.data ?? []).map((n) => (
                  <li key={n.id}>
                    <button
                      type="button"
                      onClick={() => {
                        if (!n.read) markRead.mutate([n.id]);
                        setOpen(false);
                        if (n.link) router.push(n.link);
                      }}
                      className={cn("flex w-full gap-3 px-4 py-3 text-left hover:bg-canvas", !n.read && "bg-cobalt-soft/40")}
                    >
                      <span className={cn("mt-1.5 h-2 w-2 shrink-0 rounded-full", n.read ? "bg-transparent" : "bg-cobalt")} aria-hidden />
                      <span className="min-w-0">
                        <span className="block text-[13.5px] font-medium text-ink">{n.title}</span>
                        {n.body && <span className="block text-[12.5px] text-muted">{n.body}</span>}
                        <span className="mt-0.5 block text-[11.5px] text-faint">{formatDateTime(n.createdAt)}</span>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  );
}
