"use client";

import { Ellipsis, Plus, Search, Wallet } from "lucide-react";
import { useRouter, useSearchParams } from "next/navigation";
import { useState, type FormEvent } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { Checkbox, Field, FormError, Input, Select } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Menu, MenuItem } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { ReasonDialog } from "@/components/ui/reason-dialog";
import { useBusiness } from "@/features/business/context";
import { PAYMENT_METHODS, methodLabel, type PaymentMethod } from "@/features/billing/api";
import type { Schemas } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { formatDate, formatMoney, localToday } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";
import { useExpenseCategories, useExpenseSummary, useExpenses, useRecordExpense, useSaveCategory, useVoidExpense } from "./api";

const PAGE_SIZE = 25;

/** Money going out, by category (EXP-1 to EXP-4). Wrong entries are voided with a reason, never deleted. */
export function ExpensesPage() {
  const business = useBusiness();
  const params = useSearchParams();
  const router = useRouter();
  const today = localToday();
  const [from, setFrom] = useState(`${today.slice(0, 8)}01`);
  const [to, setTo] = useState(today);
  const [categoryId, setCategoryId] = useState("");
  const [text, setText] = useState("");
  const [includeVoid, setIncludeVoid] = useState(false);
  const [page, setPage] = useState(0);
  const [recording, setRecording] = useState(false);
  const [managing, setManaging] = useState(false);
  const [voiding, setVoiding] = useState<Schemas["ExpenseResponse"] | null>(null);
  const q = useDebounced(text.trim(), 250);
  const requested = params.get("new") === "1";

  const categories = useExpenseCategories();
  const summary = useExpenseSummary(from, to);
  const expenses = useExpenses({ from, to, categoryId: categoryId ? Number(categoryId) : undefined, q: q || undefined, includeVoid, page, size: PAGE_SIZE });
  const voidExpense = useVoidExpense();
  const canRecord = business.can("expenses.record");

  return (
    <>
      <PageHeader
        title="Expenses"
        subtitle={summary.data ? `${formatMoney(summary.data.total)} spent from ${formatDate(from)} to ${formatDate(to)}` : undefined}
        actions={
          <>
            {business.can("settings.manage") && <Button onClick={() => setManaging(true)}>Categories</Button>}
            {canRecord && <Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setRecording(true)}>Record expense</Button>}
          </>
        }
      />

      {summary.data && summary.data.byCategory.length > 0 && (
        <Panel className="mb-5" bodyClassName="px-5 py-4">
          <div className="flex h-2.5 overflow-hidden rounded-full bg-line" aria-hidden>
            {summary.data.byCategory.map((c, i) => (
              <span key={c.categoryId} style={{ width: `${(c.total / summary.data.total) * 100}%`, opacity: 1 - i * 0.12 }} className="bg-cobalt" />
            ))}
          </div>
          <ul className="mt-3 flex flex-wrap gap-x-6 gap-y-1.5 text-[12.5px] text-muted">
            {summary.data.byCategory.map((c) => (
              <li key={c.categoryId}>
                {c.category} <span className="font-medium tabular-nums text-ink">{formatMoney(c.total)}</span>
              </li>
            ))}
          </ul>
        </Panel>
      )}

      <Panel>
        <div className="flex flex-wrap items-end gap-3 border-b border-line px-5 py-3">
          <label className="text-[12.5px] text-muted">
            From
            <Input type="date" value={from} max={to} onChange={(e) => { setFrom(e.target.value); setPage(0); }} className="mt-1 w-[160px]" />
          </label>
          <label className="text-[12.5px] text-muted">
            To
            <Input type="date" value={to} min={from} onChange={(e) => { setTo(e.target.value); setPage(0); }} className="mt-1 w-[160px]" />
          </label>
          <label className="text-[12.5px] text-muted">
            Category
            <Select value={categoryId} onChange={(e) => { setCategoryId(e.target.value); setPage(0); }} className="mt-1 w-[180px]">
              <option value="">All categories</option>
              {(categories.data ?? []).map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </Select>
          </label>
          <div className="relative min-w-[200px] flex-1">
            <Search className="pointer-events-none absolute bottom-2.5 left-3 h-4 w-4 text-faint" aria-hidden />
            <Input value={text} onChange={(e) => { setText(e.target.value); setPage(0); }} placeholder="Payee or description" aria-label="Search expenses" className="pl-9" />
          </div>
          <Checkbox label="Show void" checked={includeVoid} onChange={(e) => setIncludeVoid(e.target.checked)} className="pb-2" />
        </div>
        {expenses.isPending ? (
          <div className="space-y-2 p-5">{[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
        ) : expenses.isError ? (
          <EmptyState title="Expenses couldn't load" body={errorMessage(expenses.error)} />
        ) : expenses.data.items.length === 0 ? (
          <EmptyState icon={<Wallet className="h-5 w-5" />} title="No expenses in this period" action={canRecord && <Button variant="primary" onClick={() => setRecording(true)}>Record expense</Button>} />
        ) : (
          <>
            <Table>
              <THead>
                <tr>
                  <TH>Date</TH>
                  <TH>Category</TH>
                  <TH>Paid to</TH>
                  <TH>Method</TH>
                  <TH>Recorded by</TH>
                  <TH className="text-right">Amount</TH>
                  <TH><span className="sr-only">Actions</span></TH>
                </tr>
              </THead>
              <tbody>
                {expenses.data.items.map((e) => (
                  <TR key={e.id} className={e.voided ? "text-faint" : undefined}>
                    <TD className="tabular-nums">{formatDate(e.spentOn)}</TD>
                    <TD>
                      {e.category.name}
                      {e.voided && <Pill className="ml-2">Void</Pill>}
                      {e.payrollEntryId && <Pill tone="info" className="ml-2">Salary</Pill>}
                      {e.requestId && <Pill tone="info" className="ml-2">Approved request</Pill>}
                    </TD>
                    <TD>
                      {e.payee ?? <span className="text-faint">—</span>}
                      {e.description && <span className="block text-[12px] text-muted">{e.description}</span>}
                      {e.voided && e.voidReason && <span className="block text-[12px]">Voided: {e.voidReason}</span>}
                    </TD>
                    <TD className="text-muted">{methodLabel(e.method)}{e.reference ? ` · ${e.reference}` : ""}</TD>
                    <TD className="text-muted">{e.recordedBy ?? "—"}</TD>
                    <TD className={e.voided ? "text-right tabular-nums line-through" : "text-right font-medium tabular-nums"}>{formatMoney(e.amount)}</TD>
                    <TD className="w-10 text-right">
                      {canRecord && !e.voided && (
                        <Menu trigger={<Button size="icon" variant="ghost" aria-label="Expense actions"><Ellipsis className="h-4 w-4" /></Button>}>
                          <MenuItem danger onSelect={() => setVoiding(e)}>Void</MenuItem>
                        </Menu>
                      )}
                    </TD>
                  </TR>
                ))}
              </tbody>
            </Table>
            <Pagination page={page} size={PAGE_SIZE} total={expenses.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>

      <Dialog
        open={recording || requested}
        onOpenChange={(open) => {
          setRecording(open);
          if (!open && requested) router.replace(`/app/${business.id}/expenses`);
        }}
        title="Record an expense"
      >
        <ExpenseForm categories={(categories.data ?? []).filter((c) => !c.archived)} onDone={() => { setRecording(false); if (requested) router.replace(`/app/${business.id}/expenses`); }} />
      </Dialog>
      <Dialog open={managing} onOpenChange={setManaging} title="Expense categories" description="Archived categories can't take new expenses. Salaries is needed for payroll and stays." size="sm">
        <CategoriesForm categories={categories.data ?? []} onDone={() => setManaging(false)} />
      </Dialog>
      <ReasonDialog
        open={voiding != null}
        onOpenChange={(o) => !o && setVoiding(null)}
        title="Void this expense?"
        description={voiding ? `${formatMoney(voiding.amount)} for ${voiding.category.name} on ${formatDate(voiding.spentOn)}. It stays on record, marked void.` : undefined}
        confirmLabel="Void expense"
        onConfirm={async (reason) => {
          if (!voiding) return;
          await voidExpense.mutateAsync({ expenseId: voiding.id, reason });
          toast.success("Expense voided");
        }}
      />
    </>
  );
}

function ExpenseForm({ categories, onDone }: { categories: Schemas["CategoryResponse"][]; onDone: () => void }) {
  const record = useRecordExpense();
  const [values, setValues] = useState({ categoryId: "", amount: "", spentOn: localToday(), method: "CASH" as PaymentMethod, payee: "", description: "", reference: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState<string | null>(null);
  const set = (key: keyof typeof values) => (e: { target: { value: string } }) => setValues((v) => ({ ...v, [key]: e.target.value }));

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    const local: Record<string, string> = {};
    if (!values.categoryId) local.categoryId = "Choose a category";
    if (!/^\d+(\.\d{1,2})?$/.test(values.amount) || Number(values.amount) <= 0) local.amount = "Enter an amount like 850";
    setErrors(local);
    setFormError(null);
    if (Object.keys(local).length) return;
    try {
      const expense = await record.mutateAsync({
        categoryId: Number(values.categoryId),
        amount: Number(values.amount),
        spentOn: values.spentOn || undefined,
        method: values.method,
        payee: values.payee.trim() || undefined,
        description: values.description.trim() || undefined,
        reference: values.reference.trim() || undefined,
      });
      toast.success(`Recorded ${formatMoney(expense.amount)} for ${expense.category.name}`);
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
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Category" error={errors.categoryId}>
            <Select value={values.categoryId} onChange={set("categoryId")}>
              <option value="">Choose…</option>
              {categories.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </Select>
          </Field>
          <Field label="Amount" error={errors.amount}>
            <Input autoFocus inputMode="decimal" value={values.amount} onChange={set("amount")} />
          </Field>
          <Field label="Spent on" error={errors.spentOn}>
            <Input type="date" max={localToday()} value={values.spentOn} onChange={set("spentOn")} />
          </Field>
          <Field label="Paid by">
            <Select value={values.method} onChange={set("method")}>
              {PAYMENT_METHODS.map((m) => (
                <option key={m.value} value={m.value}>{m.label}</option>
              ))}
            </Select>
          </Field>
          <Field label="Paid to" optional>
            <Input value={values.payee} onChange={set("payee")} placeholder="Shop, person or company" />
          </Field>
          <Field label="Reference" optional>
            <Input value={values.reference} onChange={set("reference")} placeholder="Bill or UPI ref" />
          </Field>
        </div>
        <Field label="What for" optional>
          <Input value={values.description} onChange={set("description")} />
        </Field>
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button type="submit" variant="primary" loading={record.isPending}>Record expense</Button>
      </DialogFooter>
    </form>
  );
}

function CategoriesForm({ categories, onDone }: { categories: Schemas["CategoryResponse"][]; onDone: () => void }) {
  const add = useSaveCategory();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!name.trim()) return;
    setError(null);
    try {
      await add.mutateAsync({ name: name.trim() });
      setName("");
    } catch (e) {
      setError(errorMessage(e));
    }
  };

  return (
    <>
      <DialogBody className="space-y-4">
        <FormError message={error} />
        <ul className="divide-y divide-line rounded-[var(--radius-control)] border border-line">
          {categories.map((c) => (
            <CategoryRow key={c.id} category={c} onError={setError} />
          ))}
        </ul>
        <form onSubmit={submit} className="flex gap-2">
          <Input value={name} onChange={(e) => setName(e.target.value)} placeholder="New category" aria-label="New category name" />
          <Button type="submit" loading={add.isPending}>Add</Button>
        </form>
      </DialogBody>
      <DialogFooter>
        <Button variant="primary" onClick={onDone}>Done</Button>
      </DialogFooter>
    </>
  );
}

function CategoryRow({ category, onError }: { category: Schemas["CategoryResponse"]; onError: (message: string | null) => void }) {
  const save = useSaveCategory(category.id);
  const toggle = async () => {
    onError(null);
    try {
      await save.mutateAsync({ name: category.name, archived: !category.archived });
    } catch (e) {
      onError(errorMessage(e));
    }
  };
  return (
    <li className="flex items-center justify-between gap-3 px-3 py-2 text-[13.5px]">
      <span className={category.archived ? "text-faint" : "text-ink"}>
        {category.name}
        {category.archived && <Pill className="ml-2">Archived</Pill>}
      </span>
      {category.systemKey !== "SALARIES" && (
        <Button size="sm" variant="ghost" loading={save.isPending} onClick={toggle}>{category.archived ? "Restore" : "Archive"}</Button>
      )}
    </li>
  );
}
