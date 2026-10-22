"use client";

import { Ellipsis, Plus, ReceiptIndianRupee, Search, X } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input, Select } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem, Segmented, Tabs } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useBusiness } from "@/features/business/context";
import { useCustomer } from "@/features/customers/api";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { addDays, formatDate, formatDateTime, formatMoney, localToday } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";
import { INVOICE_STATE, PAYMENT_METHODS, methodLabel, useInvoices, usePayments, useVoidPayment, type InvoiceView, type PaymentMethod } from "./api";
import { RefundDialog } from "./refund-dialog";
import { SaleDialog } from "./sale-dialog";

const PAGE_SIZE = 25;

/** Billing: invoices (BIL-1, BIL-2) and the payments taken against them (BIL-4, BIL-5). */
export function BillingPage() {
  const business = useBusiness();
  const params = useSearchParams();
  const router = useRouter();
  const [tab, setTab] = useState(params.get("tab") === "payments" ? "payments" : "invoices");
  const [selling, setSelling] = useState(false);
  const sellRequested = params.get("new") === "sale";
  const customerParam = params.get("customer");
  const customerId = customerParam ? Number(customerParam) : undefined;
  const customer = useCustomer(customerId ?? null);

  return (
    <>
      <PageHeader
        title="Billing"
        subtitle="Invoices and the money taken against them"
        actions={
          business.can("billing.collect") && (
            <Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setSelling(true)}>
              New sale or invoice
            </Button>
          )
        }
      />
      {customerId && (
        <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-line bg-surface py-1 pl-3 pr-1 text-[13px]">
          Only {customer.data?.fullName ?? "this customer"}
          <button type="button" aria-label="Show everyone" onClick={() => router.replace(`/app/${business.id}/billing`)} className="rounded-full p-1 text-faint hover:bg-canvas hover:text-ink">
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}
      <Tabs value={tab} onValueChange={setTab} items={[{ value: "invoices", label: "Invoices" }, { value: "payments", label: "Payments" }]} />
      <div className="mt-4">{tab === "invoices" ? <Invoices customerId={customerId} /> : <Payments customerId={customerId} />}</div>
      <SaleDialog
        open={selling || sellRequested}
        onOpenChange={(open) => {
          setSelling(open);
          if (!open && sellRequested) router.replace(`/app/${business.id}/billing`);
        }}
        customer={customer.data ? { id: customer.data.id, name: customer.data.fullName, number: customer.data.number } : undefined}
      />
    </>
  );
}

function Invoices({ customerId }: { customerId?: number }) {
  const business = useBusiness();
  const router = useRouter();
  const [view, setView] = useState<InvoiceView>("OPEN");
  const [text, setText] = useState("");
  const [page, setPage] = useState(0);
  const q = useDebounced(text.trim(), 250);
  const invoices = useInvoices({ view, customerId, q: q || undefined, page, size: PAGE_SIZE });

  return (
    <Panel>
      <div className="flex flex-wrap items-center gap-3 border-b border-line px-5 py-3">
        <Segmented
          label="Show"
          value={view}
          onChange={(v) => {
            setView(v);
            setPage(0);
          }}
          options={[
            { value: "OPEN", label: "Open" },
            { value: "OVERDUE", label: "Overdue" },
            { value: "PAID", label: "Paid" },
            { value: "VOID", label: "Void" },
            { value: "ALL", label: "All" },
          ]}
        />
        <div className="relative ml-auto w-full max-w-[280px]">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" aria-hidden />
          <Input value={text} onChange={(e) => setText(e.target.value)} placeholder="Invoice number or name" aria-label="Find an invoice" className="pl-9" />
        </div>
      </div>
      {invoices.isPending ? (
        <div className="space-y-2 p-5">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
      ) : invoices.isError ? (
        <EmptyState title="Invoices couldn't load" body={errorMessage(invoices.error)} />
      ) : invoices.data.items.length === 0 ? (
        <EmptyState icon={<ReceiptIndianRupee className="h-5 w-5" />} title={view === "OPEN" ? "Nothing waiting to be paid" : "No invoices here"} />
      ) : (
        <>
          <Table>
            <THead>
              <tr>
                <TH>Invoice</TH>
                <TH>{business.labels.customer}</TH>
                <TH>Issued</TH>
                <TH>Due</TH>
                <TH className="text-right">Total</TH>
                <TH className="text-right">Still owed</TH>
                <TH>Status</TH>
              </tr>
            </THead>
            <tbody>
              {invoices.data.items.map((invoice) => (
                <TR key={invoice.id} onClick={() => router.push(`/app/${business.id}/billing/invoices/${invoice.id}`)}>
                  <TD className="font-medium tabular-nums">
                    <Link href={`/app/${business.id}/billing/invoices/${invoice.id}`} className="hover:text-cobalt">{invoice.invoiceNumber}</Link>
                  </TD>
                  <TD>{invoice.customer?.name ?? <span className="text-faint">Walk-in</span>}</TD>
                  <TD className="tabular-nums text-muted">{formatDate(invoice.issuedOn)}</TD>
                  <TD className="tabular-nums text-muted">{formatDate(invoice.dueOn)}</TD>
                  <TD className="text-right tabular-nums">{formatMoney(invoice.total)}</TD>
                  <TD className="text-right tabular-nums">{invoice.balance > 0 ? <span className="font-medium">{formatMoney(invoice.balance)}</span> : <span className="text-faint">—</span>}</TD>
                  <TD><Pill tone={INVOICE_STATE[invoice.state].tone}>{INVOICE_STATE[invoice.state].label}</Pill></TD>
                </TR>
              ))}
            </tbody>
          </Table>
          <Pagination page={page} size={PAGE_SIZE} total={invoices.data.totalItems} onPage={setPage} />
        </>
      )}
    </Panel>
  );
}

