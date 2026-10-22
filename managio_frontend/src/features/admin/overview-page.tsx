"use client";

import { KpiStrip, PageHeader, Panel, Table, TD, TH, THead, TR } from "@/components/ui/layout";
import { BUSINESS_TYPE_LABELS, MODULE_LABELS, type BusinessType, type ModuleKey } from "@/features/business/access";
import type { Schemas } from "@/lib/api/client";
import { formatDateTime, formatNumber } from "@/lib/format";
import { AdminQuery } from "./admin-shell";
import { FEATURE_LABELS, useAdminOverview } from "./api";
import { ActivityChart, GrowthChart, ShareList } from "./charts";

/** /admin: how Managio is being used, from real data across every business (D-028). */
export function OverviewPage() {
  const overview = useAdminOverview();
  return <AdminQuery query={overview}>{(data) => <Overview data={data} />}</AdminQuery>;
}

function Overview({ data }: { data: Schemas["PlatformOverview"] }) {
  const t = data.totals;
  return (
    <>
      <PageHeader title="Platform overview" subtitle={`Figures as of ${formatDateTime(data.generatedAt)}. Days and weeks are counted in India time.`} />
      <KpiStrip
        items={[
          { label: "Businesses", value: formatNumber(t.businesses), note: `${formatNumber(t.newBusinesses30d)} new in 30 days` },
          {
            label: "Active businesses, 7 days",
            value: formatNumber(t.activeBusinesses7d),
            note: `${formatNumber(t.activeBusinesses30d)} in 30 days`,
            tone: t.activeBusinesses7d > 0 ? "ok" : "muted",
          },
          { label: "Accounts", value: formatNumber(t.users), note: `${formatNumber(t.newUsers30d)} new in 30 days` },
          {
            label: "People active today",
            value: formatNumber(t.activeUsers1d),
            note: `${formatNumber(t.activeUsers7d)} this week, ${formatNumber(t.activeUsers30d)} in 30 days`,
          },
          {
            label: "Customers managed",
            value: formatNumber(t.customers),
            note: `${formatNumber(t.portalCustomers)} can use the portal`,
          },
        ]}
      />
      <p className="mt-2 text-[12px] text-muted">
        A business is active when someone recorded something in it. A person is active when they signed in or used the app.
      </p>

      <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <Panel title="Sign-ups, last 12 weeks" bodyClassName="px-5 pb-5 pt-8">
          <GrowthChart weeks={data.growth} />
        </Panel>
        <Panel title="Activation" bodyClassName="p-5">
          <Activation funnel={data.activation} />
        </Panel>
      </div>

      <Panel title="Activity, last 30 days" className="mt-6" bodyClassName="px-5 pb-5 pt-8">
        <ActivityChart days={data.activity} />
        <p className="mt-3 text-[12px] text-muted">Each bar is the number of businesses that recorded something that day.</p>
      </Panel>

      <div className="mt-6 grid grid-cols-1 items-start gap-6 lg:grid-cols-3">
        <Panel title="Business types" bodyClassName="p-5">
          <ShareList
            unit="businesses"
            total={t.businesses}
            items={data.businessTypes.map((type) => ({
              label: BUSINESS_TYPE_LABELS[type.key as BusinessType] ?? type.key,
              count: type.count,
            }))}
          />
        </Panel>
        <Panel title="Modules switched on" bodyClassName="p-5">
          <ShareList
            unit="businesses"
            total={t.businesses}
            items={data.modules.map((module) => ({
              label: MODULE_LABELS[module.key as ModuleKey]?.name ?? module.key,
              count: module.count,
            }))}
          />
        </Panel>
        <Panel title="Feature use, last 30 days" bodyClassName="">
          <FeatureTable features={data.features} />
        </Panel>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-6 sm:grid-cols-3">
        <Figure label="Team seats" value={t.teamSeats} note="Owners and staff across all businesses" />
        <Figure label="Unverified accounts" value={t.unverifiedUsers} note="Haven't confirmed their email yet" />
        <Figure label="Archived businesses" value={t.archivedBusinesses} note="Not counted above" />
      </div>
    </>
  );
}

const STEPS: { key: keyof Schemas["ActivationFunnel"]; label: string }[] = [
  { key: "created", label: "Created a business" },
  { key: "addedCustomer", label: "Added a customer" },
  { key: "recordedPayment", label: "Recorded a payment" },
  { key: "addedTeammate", label: "Added a teammate" },
  { key: "activeLast7Days", label: "Active in the last 7 days" },
];

/** How far open businesses have got: each step as a share of the businesses created. */
function Activation({ funnel }: { funnel: Schemas["ActivationFunnel"] }) {
  return (
    <>
      <ShareList total={funnel.created} items={STEPS.map((step) => ({ label: step.label, count: funnel[step.key] }))} />
      <p className="mt-4 text-[12px] text-muted">Share of open businesses that have done each at least once.</p>
    </>
  );
}

function FeatureTable({ features }: { features: Schemas["FeatureUsage"][] }) {
  if (features.length === 0) {
    return <p className="px-5 py-6 text-[13.5px] text-muted">Nothing was recorded in the last 30 days.</p>;
  }
  return (
    <Table label="Feature use, last 30 days">
      <THead>
        <tr>
          <TH>Area</TH>
          <TH className="text-right">Businesses</TH>
          <TH className="text-right">Records</TH>
        </tr>
      </THead>
      <tbody>
        {features.map((f) => (
          <TR key={f.area}>
            <TD>{FEATURE_LABELS[f.area] ?? f.area}</TD>
            <TD className="text-right tabular-nums">{formatNumber(f.businesses)}</TD>
            <TD className="text-right tabular-nums text-muted">{formatNumber(f.events)}</TD>
          </TR>
        ))}
      </tbody>
    </Table>
  );
}

function Figure({ label, value, note }: { label: string; value: number; note: string }) {
  return (
    <div className="rounded-[var(--radius-panel)] border border-line bg-surface px-5 py-4">
      <div className="text-[12.5px] text-muted">{label}</div>
      <div className="mt-1 text-[20px] font-semibold tabular-nums text-ink">{formatNumber(value)}</div>
      <div className="mt-0.5 text-[12px] text-muted">{note}</div>
    </div>
  );
}
