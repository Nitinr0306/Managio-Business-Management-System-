"use client";

import { Building2 } from "lucide-react";
import { useState } from "react";
import { SearchInput, Select } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill, Tag } from "@/components/ui/pill";
import { BUSINESS_TYPE_LABELS, MODULE_LABELS, type BusinessType, type ModuleKey } from "@/features/business/access";
import { daysAgo, formatInstantDate, formatNumber } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";
import { AdminQuery } from "./admin-shell";
import { useAdminBusinesses, type TenantRow, type TenantSort, type TenantView } from "./api";

const PAGE_SIZE = 25;
const VIEWS: { value: TenantView; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "ACTIVE", label: "Active" },
  { value: "DORMANT", label: "Dormant" },
  { value: "ARCHIVED", label: "Archived" },
];

/**
 * /admin/businesses: every business on Managio with its owner, size and last activity, so an administrator can
 * see who is thriving, who went quiet and whom to contact. It never shows a business's customers or money.
 */
export function BusinessesPage() {
  const [view, setView] = useState<TenantView>("ALL");
  const [type, setType] = useState<BusinessType | "">("");
  const [sort, setSort] = useState<TenantSort>("NEWEST");
  const [text, setText] = useState("");
  const [page, setPage] = useState(0);
  const q = useDebounced(text.trim(), 250);
  const list = useAdminBusinesses({ view, type: type || undefined, q: q || undefined, sort, page, size: PAGE_SIZE });

  return (
    <>
      <PageHeader title="Businesses" subtitle="Dormant means open but nothing recorded in the last 30 days." />
      <div className="mb-4">
        <Segmented
          label="Show"
          value={view}
          onChange={(v) => {
            setView(v);
            setPage(0);
          }}
          options={VIEWS}
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
            placeholder="Name, code or owner's email"
            aria-label="Search businesses"
          />
          <label className="flex items-center gap-2 text-[13px] text-muted">
            Type
            <Select
              value={type}
              onChange={(e) => {
                setType(e.target.value as BusinessType | "");
                setPage(0);
              }}
              className="h-9 w-[190px]"
            >
              <option value="">All types</option>
              {Object.entries(BUSINESS_TYPE_LABELS).map(([key, label]) => (
                <option key={key} value={key}>
                  {label}
                </option>
              ))}
            </Select>
          </label>
          <label className="flex items-center gap-2 text-[13px] text-muted">
            Sort
            <Select value={sort} onChange={(e) => setSort(e.target.value as TenantSort)} className="h-9 w-[170px]">
              <option value="NEWEST">Newest first</option>
              <option value="LAST_ACTIVE">Recently active</option>
              <option value="CUSTOMERS">Most customers</option>
              <option value="NAME">Name</option>
            </Select>
          </label>
        </div>
        <AdminQuery query={list}>
          {(data) =>
            data.items.length === 0 ? (
              <EmptyState icon={<Building2 className="h-5 w-5" />} title="No businesses match" body="Try another search or view." />
            ) : (
              <>
                <BusinessTable rows={data.items} />
                <Pagination page={page} size={PAGE_SIZE} total={data.totalItems} onPage={setPage} />
              </>
            )
          }
        </AdminQuery>
      </Panel>
    </>
  );
}

function BusinessTable({ rows }: { rows: TenantRow[] }) {
  return (
    <Table label="Businesses">
      <THead>
        <tr>
          <TH>Business</TH>
          <TH>Owner</TH>
          <TH className="text-right">Team</TH>
          <TH className="text-right">Customers</TH>
          <TH>Last activity</TH>
          <TH>Created</TH>
        </tr>
      </THead>
      <tbody>
        {rows.map((b) => (
          <TR key={b.id}>
            <TD className="py-2.5">
              <div className="flex items-center gap-2">
                <span className="font-medium text-ink">{b.name}</span>
                {b.archived && <Pill tone="neutral">Archived</Pill>}
              </div>
              <div className="text-[12px] text-muted">
                {BUSINESS_TYPE_LABELS[b.type as BusinessType] ?? b.type}
                {b.city && `, ${b.city}`} <span className="tabular-nums text-faint">{b.code}</span>
              </div>
              {b.modules.length > 0 && (
                <div className="mt-1 flex flex-wrap gap-1">
                  {b.modules.map((m) => (
                    <Tag key={m}>{MODULE_LABELS[m as ModuleKey]?.name ?? m}</Tag>
                  ))}
                </div>
              )}
            </TD>
            <TD className="py-2.5">
              {b.ownerName ? (
                <>
                  <div className="text-ink">{b.ownerName}</div>
                  <div className="text-[12px] text-muted">{b.ownerEmail}</div>
                </>
              ) : (
                <span className="text-muted">No owner</span>
              )}
            </TD>
            <TD className="text-right tabular-nums">{formatNumber(b.teamMembers)}</TD>
            <TD className="text-right tabular-nums">{formatNumber(b.customers)}</TD>
            <TD className="whitespace-nowrap">{daysAgo(b.lastActivityAt)}</TD>
            <TD className="whitespace-nowrap text-muted">{formatInstantDate(b.createdAt)}</TD>
          </TR>
        ))}
      </tbody>
    </Table>
  );
}
