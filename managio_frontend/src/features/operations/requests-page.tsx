"use client";

import { Inbox, Plus } from "lucide-react";
import Link from "next/link";
import { useParams, useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Details } from "@/components/ui/layout";
import { Field, FormError, Input, Select, Textarea } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { LoadingBlock } from "@/components/ui/spinner";
import { useBusiness } from "@/features/business/context";
import { PAYMENT_METHODS, useInvoices, type PaymentMethod } from "@/features/billing/api";
import { CustomerPicker, type PickedCustomer } from "@/features/customers/customer-picker";
import { useStaffSession } from "@/lib/auth/use-session";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatDate, formatDateTime, formatMoney, localToday } from "@/lib/format";
import {
  REQUEST_STATUS,
  REQUEST_TYPE,
  useCreateRequest,
  useDecideRequest,
  useExpenseCategories,
  useRequest,
  useRequests,
  type ApprovalRequest,
  type RequestStatus,
  type RequestType,
} from "./api";

const PAGE_SIZE = 25;

/**
 * Requests and approvals (REQ-1 to REQ-5): anyone can ask; people with requests.approve decide, never on their
 * own requests. An approved expense claim becomes an expense; an approved discount is applied to the invoice.
 */
export function RequestsPage() {
  const business = useBusiness();
  const router = useRouter();
  const params = useSearchParams();
  const route = useParams<{ requestId?: string }>();
  const approver = business.can("requests.approve");
  const initialStatus = (params.get("status") as RequestStatus | null) ?? (approver ? "PENDING" : null);
  const [status, setStatus] = useState<RequestStatus | "ALL">(initialStatus ?? "ALL");
  const [mine, setMine] = useState(!approver);
  const [page, setPage] = useState(0);
  const [creating, setCreating] = useState(false);
  const selectedId = route.requestId ? Number(route.requestId) : null;
  const requested = params.get("new") === "1";
  const requests = useRequests({ status: status === "ALL" ? undefined : status, mine, page, size: PAGE_SIZE });

  const open = (id: number) => router.push(`/app/${business.id}/requests/${id}`);
  const closeDetail = () => router.push(`/app/${business.id}/requests`);

  return (
    <>
      <PageHeader
        title="Requests"
        subtitle={approver ? "Expense claims, purchases, leave and discounts waiting for a decision" : "Ask for an expense claim, a purchase, leave or a discount"}
        actions={<Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setCreating(true)}>New request</Button>}
      />
      <div className="mb-4 flex flex-wrap gap-3">
        {approver && (
          <Segmented label="Whose" value={mine ? "mine" : "all"} onChange={(v) => { setMine(v === "mine"); setPage(0); }} options={[{ value: "all", label: "Everyone's" }, { value: "mine", label: "Mine" }]} />
        )}
        <Segmented
          label="Status"
          value={status}
          onChange={(v) => { setStatus(v); setPage(0); }}
          options={[
            { value: "PENDING", label: "Waiting" },
            { value: "APPROVED", label: "Approved" },
            { value: "REJECTED", label: "Rejected" },
            { value: "ALL", label: "All" },
          ]}
        />
      </div>
      <Panel>
        {requests.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2].map((i) => <Skeleton key={i} className="h-10 w-full" />)}</div>
        ) : requests.isError ? (
          <EmptyState title="Requests couldn't load" body={errorMessage(requests.error)} />
        ) : requests.data.items.length === 0 ? (
          <EmptyState icon={<Inbox className="h-5 w-5" />} title={status === "PENDING" ? "Nothing waiting" : "No requests here"} />
        ) : (
          <>
            <Table>
              <THead>
                <tr>
                  <TH>Request</TH>
                  <TH>Kind</TH>
                  <TH>From</TH>
                  <TH>Asked</TH>
                  <TH className="text-right">Amount</TH>
                  <TH>Status</TH>
                </tr>
              </THead>
              <tbody>
                {requests.data.items.map((r) => (
                  <TR key={r.id} onClick={() => open(r.id)}>
                    <TD className="font-medium">{r.title}</TD>
                    <TD className="text-muted">{REQUEST_TYPE[r.type]}</TD>
                    <TD>{r.requestedBy}</TD>
                    <TD className="tabular-nums text-muted">{formatDateTime(r.createdAt)}</TD>
                    <TD className="text-right tabular-nums">{r.amount != null ? formatMoney(r.amount) : r.startsOn ? `${formatDate(r.startsOn)}${r.endsOn && r.endsOn !== r.startsOn ? ` – ${formatDate(r.endsOn)}` : ""}` : "—"}</TD>
                    <TD><Pill tone={REQUEST_STATUS[r.status].tone}>{REQUEST_STATUS[r.status].label}</Pill></TD>
                  </TR>
                ))}
              </tbody>
            </Table>
            <Pagination page={page} size={PAGE_SIZE} total={requests.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>

      <Dialog open={selectedId != null} onOpenChange={(o) => !o && closeDetail()} title="Request" size="md">
        {selectedId != null && <RequestDetail requestId={selectedId} onDone={closeDetail} />}
      </Dialog>
      <Dialog
        open={creating || requested}
        onOpenChange={(o) => {
          setCreating(o);
          if (!o && requested) router.replace(`/app/${business.id}/requests`);
        }}
        title="New request"
      >
        <RequestForm onDone={() => { setCreating(false); if (requested) router.replace(`/app/${business.id}/requests`); }} />
      </Dialog>
    </>
  );
}

