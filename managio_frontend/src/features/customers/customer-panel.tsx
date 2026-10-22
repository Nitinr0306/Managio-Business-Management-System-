"use client";

import { Archive, Ellipsis, Mail, Pencil, Phone, Send, ShieldOff, Undo2 } from "lucide-react";
import Link from "next/link";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/dialog";
import { Details } from "@/components/ui/layout";
import { Menu, MenuItem, MenuSeparator } from "@/components/ui/menu";
import { Pill } from "@/components/ui/pill";
import { LoadingBlock } from "@/components/ui/spinner";
import { useBusiness } from "@/features/business/context";
import { INVOICE_STATE, useInvoices } from "@/features/billing/api";
import { PaymentDialog } from "@/features/billing/payment-dialog";
import { MEMBERSHIP_ITEM_STATE } from "@/features/memberships/api";
import { MembershipDialog } from "@/features/memberships/membership-dialog";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatMoney, humanize } from "@/lib/format";
import { MEMBERSHIP_STATE, useCustomer, useCustomerAction, useCustomerMemberships, type MembershipState } from "./api";
import { CustomerDialog } from "./customer-dialog";

type Membership = Schemas["MembershipResponse"];

/**
 * The customer panel (D-001, from Variant B). It leads with the single thing most likely to be needed:
 * take the money that's owed, otherwise renew, otherwise start a plan. Everything else is one menu away.
 */
export function CustomerPanel({ customerId, onClose }: { customerId: number | null; onClose: () => void }) {
  const customer = useCustomer(customerId);
  const memberships = useCustomerMemberships(customerId);
  const data = customer.data;

  return (
    <Sheet
      open={customerId != null}
      onOpenChange={(open) => !open && onClose()}
      title={data ? data.fullName : "Loading…"}
      description={data ? <PanelSubtitle customer={data} /> : undefined}
    >
      {customer.isPending ? (
        <LoadingBlock />
      ) : customer.isError ? (
        <p className="px-5 py-6 text-[13.5px] text-bad">{errorMessage(customer.error)}</p>
      ) : (
        <PanelBody customer={customer.data} memberships={memberships.data ?? []} />
      )}
    </Sheet>
  );
}

function PanelSubtitle({ customer }: { customer: Schemas["CustomerResponse"] }) {
  const state = MEMBERSHIP_STATE[customer.membershipState as MembershipState] ?? MEMBERSHIP_STATE.NONE;
  return (
    <span className="mt-1 flex flex-wrap items-center gap-2">
      <span className="tabular-nums">#{customer.number}</span>
      {customer.status === "ARCHIVED" ? <Pill tone="neutral">Archived</Pill> : <Pill tone={state.tone}>{state.label}</Pill>}
    </span>
  );
}

