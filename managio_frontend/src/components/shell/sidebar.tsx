"use client";

import { Building, Check, ChevronsUpDown, LayoutGrid, LogOut, Plus, UserRound } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { DropdownMenu } from "radix-ui";
import type { BusinessAccess } from "@/features/business/access";
import { signOut } from "@/features/auth/api";
import { useStaffSession } from "@/lib/auth/use-session";
import { cn } from "@/lib/cn";
import { initials } from "@/lib/format";
import { navigation, type NavItem } from "./nav";

export function Sidebar({ business, businesses, onNavigate }: {
  business: BusinessAccess;
  businesses: BusinessAccess[];
  onNavigate?: () => void;
}) {
  const pathname = usePathname();
  const { main, manage, settings } = navigation(business);
  const isActive = (item: NavItem) => (item.exact ? pathname === item.href : pathname === item.href || pathname.startsWith(`${item.href}/`));

  return (
    <aside className="flex h-full w-[240px] shrink-0 flex-col border-r border-line bg-canvas">
      <BusinessSwitcher current={business} businesses={businesses} onNavigate={onNavigate} />
      <nav className="flex flex-1 flex-col overflow-y-auto px-3 py-2" aria-label="Main">
        <ul className="space-y-0.5">
          {main.map((item) => (
            <li key={item.href}>
              <NavLink item={item} active={isActive(item)} onNavigate={onNavigate} />
            </li>
          ))}
        </ul>
        {manage.length > 0 && (
          <>
            <p className="mt-5 mb-1 px-2.5 text-[12px] font-medium text-faint">Manage</p>
            <ul className="space-y-0.5">
              {manage.map((item) => (
                <li key={item.href}>
                  <NavLink item={item} active={isActive(item)} onNavigate={onNavigate} />
                </li>
              ))}
            </ul>
          </>
        )}
        {settings && (
          <div className="mt-auto pt-4">
            <NavLink item={settings} active={isActive(settings)} onNavigate={onNavigate} />
          </div>
        )}
      </nav>
      <UserBlock roleName={business.roleName} />
    </aside>
  );
}

function NavLink({ item, active, onNavigate }: { item: NavItem; active: boolean; onNavigate?: () => void }) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      onClick={onNavigate}
      aria-current={active ? "page" : undefined}
      className={cn(
        "group flex h-8 items-center gap-2.5 rounded-md px-2.5 text-[13.5px]",
        active
          ? "bg-surface font-semibold text-cobalt shadow-[0_0_0_1px_var(--color-line)]"
          : "text-muted hover:bg-black/[0.03] hover:text-ink",
      )}
    >
      <Icon className={cn("h-4 w-4 shrink-0", active ? "" : "text-faint group-hover:text-muted")} aria-hidden />
      <span className="truncate">{item.label}</span>
    </Link>
  );
}