function RequestDetail({ requestId, onDone }: { requestId: number; onDone: () => void }) {
  const business = useBusiness();
  const session = useStaffSession();
  const request = useRequest(requestId);
  const decide = useDecideRequest();
  const [mode, setMode] = useState<null | "approve" | "reject">(null);
  const [note, setNote] = useState("");
  const [method, setMethod] = useState<PaymentMethod>("CASH");
  const [error, setError] = useState<string | null>(null);

  if (request.isPending) return <LoadingBlock />;
  if (request.isError) {
    return (
      <DialogBody>
        <p className="text-[13.5px] text-bad">{errorMessage(request.error)}</p>
      </DialogBody>
    );
  }
  const r: ApprovalRequest = request.data;
  const mineToDecide = r.requestedBy === session.principal?.fullName;
  const canDecide = business.can("requests.approve") && r.status === "PENDING";

  const run = async (decision: "approve" | "reject" | "cancel") => {
    setError(null);
    try {
      if (decision === "approve") await decide.mutateAsync({ requestId: r.id, decision, note: note.trim() || undefined, method: r.type === "EXPENSE" ? method : undefined });
      else if (decision === "reject") await decide.mutateAsync({ requestId: r.id, decision, note: note.trim() });
      else await decide.mutateAsync({ requestId: r.id, decision });
      toast.success(decision === "approve" ? "Approved" : decision === "reject" ? "Rejected" : "Request cancelled");
      onDone();
    } catch (e) {
      setError(e instanceof ApiError && e.is("request.own") ? "You can't decide your own request. Another approver has to." : errorMessage(e));
    }
  };

  return (
    <>
      <DialogBody className="space-y-4 px-0 py-0">
        <div className="px-5 pt-4">
          <FormError message={error} />
          <p className="text-[17px] font-semibold text-ink">{r.title}</p>
          <p className="mt-0.5 flex items-center gap-2 text-[13px] text-muted">
            {REQUEST_TYPE[r.type]} · from {r.requestedBy} <Pill tone={REQUEST_STATUS[r.status].tone}>{REQUEST_STATUS[r.status].label}</Pill>
          </p>
        </div>
        <Details
          items={[
            ...(r.amount != null ? [{ label: r.type === "DISCOUNT" ? "Discount" : "Amount", value: formatMoney(r.amount) }] : []),
            ...(r.startsOn ? [{ label: "Dates", value: `${formatDate(r.startsOn)}${r.endsOn && r.endsOn !== r.startsOn ? ` to ${formatDate(r.endsOn)}` : ""}` }] : []),
            ...(r.invoiceId ? [{ label: "Invoice", value: <Link href={`/app/${business.id}/billing/invoices/${r.invoiceId}`} className="text-cobalt hover:underline">Open the invoice</Link> }] : []),
            ...(r.details ? [{ label: "Details", value: <span className="whitespace-pre-wrap">{r.details}</span> }] : []),
            { label: "Asked", value: formatDateTime(r.createdAt) },
            ...(r.decidedAt ? [{ label: r.status === "CANCELLED" ? "Cancelled" : "Decided", value: `${formatDateTime(r.decidedAt)}${r.decidedBy ? ` by ${r.decidedBy}` : ""}` }] : []),
            ...(r.decisionNote ? [{ label: "Note", value: r.decisionNote }] : []),
            ...(r.expenseId ? [{ label: "Expense", value: <Link href={`/app/${business.id}/expenses`} className="text-cobalt hover:underline">Recorded</Link> }] : []),
          ]}
        />
        {canDecide && !mineToDecide && mode && (
          <div className="space-y-3 border-t border-line px-5 py-4">
            {mode === "approve" && r.type === "EXPENSE" && (
              <Field label="How was it paid?" hint="The expense is recorded today with this method">
                <Select value={method} onChange={(e) => setMethod(e.target.value as PaymentMethod)}>
                  {PAYMENT_METHODS.map((m) => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </Select>
              </Field>
            )}
            <Field label={mode === "reject" ? "Why not?" : "Note"} optional={mode === "approve"}>
              <Textarea rows={2} value={note} onChange={(e) => setNote(e.target.value)} maxLength={500} autoFocus />
            </Field>
          </div>
        )}
      </DialogBody>
      <DialogFooter>
        {r.status === "PENDING" && mineToDecide && (
          <Button variant="danger-outline" loading={decide.isPending} onClick={() => run("cancel")}>Cancel my request</Button>
        )}
        {canDecide && !mineToDecide ? (
          mode == null ? (
            <>
              <Button onClick={() => setMode("reject")}>Reject</Button>
              <Button variant="primary" onClick={() => setMode("approve")}>Approve</Button>
            </>
          ) : (
            <>
              <Button onClick={() => setMode(null)}>Back</Button>
              <Button
                variant={mode === "approve" ? "primary" : "danger"}
                loading={decide.isPending}
                disabled={mode === "reject" && !note.trim()}
                onClick={() => run(mode)}
              >
                {mode === "approve" ? (r.type === "EXPENSE" ? "Approve and record expense" : r.type === "DISCOUNT" ? "Approve and apply discount" : "Approve") : "Reject"}
              </Button>
            </>
          )
        ) : (
          <Button onClick={onDone}>Close</Button>
        )}
      </DialogFooter>
    </>
  );
}

function RequestForm({ onDone }: { onDone: () => void }) {
  const create = useCreateRequest();
  const categories = useExpenseCategories();
  const [type, setType] = useState<RequestType>("EXPENSE");
  const [values, setValues] = useState({ title: "", details: "", amount: "", expenseCategoryId: "", startsOn: localToday(), endsOn: "", invoiceId: "" });
  const [customer, setCustomer] = useState<PickedCustomer | null>(null);
  const invoices = useInvoices({ view: "OPEN", customerId: customer?.id, size: 20 }, type === "DISCOUNT" && customer != null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));
  const needsAmount = type === "EXPENSE" || type === "PURCHASE" || type === "DISCOUNT";

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setFormError(null);
    const local: Record<string, string> = {};
    if (!values.title.trim()) local.title = "Enter a short title";
    if (needsAmount && !/^\d+(\.\d{1,2})?$/.test(values.amount)) local.amount = "Enter an amount like 850";
    if (type === "EXPENSE" && !values.expenseCategoryId) local.expenseCategoryId = "Choose a category";
    if (type === "DISCOUNT" && !values.invoiceId) local.invoiceId = "Choose the invoice";
    if (type === "LEAVE" && !values.startsOn) local.startsOn = "Choose the first day";
    setErrors(local);
    if (Object.keys(local).length) return;
    try {
      await create.mutateAsync({
        type,
        title: values.title.trim(),
        details: values.details.trim() || undefined,
        amount: needsAmount ? Number(values.amount) : undefined,
        expenseCategoryId: type === "EXPENSE" ? Number(values.expenseCategoryId) : undefined,
        startsOn: type === "LEAVE" ? values.startsOn : undefined,
        endsOn: type === "LEAVE" && values.endsOn ? values.endsOn : undefined,
        invoiceId: type === "DISCOUNT" ? Number(values.invoiceId) : undefined,
      });
      toast.success("Request sent. Approvers have been told.");
      onDone();
    } catch (error) {
      if (error instanceof ApiError && Object.keys(error.fieldErrors).length) setErrors(error.fieldErrors);
      else setFormError(errorMessage(error));
    }
  };

  return (
    <form onSubmit={submit} className="flex min-h-0 flex-1 flex-col" noValidate>
      <DialogBody className="space-y-4">
        <FormError message={formError} />
        <Field label="What are you asking for?">
          <Select value={type} onChange={(e) => setType(e.target.value as RequestType)}>
            <option value="EXPENSE">Pay me back for an expense</option>
            <option value="PURCHASE">Permission to buy something</option>
            <option value="LEAVE">Leave</option>
            <option value="DISCOUNT">A discount for a customer</option>
            <option value="OTHER">Something else</option>
          </Select>
        </Field>
        <Field label="Title" error={errors.title}>
          <Input value={values.title} onChange={set("title")} placeholder={type === "LEAVE" ? "Family wedding" : type === "EXPENSE" ? "Cleaning supplies" : ""} />
        </Field>
        {type === "DISCOUNT" && (
          <>
            <Field label="Customer">
              <CustomerPicker value={customer} onChange={(c) => { setCustomer(c); setValues((v) => ({ ...v, invoiceId: "" })); }} />
            </Field>
            {customer && (
              <Field label="Invoice" error={errors.invoiceId} hint={invoices.data?.items.length === 0 ? "They have no open invoices" : "Only invoices with no payments yet can be discounted"}>
                <Select value={values.invoiceId} onChange={set("invoiceId")}>
                  <option value="">Choose…</option>
                  {(invoices.data?.items ?? []).filter((i) => i.paid === 0).map((i) => (
                    <option key={i.id} value={i.id}>{i.invoiceNumber} · {formatMoney(i.total)}</option>
                  ))}
                </Select>
              </Field>
            )}
          </>
        )}
        <div className="grid gap-4 sm:grid-cols-2">
          {needsAmount && (
            <Field label={type === "DISCOUNT" ? "Discount amount" : "Amount"} error={errors.amount}>
              <Input inputMode="decimal" value={values.amount} onChange={set("amount")} />
            </Field>
          )}
          {type === "EXPENSE" && (
            <Field label="Category" error={errors.expenseCategoryId}>
              <Select value={values.expenseCategoryId} onChange={set("expenseCategoryId")}>
                <option value="">Choose…</option>
                {(categories.data ?? []).filter((c) => !c.archived).map((c) => (
                  <option key={c.id} value={c.id}>{c.name}</option>
                ))}
              </Select>
            </Field>
          )}
          {type === "LEAVE" && (
            <>
              <Field label="First day" error={errors.startsOn}>
                <Input type="date" value={values.startsOn} onChange={set("startsOn")} />
              </Field>
              <Field label="Last day" optional hint="Leave empty for one day" error={errors.endsOn}>
                <Input type="date" min={values.startsOn} value={values.endsOn} onChange={set("endsOn")} />
              </Field>
            </>
          )}
        </div>
        <Field label="Details" optional>
          <Textarea rows={3} value={values.details} onChange={set("details")} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={create.isPending}>Send request</Button>
      </DialogFooter>
    </form>
  );
}
