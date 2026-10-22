"use client";

import { UserRound } from "lucide-react";
import { useState } from "react";
import { SearchInput } from "@/components/ui/field";
import { EmptyState, PageHeader, Panel, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { Segmented } from "@/components/ui/menu";
import { Pagination } from "@/components/ui/pagination";
import { Pill } from "@/components/ui/pill";
import { daysAgo, formatInstantDate, formatNumber } from "@/lib/format";
import { useDebounced } from "@/lib/use-debounced";
import { AdminQuery } from "./admin-shell";
import { useAdminPeople, type PeopleView, type PlatformUserRow } from "./api";

const PAGE_SIZE = 25;
const VIEWS: { value: PeopleView; label: string }[] = [
  { value: "ALL", label: "Everyone" },
  { value: "UNVERIFIED", label: "Unverified email" },
  { value: "WITHOUT_BUSINESS", label: "No business yet" },
  { value: "LOCKED", label: "Locked out" },
];

/**
 * /admin/people: owner and staff accounts, for answering "I can't sign in" and spotting people who signed up
 * but never got started. Customers of businesses are not accounts and never appear here.
 */
export function PeoplePage() {
  const [view, setView] = useState<PeopleView>("ALL");
  const [text, setText] = useState("");
  const [page, setPage] = useState(0);
  const q = useDebounced(text.trim(), 250);
  const list = useAdminPeople({ view, q: q || undefined, page, size: PAGE_SIZE });

  return (
    <>
      <PageHeader title="People" subtitle="Owner and staff accounts. Last active is the last time they signed in or used the app." />
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
        <div className="border-b border-line px-5 py-3">
          <SearchInput
            value={text}
            onChange={(e) => {
              setText(e.target.value);
              setPage(0);
            }}
            placeholder="Name or email"
            aria-label="Search people"
          />
        </div>
        <AdminQuery query={list}>
          {(data) =>
            data.items.length === 0 ? (
              <EmptyState icon={<UserRound className="h-5 w-5" />} title="Nobody matches" body="Try another search or view." />
            ) : (
              <>
                <PeopleTable rows={data.items} />
                <Pagination page={page} size={PAGE_SIZE} total={data.totalItems} onPage={setPage} />
              </>
            )
          }
        </AdminQuery>
      </Panel>
    </>
  );
}

function signInMethods(person: PlatformUserRow) {
  const methods = [person.passwordSignIn && "Password", person.googleSignIn && "Google"].filter(Boolean);
  return methods.join(" and ") || "None";
}

function PeopleTable({ rows }: { rows: PlatformUserRow[] }) {
  return (
    <Table label="People">
      <THead>
        <tr>
          <TH>Person</TH>
          <TH>Status</TH>
          <TH>Signs in with</TH>
          <TH className="text-right">Businesses</TH>
          <TH>Last active</TH>
          <TH>Joined</TH>
        </tr>
      </THead>
      <tbody>
        {rows.map((p) => (
          <TR key={p.id}>
            <TD className="py-2.5">
              <div className="font-medium text-ink">{p.fullName}</div>
              <div className="text-[12px] text-muted">{p.email}</div>
            </TD>
            <TD>
              <div className="flex flex-wrap gap-1">
                {p.disabled ? (
                  <Pill tone="bad">Disabled</Pill>
                ) : p.locked ? (
                  <Pill tone="bad">Locked out</Pill>
                ) : p.emailVerified ? (
                  <Pill tone="ok">Verified</Pill>
                ) : (
                  <Pill tone="due">Unverified</Pill>
                )}
              </div>
            </TD>
            <TD className="whitespace-nowrap text-muted">{signInMethods(p)}</TD>
            <TD className="text-right tabular-nums">{formatNumber(p.businesses)}</TD>
            <TD className="whitespace-nowrap">{daysAgo(p.lastActiveAt)}</TD>
            <TD className="whitespace-nowrap text-muted">{formatInstantDate(p.createdAt)}</TD>
          </TR>
        ))}
      </tbody>
    </Table>
  );
}
