"use client";

import { Download, Plus, Upload, Users } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useState } from "react";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { SearchInput, Select } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { useBusiness } from "@/features/business/context";
import { staff } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatMoney } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";
import { MEMBERSHIP_STATE, useCustomerCounts, useCustomers, type CustomerListParams, type CustomerView } from "./api";
import { CustomerDialog } from "./customer-dialog";
import { ImportDialog } from "./import-dialog";
import { useCustomerPanel } from "./panel-context";

const PAGE_SIZE = 25;
const VIEWS: CustomerView[] = ["ALL", "ACTIVE", "ENDING_SOON", "LAPSED", "NO_PLAN", "WITH_DUES", "ARCHIVED"];

/** The customer list (CUS-1, CUS-2): table first, filtered by standing, every row opens the panel. */
export function CustomersPage() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const params = useSearchParams();
  const initialView = VIEWS.find((v) => v === params.get("view")) ?? "ALL";
  const [view, setView] = useState<CustomerView>(initialView);
  const [text, setText] = useState("");
  const [sort, setSort] = useState<NonNullable<CustomerListParams["sort"]>>("NAME");
  const [page, setPage] = useState(0);
  const [adding, setAdding] = useState(false);
  const [importing, setImporting] = useState(false);
  const q = useDebounced(text.trim(), 250);

  const counts = useCustomerCounts();
  const list = useCustomers({ view, q: q || undefined, sort, page, size: PAGE_SIZE });
  const labels = business.labels;
  const memberships = business.has("MEMBERSHIPS");

  const viewLabels: Record<CustomerView, string> = {
    ALL: "All",
    ACTIVE: "Active",
    ENDING_SOON: "Ending soon",
    LAPSED: "Lapsed",
    NO_PLAN: "No plan",
    WITH_DUES: "Owe money",
    ARCHIVED: "Archived",
  };
  const countFor: Record<CustomerView, number | undefined> = {
    ALL: counts.data?.all,
    ACTIVE: counts.data?.active,
    ENDING_SOON: counts.data?.endingSoon,
    LAPSED: counts.data?.lapsed,
    NO_PLAN: counts.data?.noPlan,
    WITH_DUES: counts.data?.withDues,
    ARCHIVED: counts.data?.archived,
  };
  const views = VIEWS.filter((v) => memberships || !["ACTIVE", "ENDING_SOON", "LAPSED", "NO_PLAN"].includes(v));

  const exportCsv = async () => {
    try {
      const response = await fetch(`/api/v1/businesses/${business.id}/customers/export`, {
        headers: { Authorization: `Bearer ${staff.store.getState().token}` },
      });
      if (!response.ok) throw new Error("The export didn't work. Try again.");
      const url = URL.createObjectURL(await response.blob());
      const link = document.createElement("a");
      link.href = url;
      link.download = `${business.labels.customers.toLowerCase()}-${business.code}.csv`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(errorMessage(error));
    }
  };

  return (
    <>
      <PageHeader
        title={labels.customers}
        subtitle={counts.data ? `${counts.data.all - counts.data.archived} on the books` : undefined}
        actions={
          <>
            {business.can("customers.import") && (
              <>
                <Button icon={<Upload className="h-4 w-4" />} onClick={() => setImporting(true)}>Import</Button>
                <Button icon={<Download className="h-4 w-4" />} onClick={exportCsv}>Export</Button>
              </>
            )}
            {business.can("customers.manage") && (
              <Button variant="primary" icon={<Plus className="h-4 w-4" />} onClick={() => setAdding(true)}>
                Add {labels.customer.toLowerCase()}
              </Button>
            )}
          </>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Segmented
          label="Show"
          value={view}
          onChange={(v) => {
            setView(v);
            setPage(0);
          }}
          options={views.map((v) => ({ value: v, label: viewLabels[v], count: countFor[v] }))}
        />
      </div>

      <Panel>
        <div className="flex flex-wrap items-center gap-3 border-b border-line px-5 py-3">
          <SearchInput
            className="min-w-[220px] flex-1"
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setPage(0);
            }}
            placeholder="Filter by name, phone or #number"
            aria-label={`Filter ${labels.customers.toLowerCase()}`}
          />
          <label className="flex items-center gap-2 text-[13px] text-muted">
            Sort
            <Select value={sort} onChange={(e) => setSort(e.target.value as typeof sort)} className="h-9 w-[170px]">
              <option value="NAME">Name</option>
              <option value="NUMBER">Number</option>
              <option value="NEWEST">Newest first</option>
              {memberships && <option value="ENDING">Ending soonest</option>}
              <option value="DUES">Owes the most</option>
            </Select>
          </label>
        </div>

        {list.isPending ? (
          <div className="space-y-2 p-5">{Array.from({ length: 6 }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
        ) : list.isError ? (
          <EmptyState title="The list couldn't load" body={errorMessage(list.error)} action={<Button onClick={() => list.refetch()}>Try again</Button>} />
        ) : list.data.items.length === 0 ? (
          <EmptyState
            icon={<Users className="h-5 w-5" />}
            title={q ? `Nobody matches “${q}”` : view === "ALL" ? `No ${labels.customers.toLowerCase()} yet` : "Nobody here right now"}
            body={!q && view === "ALL" ? `Add your first ${labels.customer.toLowerCase()}, or import a spreadsheet of them.` : undefined}
            action={!q && view === "ALL" && business.can("customers.manage") ? <Button variant="primary" onClick={() => setAdding(true)}>Add {labels.customer.toLowerCase()}</Button> : undefined}
          />
        ) : (
          <>
            <Table>
              <THead>
                <tr>
                  <TH className="w-16">#</TH>
                  <TH>Name</TH>
                  {memberships && <TH>{labels.plan}</TH>}
                  {memberships && <TH>Covered until</TH>}
                  <TH>Status</TH>
                  <TH className="text-right">Owes</TH>
                </tr>
              </THead>
              <tbody>
                {list.data.items.map((c) => {
                  const state = MEMBERSHIP_STATE[c.membershipState];
                  return (
                    <TR key={c.id} onClick={() => panel.open(c.id)} className="group">
                      <TD className="tabular-nums text-faint">{c.number}</TD>
                      <TD>
                        <button type="button" className="text-left font-medium text-ink group-hover:text-cobalt" onClick={() => panel.open(c.id)}>
                          {c.fullName}
                        </button>
                        {c.phone && <span className="block text-[12px] text-muted tabular-nums">{c.phone}</span>}
                      </TD>
                      {memberships && <TD className="text-muted">{c.planName ?? "—"}</TD>}
                      {memberships && <TD className="tabular-nums text-muted">{c.coveredUntil ? formatDate(c.coveredUntil) : c.lastEnded ? `Ended ${formatDate(c.lastEnded)}` : "—"}</TD>}
                      <TD>{c.status === "ARCHIVED" ? <Pill>Archived</Pill> : <Pill tone={state.tone}>{state.label}</Pill>}</TD>
                      <TD className="text-right tabular-nums">{c.balanceDue > 0 ? <span className="font-semibold text-bad">{formatMoney(c.balanceDue)}</span> : <span className="text-faint">—</span>}</TD>
                    </TR>
                  );
                })}
              </tbody>
            </Table>
            <Pagination page={page} size={PAGE_SIZE} total={list.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>

      <CustomerDialog open={adding} onOpenChange={setAdding} onSaved={(c) => panel.open(c.id)} />
      <ImportDialog open={importing} onOpenChange={setImporting} />
    </>
  );
}
