"use client";

import { Menu as MenuIcon, Plus } from "lucide-react";
import Link from "next/link";
import { useParams, usePathname, useRouter } from "next/navigation";
import { Dialog as Primitive } from "radix-ui";
import { useState, type ReactNode } from "react";
import { Button } from "@/components/ui/button";
import { EmptyState, Skeleton } from "@/components/ui/layout";
import { Menu, MenuItem } from "@/components/ui/menu";
import { BusinessProvider, useBusiness, useMyBusinesses } from "@/features/business/context";
import { CustomerDialog } from "@/features/customers/customer-dialog";
import { CustomerPanelProvider, useCustomerPanel } from "@/features/customers/panel-context";
import { errorMessage } from "@/lib/api/errors";
import { CustomerSearch } from "./customer-search";
import { canOpen } from "./nav";
import { Notifications } from "./notifications";
import { Sidebar } from "./sidebar";

/** The signed-in frame for one business: sidebar, top bar with search, and the customer panel. */
export function BusinessShell({ children }: { children: ReactNode }) {
  const params = useParams<{ businessId: string }>();
  const businessId = Number(params.businessId);
  const businesses = useMyBusinesses();
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);

  if (businesses.isPending) {
    return <ShellSkeleton />;
  }
  if (businesses.isError) {
    return (
      <div className="grid min-h-screen place-items-center px-4">
        <EmptyState title="Managio couldn't load your businesses" body={errorMessage(businesses.error)}
          action={<Button onClick={() => businesses.refetch()}>Try again</Button>} />
      </div>
    );
  }
  const business = businesses.data.find((b) => b.id === businessId);
  if (!business) {
    return (
      <div className="grid min-h-screen place-items-center px-4">
        <EmptyState
          title="You don't have access to this business"
          body="It may have been archived, or your access was removed. Ask the owner if you think this is a mistake."
          action={<Button asChild variant="primary"><Link href="/app">Go to your businesses</Link></Button>}
        />
      </div>
    );
  }

  return (
    <BusinessProvider business={business}>
      <CustomerPanelProvider>
        <div className="flex min-h-screen">
          <div className="sticky top-0 hidden h-screen lg:block">
            <Sidebar business={business} businesses={businesses.data} />
          </div>
          <Primitive.Root open={menuOpen} onOpenChange={setMenuOpen}>
            <Primitive.Portal>
              <Primitive.Overlay className="fixed inset-0 z-40 bg-ink/30 lg:hidden" />
              <Primitive.Content className="fixed inset-y-0 left-0 z-50 animate-fade-in lg:hidden" aria-describedby={undefined}>
                <Primitive.Title className="sr-only">Menu</Primitive.Title>
                <Sidebar business={business} businesses={businesses.data} onNavigate={() => setMenuOpen(false)} />
              </Primitive.Content>
            </Primitive.Portal>
          </Primitive.Root>
          <div className="flex min-w-0 flex-1 flex-col">
            <Topbar onMenu={() => setMenuOpen(true)} />
            <main id="main" className="mx-auto w-full max-w-[1280px] flex-1 px-4 py-6 sm:px-8 sm:py-7">
              {canOpen(business, pathname) ? (
                children
              ) : (
                <EmptyState
                  title="This screen isn't part of your role"
                  body={`Your role in ${business.name} (${business.roleName}) doesn't include it. Ask the owner if you need it.`}
                  action={<Button asChild><Link href={`/app/${business.id}`}>Go to Today</Link></Button>}
                />
              )}
            </main>
          </div>
        </div>
      </CustomerPanelProvider>
    </BusinessProvider>
  );
}

function Topbar({ onMenu }: { onMenu: () => void }) {
  const business = useBusiness();
  return (
    <div className="sticky top-0 z-30 flex h-14 items-center gap-2 border-b border-line bg-canvas/90 px-3 backdrop-blur sm:gap-3 sm:px-8">
      <Button variant="ghost" size="icon" className="lg:hidden" aria-label="Open menu" onClick={onMenu}>
        <MenuIcon className="h-5 w-5" />
      </Button>
      {business.can("customers.view") ? <CustomerSearch /> : <div className="flex-1" />}
      <div className="ml-auto flex items-center gap-1">
        <QuickAdd />
        <Notifications />
      </div>
    </div>
  );
}

/** The "New" menu: the handful of things people add many times a day. */
function QuickAdd() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const router = useRouter();
  const [adding, setAdding] = useState(false);
  const base = `/app/${business.id}`;
  return (
    <>
      <Menu trigger={<Button variant="primary" size="sm" aria-label="New" icon={<Plus className="h-4 w-4" />}><span className="hidden sm:inline" aria-hidden>New</span></Button>}>
        {business.can("customers.manage") && <MenuItem onSelect={() => setAdding(true)}>{business.labels.customer}</MenuItem>}
        {business.can("billing.collect") && (
          <MenuItem onSelect={() => router.push(`${base}/billing?new=sale`)}>Sale or invoice</MenuItem>
        )}
        {business.can("expenses.record") && <MenuItem onSelect={() => router.push(`${base}/expenses?new=1`)}>Expense</MenuItem>}
        <MenuItem onSelect={() => router.push(`${base}/tasks?new=1`)}>Task</MenuItem>
        <MenuItem onSelect={() => router.push(`${base}/requests?new=1`)}>Request</MenuItem>
      </Menu>
      <CustomerDialog open={adding} onOpenChange={setAdding} onSaved={(c) => panel.open(c.id)} />
    </>
  );
}

export function ShellSkeleton() {
  return (
    <div className="flex min-h-screen">
      <div className="hidden w-[240px] border-r border-line bg-canvas p-3 lg:block">
        <Skeleton className="h-12 w-full" />
        <div className="mt-4 space-y-2">
          {Array.from({ length: 8 }, (_, i) => (
            <Skeleton key={i} className="h-7 w-full" />
          ))}
        </div>
      </div>
      <div className="flex-1">
        <div className="h-14 border-b border-line" />
        <div className="mx-auto max-w-[1280px] space-y-4 px-8 py-7">
          <Skeleton className="h-7 w-48" />
          <Skeleton className="h-24 w-full" />
          <Skeleton className="h-64 w-full" />
        </div>
      </div>
    </div>
  );
}
