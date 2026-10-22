"use client";

import { CircleCheck, Phone } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { EmptyState, PageHeader, Panel, Skeleton, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { useBusiness } from "@/features/business/context";
import { useCustomerPanel } from "@/features/customers/panel-context";
import { useRunway } from "@/features/reports/api";
import { RunwayChart } from "@/features/reports/runway-chart";
import { errorMessage } from "@/lib/api/errors";
import { formatDate, formatMoney, plural } from "@/lib/format";
import { MEMBERSHIP_ITEM_STATE, useMemberships, type MembershipView } from "./api";

const PAGE_SIZE = 25;

type Worklist = "ENDING" | "LAPSED" | "UPCOMING" | "ACTIVE" | "CANCELLED";
const WORKLISTS: Record<Worklist, { label: string; view: MembershipView; needsRenewal: boolean; empty: string }> = {
  ENDING: { label: "Ending this week", view: "ENDING_SOON", needsRenewal: true, empty: "Nobody's plan ends in the next 7 days without a renewal." },
  LAPSED: { label: "Lapsed, not renewed", view: "EXPIRED", needsRenewal: true, empty: "Nobody has lapsed without renewing." },
  UPCOMING: { label: "Starting later", view: "UPCOMING", needsRenewal: false, empty: "No plans are booked to start later." },
  ACTIVE: { label: "All active", view: "ACTIVE", needsRenewal: false, empty: "No active plans yet." },
  CANCELLED: { label: "Cancelled", view: "CANCELLED", needsRenewal: false, empty: "No cancelled plans." },
};

/** The renewal worklists (MEM-2, REP-4): who to call today, in the order their plans end. */
export function RenewalsPage() {
  const business = useBusiness();
  const panel = useCustomerPanel();
  const [list, setList] = useState<Worklist>("ENDING");
  const [page, setPage] = useState(0);
  const config = WORKLISTS[list];
  const memberships = useMemberships({ view: config.view, needsRenewal: config.needsRenewal, page, size: PAGE_SIZE });
  const runway = useRunway(30);

  return (
    <>
      <PageHeader
        title="Renewals"
        subtitle={runway.data ? `${plural(runway.data.lapsedLast30Days, "lapse")} in the last 30 days worth ${formatMoney(runway.data.lapsedValue)}` : undefined}
      />
      <Panel className="mb-5" bodyClassName="px-5 pb-5 pt-4">
        <h2 className="text-[15px] font-semibold">Next 30 days</h2>
        {runway.isPending ? <Skeleton className="mt-8 h-[170px] w-full" /> : <RunwayChart days={runway.data?.days ?? []} />}
      </Panel>

      <div className="mb-4">
        <Segmented
          label="Worklist"
          value={list}
          onChange={(value) => {
            setList(value);
            setPage(0);
          }}
          options={(Object.keys(WORKLISTS) as Worklist[]).map((key) => ({ value: key, label: WORKLISTS[key].label }))}
        />
      </div>

      <Panel>
        {memberships.isPending ? (
          <div className="space-y-2 p-5">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-9 w-full" />)}</div>
        ) : memberships.isError ? (
          <EmptyState title="The list couldn't load" body={errorMessage(memberships.error)} action={<Button onClick={() => memberships.refetch()}>Try again</Button>} />
        ) : memberships.data.items.length === 0 ? (
          <EmptyState icon={<CircleCheck className="h-5 w-5" />} title="All clear" body={config.empty} />
        ) : (
          <>
            <Table>
              <THead>
                <tr>
                  <TH>{business.labels.customer}</TH>
                  <TH>Phone</TH>
                  <TH>{business.labels.plan}</TH>
                  <TH>Ends</TH>
                  <TH>Status</TH>
                  <TH className="text-right">Price</TH>
                </tr>
              </THead>
              <tbody>
                {memberships.data.items.map((m) => (
                  <TR key={m.id} onClick={() => panel.open(m.customerId)}>
                    <TD>
                      <span className="font-medium text-ink">{m.customerName}</span>
                      <span className="ml-2 text-[12px] tabular-nums text-faint">#{m.customerNumber}</span>
                    </TD>
                    <TD>
                      {m.customerPhone ? (
                        <a href={`tel:${m.customerPhone}`} onClick={(e) => e.stopPropagation()} className="inline-flex items-center gap-1.5 tabular-nums text-cobalt hover:underline">
                          <Phone className="h-3.5 w-3.5" aria-hidden /> {m.customerPhone}
                        </a>
                      ) : (
                        <span className="text-faint">—</span>
                      )}
                    </TD>
                    <TD className="text-muted">{m.planName}</TD>
                    <TD className="tabular-nums">
                      {formatDate(m.endsOn)}
                      {m.state === "ENDING_SOON" && <span className="ml-2 text-[12px] text-due">{m.daysLeft === 0 ? "today" : `in ${m.daysLeft} days`}</span>}
                      {m.state === "EXPIRED" && <span className="ml-2 text-[12px] text-bad">{-m.daysLeft} days ago</span>}
                    </TD>
                    <TD><Pill tone={MEMBERSHIP_ITEM_STATE[m.state].tone}>{MEMBERSHIP_ITEM_STATE[m.state].label}</Pill></TD>
                    <TD className="text-right tabular-nums">{formatMoney(m.price)}</TD>
                  </TR>
                ))}
              </tbody>
            </Table>
            <Pagination page={page} size={PAGE_SIZE} total={memberships.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>
    </>
  );
}