function Payments({ customerId }: { customerId?: number }) {
  const business = useBusiness();
  const today = localToday();
  const [from, setFrom] = useState(addDays(today, -29));
  const [to, setTo] = useState(today);
  const [method, setMethod] = useState<PaymentMethod | "">("");
  const [page, setPage] = useState(0);
  const [voiding, setVoiding] = useState<Schemas["PaymentResponse"] | null>(null);
  const [refunding, setRefunding] = useState<Schemas["PaymentResponse"] | null>(null);
  const payments = usePayments({ from: customerId ? undefined : from, to: customerId ? undefined : to, method: method || undefined, customerId, page, size: PAGE_SIZE });
  const voidPayment = useVoidPayment();
  const canVoid = business.can("billing.void");
  const net = (payments.data?.items ?? []).filter((p) => !p.voided).reduce((s, p) => s + (p.kind === "REFUND" ? -p.amount : p.amount), 0);

  return (
    <Panel>
      <div className="flex flex-wrap items-end gap-3 border-b border-line px-5 py-3">
        {!customerId && (
          <>
            <label className="text-[12.5px] text-muted">
              From
              <Input type="date" value={from} max={to} onChange={(e) => { setFrom(e.target.value); setPage(0); }} className="mt-1 w-[160px]" />
            </label>
            <label className="text-[12.5px] text-muted">
              To
              <Input type="date" value={to} min={from} onChange={(e) => { setTo(e.target.value); setPage(0); }} className="mt-1 w-[160px]" />
            </label>
          </>
        )}
        <label className="text-[12.5px] text-muted">
          Method
          <Select value={method} onChange={(e) => { setMethod(e.target.value as PaymentMethod | ""); setPage(0); }} className="mt-1 w-[160px]">
            <option value="">All methods</option>
            {PAYMENT_METHODS.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </Select>
        </label>
        <p className="ml-auto text-[13px] text-muted">
          On this page: <span className="font-semibold tabular-nums text-ink">{formatMoney(net)}</span>
        </p>
      </div>
      {payments.isPending ? (
        <div className="space-y-2 p-5">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
      ) : payments.isError ? (
        <EmptyState title="Payments couldn't load" body={errorMessage(payments.error)} />
      ) : payments.data.items.length === 0 ? (
        <EmptyState icon={<ReceiptIndianRupee className="h-5 w-5" />} title="No payments in this period" />
      ) : (
        <>
          <Table>
            <THead>
              <tr>
                <TH>Receipt</TH>
                <TH>When</TH>
                <TH>{business.labels.customer}</TH>
                <TH>Invoice</TH>
                <TH>Method</TH>
                <TH className="text-right">Amount</TH>
                <TH><span className="sr-only">Actions</span></TH>
              </tr>
            </THead>
            <tbody>
              {payments.data.items.map((p) => (
                <TR key={p.id} className={p.voided ? "text-faint" : undefined}>
                  <TD className="tabular-nums">
                    {p.receiptNumber}
                    {p.kind === "REFUND" && <Pill tone="due" className="ml-2">Refund</Pill>}
                    {p.voided && <Pill className="ml-2">Void</Pill>}
                  </TD>
                  <TD className="tabular-nums text-muted">{formatDateTime(p.receivedAt)}</TD>
                  <TD>{p.customer?.name ?? "Walk-in"}</TD>
                  <TD className="tabular-nums">
                    <Link href={`/app/${business.id}/billing/invoices/${p.invoiceId}`} className="text-cobalt hover:underline">{p.invoiceNumber}</Link>
                  </TD>
                  <TD className="text-muted">{methodLabel(p.method)}{p.reference ? ` · ${p.reference}` : ""}</TD>
                  <TD className={p.voided ? "text-right tabular-nums line-through" : "text-right font-medium tabular-nums"}>
                    {p.kind === "REFUND" ? "−" : ""}{formatMoney(p.amount)}
                  </TD>
                  <TD className="w-10 text-right">
                    {canVoid && !p.voided && (
                      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${p.receiptNumber}`}><Ellipsis className="h-4 w-4" /></Button>}>
                        {p.kind === "PAYMENT" && <MenuItem onSelect={() => setRefunding(p)}>Refund</MenuItem>}
                        <MenuItem danger onSelect={() => setVoiding(p)}>Void (entered by mistake)</MenuItem>
                      </Menu>
                    )}
                  </TD>
                </TR>
              ))}
            </tbody>
          </Table>
          <Pagination page={page} size={PAGE_SIZE} total={payments.data.totalItems} onPage={setPage} />
        </>
      )}
      <ReasonDialog
        open={voiding != null}
        onOpenChange={(o) => !o && setVoiding(null)}
        title={voiding ? `Void ${voiding.receiptNumber}?` : "Void"}
        description="Use this when a payment was entered by mistake. It stays on record, marked void, and stops counting. To give money back, refund instead."
        confirmLabel="Void payment"
        onConfirm={async (reason) => {
          if (!voiding) return;
          await voidPayment.mutateAsync({ paymentId: voiding.id, reason });
          toast.success(`${voiding.receiptNumber} voided`);
        }}
      />
      <RefundDialog payment={refunding} onClose={() => setRefunding(null)} />
    </Panel>
  );
}