function BusinessSwitcher({ current, businesses, onNavigate }: { current: BusinessAccess; businesses: BusinessAccess[]; onNavigate?: () => void }) {
  const router = useRouter();
  const go = (href: string) => {
    onNavigate?.();
    router.push(href);
  };
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger
        className="m-3 mb-2 flex items-center gap-2.5 rounded-md p-2 text-left hover:bg-black/[0.03]"
        aria-label={`Switch business. Current: ${current.name}`}
      >
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-md bg-ink text-[12.5px] font-bold text-white">
          {initials(current.name)}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block truncate text-[13px] font-semibold leading-[17px] text-ink">{current.name}</span>
          <span className="block text-[12px] tabular-nums text-faint">{current.code}</span>
        </span>
        <ChevronsUpDown className="h-4 w-4 shrink-0 text-faint" aria-hidden />
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="start"
          sideOffset={4}
          className="z-50 w-[260px] rounded-[var(--radius-panel)] border border-line bg-surface p-1 shadow-pop animate-fade-in"
        >
          <DropdownMenu.Label className="px-2 py-1.5 text-[12px] font-medium text-faint">Your businesses</DropdownMenu.Label>
          {businesses.map((b) => (
            <DropdownMenu.Item
              key={b.id}
              onSelect={() => go(`/app/${b.id}`)}
              className="flex cursor-pointer items-center gap-2.5 rounded px-2 py-1.5 outline-none data-[highlighted]:bg-canvas"
            >
              <span className="grid h-6 w-6 place-items-center rounded bg-ink/90 text-[10.5px] font-bold text-white">{initials(b.name)}</span>
              <span className="min-w-0 flex-1">
                <span className="block truncate text-[13px] font-medium text-ink">{b.name}</span>
                <span className="block text-[11.5px] text-faint">{b.roleName}</span>
              </span>
              {b.id === current.id && <Check className="h-4 w-4 text-cobalt" aria-label="Current" />}
            </DropdownMenu.Item>
          ))}
          <DropdownMenu.Separator className="my-1 h-px bg-line" />
          {businesses.length > 1 && (
            <DropdownMenu.Item
              onSelect={() => go("/app?view=all")}
              className="flex h-8 cursor-pointer items-center gap-2 rounded px-2 text-[13px] outline-none data-[highlighted]:bg-canvas"
            >
              <LayoutGrid className="h-4 w-4 text-faint" aria-hidden /> All businesses
            </DropdownMenu.Item>
          )}
          <DropdownMenu.Item
            onSelect={() => go("/app/new")}
            className="flex h-8 cursor-pointer items-center gap-2 rounded px-2 text-[13px] outline-none data-[highlighted]:bg-canvas"
          >
            <Plus className="h-4 w-4 text-faint" aria-hidden /> Add a business
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}

function UserBlock({ roleName }: { roleName: string }) {
  const session = useStaffSession();
  const router = useRouter();
  const user = session.principal;
  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger className="flex items-center gap-2.5 border-t border-line p-3 text-left hover:bg-black/[0.02]">
        <span className="grid h-8 w-8 shrink-0 place-items-center rounded-full bg-cobalt-soft text-[12px] font-semibold text-cobalt">
          {initials(user?.fullName)}
        </span>
        <span className="min-w-0 flex-1 text-[13px] leading-tight">
          <span className="block truncate font-medium text-ink">{user?.fullName}</span>
          <span className="block truncate text-faint">{roleName}</span>
        </span>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          side="top"
          align="start"
          sideOffset={4}
          className="z-50 w-[220px] rounded-[var(--radius-panel)] border border-line bg-surface p-1 shadow-pop animate-fade-in"
        >
          <DropdownMenu.Label className="truncate px-2 py-1.5 text-[12px] text-faint">{user?.email}</DropdownMenu.Label>
          <DropdownMenu.Item
            onSelect={() => router.push("/app/account")}
            className="flex h-8 cursor-pointer items-center gap-2 rounded px-2 text-[13px] outline-none data-[highlighted]:bg-canvas"
          >
            <UserRound className="h-4 w-4 text-faint" aria-hidden /> Your account
          </DropdownMenu.Item>
          <DropdownMenu.Item
            onSelect={() => router.push("/app/new")}
            className="flex h-8 cursor-pointer items-center gap-2 rounded px-2 text-[13px] outline-none data-[highlighted]:bg-canvas"
          >
            <Building className="h-4 w-4 text-faint" aria-hidden /> Add a business
          </DropdownMenu.Item>
          <DropdownMenu.Separator className="my-1 h-px bg-line" />
          <DropdownMenu.Item
            onSelect={async () => {
              await signOut().catch(() => undefined);
              router.replace("/sign-in");
            }}
            className="flex h-8 cursor-pointer items-center gap-2 rounded px-2 text-[13px] text-ink outline-none data-[highlighted]:bg-canvas"
          >
            <LogOut className="h-4 w-4 text-faint" aria-hidden /> Sign out
          </DropdownMenu.Item>
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