function PanelBody({ customer, memberships }: { customer: Schemas["CustomerResponse"]; memberships: Membership[] }) {
  const business = useBusiness();
  const action = useCustomerAction();
  const invoices = useInvoices({ view: "OPEN", customerId: customer.id, size: 20 }, business.can("billing.view"));
  const [dialog, setDialog] = useState<null | "edit" | "start" | "renew" | "pay">(null);

  const archived = customer.status === "ARCHIVED";
  const latest = memberships.find((m) => m.state !== "CANCELLED" && m.state !== "UPCOMING") ?? memberships.find((m) => m.state !== "CANCELLED");
  const renewable = latest && (latest.state === "ACTIVE" || latest.state === "ENDING_SOON" || latest.state === "EXPIRED");
  const alreadyRenewed = latest ? memberships.some((m) => m.renewedFromId === latest.id && m.state !== "CANCELLED") : false;
  const canManageMemberships = business.can("memberships.manage") && business.has("MEMBERSHIPS");
  const canCollect = business.can("billing.collect");

  type Action = { label: string; run: () => void };
  let primary: Action | null = null;
  if (!archived) {
    if (customer.balanceDue > 0 && canCollect) {
      primary = { label: `Record payment · ${formatMoney(customer.balanceDue)} due`, run: () => setDialog("pay") };
    } else if (renewable && !alreadyRenewed && canManageMemberships) {
      primary = { label: latest.state === "ACTIVE" ? "Renew early" : "Renew", run: () => setDialog("renew") };
    } else if (!latest && canManageMemberships) {
      primary = { label: `Start a ${business.labels.plan.toLowerCase()}`, run: () => setDialog("start") };
    }
  }

  const run = async (kind: "archive" | "restore" | "invite" | "disable", success: string) => {
    try {
      await action.mutateAsync({ customerId: customer.id, action: kind });
      toast.success(success);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <>
      <div className="flex flex-wrap items-center gap-2 border-b border-line px-5 py-4">
        {primary && (
          <Button variant="primary" onClick={primary.run}>
            {primary.label}
          </Button>
        )}
        <Menu trigger={<Button aria-label="More actions" icon={<Ellipsis className="h-4 w-4" />}>{primary ? "" : "Actions"}</Button>} align="start">
          {business.can("customers.manage") && (
            <MenuItem icon={<Pencil className="h-4 w-4" />} onSelect={() => setDialog("edit")} disabled={archived}>
              Edit details
            </MenuItem>
          )}
          {canManageMemberships && !archived && (
            <MenuItem onSelect={() => setDialog("start")}>Start another {business.labels.plan.toLowerCase()}</MenuItem>
          )}
          {canManageMemberships && renewable && !archived && (
            <MenuItem onSelect={() => setDialog("renew")}>Renew {latest.planName}</MenuItem>
          )}
          {canCollect && (customer.balanceDue > 0 || customer.upcoming > 0) && (
            <MenuItem onSelect={() => setDialog("pay")}>{customer.balanceDue > 0 ? "Record payment" : "Take an early payment"}</MenuItem>
          )}
          {business.can("customers.manage") && !archived && (
            customer.portalEnabled ? (
              <MenuItem icon={<ShieldOff className="h-4 w-4" />} onSelect={() => run("disable", "Portal access turned off")}>
                Turn off portal access
              </MenuItem>
            ) : (
              <MenuItem
                icon={<Send className="h-4 w-4" />}
                disabled={!customer.email}
                onSelect={() => run("invite", `Invitation sent to ${customer.email}`)}
              >
                {customer.email ? "Invite to the portal" : "Invite to the portal (add an email first)"}
              </MenuItem>
            )
          )}
          {business.can("customers.archive") && (
            <>
              <MenuSeparator />
              {archived ? (
                <MenuItem icon={<Undo2 className="h-4 w-4" />} onSelect={() => run("restore", `Restored ${customer.fullName}`)}>
                  Restore
                </MenuItem>
              ) : (
                <MenuItem danger icon={<Archive className="h-4 w-4" />} onSelect={() => run("archive", `Archived ${customer.fullName}`)}>
                  Archive
                </MenuItem>
              )}
            </>
          )}
        </Menu>
      </div>

      <section className="border-b border-line py-2">
        <Details
          items={[
            {
              label: "Phone",
              value: customer.phone ? (
                <a href={`tel:${customer.phone}`} className="inline-flex items-center gap-1.5 text-cobalt hover:underline">
                  <Phone className="h-3.5 w-3.5" aria-hidden />
                  {customer.phone}
                </a>
              ) : null,
            },
            {
              label: "Email",
              value: customer.email ? (
                <a href={`mailto:${customer.email}`} className="inline-flex items-center gap-1.5 text-cobalt hover:underline">
                  <Mail className="h-3.5 w-3.5" aria-hidden />
                  {customer.email}
                </a>
              ) : null,
            },
            { label: "Covered until", value: customer.coveredUntil ? formatDate(customer.coveredUntil) : null },
            {
              label: "Owes",
              value: customer.balanceDue > 0 ? <span className="font-semibold text-bad">{formatMoney(customer.balanceDue)}</span> : "Nothing",
            },
            ...(customer.upcoming > 0 ? [{ label: "Coming up", value: `${formatMoney(customer.upcoming)} in later instalments` }] : []),
            ...(customer.groups.length ? [{ label: business.labels.groups, value: customer.groups.map((g) => g.name).join(", ") }] : []),
            ...(customer.guardianName ? [{ label: "Guardian", value: [customer.guardianName, customer.guardianPhone].filter(Boolean).join(" · ") }] : []),
            ...(customer.gender ? [{ label: "Gender", value: humanize(customer.gender) }] : []),
            { label: "Joined", value: formatDate(customer.joinedOn) },
            { label: "Portal", value: customer.portalEnabled ? "Can sign in" : "Not invited" },
            ...(customer.notes ? [{ label: "Notes", value: <span className="whitespace-pre-wrap">{customer.notes}</span> }] : []),
          ]}
        />
      </section>

      {business.has("MEMBERSHIPS") && (
        <section className="border-b border-line px-5 py-4">
          <h3 className="mb-2 text-[13px] font-semibold text-ink">{business.labels.plans}</h3>
          {memberships.length === 0 ? (
            <p className="text-[13px] text-muted">No {business.labels.plans.toLowerCase()} yet.</p>
          ) : (
            <ul className="space-y-2">
              {memberships.slice(0, 5).map((m) => (
                <li key={m.id} className="flex items-center justify-between gap-3 text-[13px]">
                  <span className="min-w-0">
                    <span className="block truncate font-medium text-ink">{m.planName}</span>
                    <span className="text-muted tabular-nums">
                      {formatDate(m.startsOn)} – {formatDate(m.endsOn)}
                    </span>
                  </span>
                  <Pill tone={MEMBERSHIP_ITEM_STATE[m.state].tone}>{MEMBERSHIP_ITEM_STATE[m.state].label}</Pill>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {business.can("billing.view") && (
        <section className="px-5 py-4">
          <div className="mb-2 flex items-center justify-between">
            <h3 className="text-[13px] font-semibold text-ink">Open invoices</h3>
            <Link href={`/app/${business.id}/billing?customer=${customer.id}`} className="text-[12.5px] font-medium text-cobalt hover:underline">
              All billing
            </Link>
          </div>
          {(invoices.data?.items ?? []).length === 0 ? (
            <p className="text-[13px] text-muted">Nothing outstanding.</p>
          ) : (
            <ul className="space-y-2">
              {(invoices.data?.items ?? []).map((invoice) => (
                <li key={invoice.id} className="flex items-center justify-between gap-3 text-[13px]">
                  <Link href={`/app/${business.id}/billing/invoices/${invoice.id}`} className="min-w-0 hover:underline">
                    <span className="block font-medium text-ink">{invoice.invoiceNumber}</span>
                    <span className="text-muted">Due {formatDate(invoice.dueOn)}</span>
                  </Link>
                  <span className="flex items-center gap-2">
                    <span className="font-medium tabular-nums">{formatMoney(invoice.balance)}</span>
                    <Pill tone={INVOICE_STATE[invoice.state].tone}>{INVOICE_STATE[invoice.state].label}</Pill>
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      <CustomerDialog open={dialog === "edit"} onOpenChange={(o) => !o && setDialog(null)} customer={customer} />
      <MembershipDialog open={dialog === "start"} onOpenChange={(o) => !o && setDialog(null)} customer={customer} />
      {latest && (
        <MembershipDialog open={dialog === "renew"} onOpenChange={(o) => !o && setDialog(null)} customer={customer} renewFrom={latest} />
      )}
      <PaymentDialog open={dialog === "pay"} onOpenChange={(o) => !o && setDialog(null)} customer={customer} />
    </>
  );
}
