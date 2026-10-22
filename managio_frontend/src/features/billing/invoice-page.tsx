"use client";

import { ArrowLeft, Ellipsis, Printer } from "lucide-react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Details, EmptyState, Panel, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem } from "@/components/ui/menu";
import { Pill } from "@/components/ui/pill";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { LoadingBlock } from "@/components/ui/spinner";
import { useBusiness } from "@/features/business/context";
import { useCustomerPanel } from "@/features/customers/panel-context";
import type { Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatDateTime, formatMoney, formatNumber } from "@/lib/format";
import { INVOICE_STATE, methodLabel, useInvoice, useVoidInvoice, useVoidPayment } from "./api";
import { PaymentDialog } from "./payment-dialog";
import { RefundDialog } from "./refund-dialog";

/** One invoice: its lines, totals (BIL-1), the payments against it and the actions that are allowed now. */
export function InvoicePage() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const params = useParams<{ invoiceId: string }>();
  const invoice = useInvoice(Number(params.invoiceId));
  const voidInvoice = useVoidInvoice();
  const voidPayment = useVoidPayment();
  const [dialog, setDialog] = useState<null | "pay" | "void">(null);
  const [voidingPayment, setVoidingPayment] = useState<Schemas["PaymentResponse"] | null>(null);
  const [refunding, setRefunding] = useState<Schemas["PaymentResponse"] | null>(null);

  if (invoice.isPending) return <LoadingBlock />;
  if (invoice.isError) {
    return <EmptyState title="This invoice couldn't load" body={errorMessage(invoice.error)} action={<Button asChild><Link href={`/app/${business.id}/billing`}>Back to billing</Link></Button>} />;
  }
  const inv = invoice.data;
  const state = INVOICE_STATE[inv.state];
  const isVoid = inv.state === "VOID";
  const hasMoney = inv.paid !== 0;
  const customer = inv.customer;

  return (
    <>
      <Link href={`/app/${business.id}/billing`} className="mb-4 inline-flex items-center gap-1.5 text-[13px] font-medium text-muted hover:text-ink print:hidden">
        <ArrowLeft className="h-4 w-4" aria-hidden /> Billing
      </Link>
      <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="flex items-center gap-3 text-[22px] font-semibold tabular-nums">
            {inv.invoiceNumber} <Pill tone={state.tone}>{state.label}</Pill>
          </h1>
          <p className="mt-0.5 text-[13px] text-muted">
            {customer ? (
              <button type="button" className="font-medium text-cobalt hover:underline" onClick={() => panel.open(customer.id)}>
                {customer.name}
              </button>
            ) : (
              "Walk-in sale"
            )}{" "}
            · issued {formatDate(inv.issuedOn)} · due {formatDate(inv.dueOn)}
          </p>
        </div>
        <div className="flex gap-2 print:hidden">
          <Button icon={<Printer className="h-4 w-4" />} onClick={() => window.print()}>Print</Button>
          {!isVoid && inv.balance > 0 && inv.customer && business.can("billing.collect") && (
            <Button variant="primary" onClick={() => setDialog("pay")}>Record payment · {formatMoney(inv.balance)}</Button>
          )}
          {!isVoid && business.can("billing.void") && (
            <Menu trigger={<Button size="icon" aria-label="More actions"><Ellipsis className="h-4 w-4" /></Button>}>
              <MenuItem danger disabled={hasMoney} onSelect={() => setDialog("void")}>
                {hasMoney ? "Void (refund or void its payments first)" : "Void invoice"}
              </MenuItem>
            </Menu>
          )}
        </div>
      </header>

      {isVoid && inv.voidReason && (
        <p className="mb-5 rounded-[var(--radius-control)] border border-line bg-surface px-4 py-3 text-[13.5px] text-muted">
          Voided: {inv.voidReason}
        </p>
      )}

      <div className="grid gap-5 xl:grid-cols-[1.6fr_1fr]">
        <Panel title="Lines">
          <Table>
            <THead>
              <tr>
                <TH>Description</TH>
                <TH className="text-right">Qty</TH>
                <TH className="text-right">Price</TH>
                <TH className="text-right">Tax</TH>
                <TH className="text-right">Amount</TH>
              </tr>
            </THead>
            <tbody>
              {inv.lines.map((line, i) => (
                <TR key={i}>
                  <TD>{line.description}</TD>
                  <TD className="text-right tabular-nums">{formatNumber(line.quantity)}</TD>
                  <TD className="text-right tabular-nums">{formatMoney(line.unitPrice)}</TD>
                  <TD className="text-right tabular-nums text-muted">{line.taxRate ? `${line.taxRate}%` : "—"}</TD>
                  <TD className="text-right tabular-nums">{formatMoney(line.amount)}</TD>
                </TR>
              ))}
            </tbody>
          </Table>
          <dl className="ml-auto max-w-[320px] space-y-1.5 border-t border-line px-5 py-4 text-[13.5px]">
            <Row label="Subtotal" value={formatMoney(inv.subtotal)} />
            {inv.discount > 0 && <Row label="Discount" value={`−${formatMoney(inv.discount)}`} />}
            {inv.tax > 0 && <Row label="Tax" value={formatMoney(inv.tax)} />}
            <Row label="Total" value={formatMoney(inv.total)} strong />
            <Row label="Paid" value={formatMoney(inv.paid)} />
            {!isVoid && <Row label="Still owed" value={formatMoney(inv.balance)} strong={inv.balance > 0} />}
          </dl>
          {inv.notes && <p className="border-t border-line px-5 py-3 text-[13px] text-muted">{inv.notes}</p>}
        </Panel>

        <div className="space-y-5">
          <Panel title="Payments">
            {inv.payments.length === 0 ? (
              <p className="px-5 py-6 text-[13px] text-muted">No payments yet.</p>
            ) : (
              <ul className="divide-y divide-line">
                {inv.payments.map((p) => (
                  <li key={p.id} className="flex items-center gap-3 px-5 py-3 text-[13.5px]">
                    <span className="min-w-0 flex-1">
                      <span className={p.voided ? "block font-medium text-faint line-through" : "block font-medium"}>
                        {p.kind === "REFUND" ? "Refund " : ""}
                        {p.receiptNumber} · {methodLabel(p.method)}
                      </span>
                      <span className="text-[12px] text-muted">
                        {formatDateTime(p.receivedAt)}
                        {p.receivedBy ? ` · ${p.receivedBy}` : ""}
                        {p.voided && p.voidReason ? ` · voided: ${p.voidReason}` : ""}
                      </span>
                    </span>
                    <span className={p.voided ? "tabular-nums text-faint line-through" : "font-medium tabular-nums"}>
                      {p.kind === "REFUND" ? "−" : ""}{formatMoney(p.amount)}
                    </span>
                    {business.can("billing.void") && !p.voided && (
                      <Menu trigger={<Button size="icon" variant="ghost" aria-label={`Actions for ${p.receiptNumber}`}><Ellipsis className="h-4 w-4" /></Button>}>
                        {p.kind === "PAYMENT" && <MenuItem onSelect={() => setRefunding(p)}>Refund</MenuItem>}
                        <MenuItem danger onSelect={() => setVoidingPayment(p)}>Void (entered by mistake)</MenuItem>
                      </Menu>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </Panel>
          <Panel title="Details">
            <Details
              items={[
                { label: "Invoice", value: inv.invoiceNumber },
                { label: business.labels.customer, value: inv.customer ? `${inv.customer.name} (#${inv.customer.number})` : "Walk-in" },
                { label: "Issued", value: formatDate(inv.issuedOn) },
                { label: "Due", value: formatDate(inv.dueOn) },
                { label: "For", value: inv.membershipId ? `A ${business.labels.plan.toLowerCase()}` : "A sale" },
              ]}
            />
          </Panel>
        </div>
      </div>

      {inv.customer && (
        <PaymentDialog
          open={dialog === "pay"}
          onOpenChange={(o) => !o && setDialog(null)}
          customer={{ id: inv.customer.id, fullName: inv.customer.name }}
          invoiceId={inv.id}
        />
      )}
      <ReasonDialog
        open={dialog === "void"}
        onOpenChange={(o) => !o && setDialog(null)}
        title={`Void ${inv.invoiceNumber}?`}
        description="It stays on record, marked void, and is no longer owed."
        confirmLabel="Void invoice"
        onConfirm={async (reason) => {
          await voidInvoice.mutateAsync({ invoiceId: inv.id, reason });
          toast.success(`${inv.invoiceNumber} voided`);
        }}
      />
      <ReasonDialog
        open={voidingPayment != null}
        onOpenChange={(o) => !o && setVoidingPayment(null)}
        title={voidingPayment ? `Void ${voidingPayment.receiptNumber}?` : "Void"}
        description="Use this when a payment was entered by mistake. To give money back, refund instead."
        confirmLabel="Void payment"
        onConfirm={async (reason) => {
          if (!voidingPayment) return;
          await voidPayment.mutateAsync({ paymentId: voidingPayment.id, reason });
          toast.success(`${voidingPayment.receiptNumber} voided`);
        }}
      />
      <RefundDialog payment={refunding} onClose={() => setRefunding(null)} />
    </>
  );
}

function Row({ label, value, strong }: { label: string; value: string; strong?: boolean }) {
  return (
    <div className="flex justify-between gap-6">
      <dt className="text-muted">{label}</dt>
      <dd className={strong ? "font-semibold tabular-nums text-ink" : "tabular-nums text-ink"}>{value}</dd>
    </div>
  );
}
