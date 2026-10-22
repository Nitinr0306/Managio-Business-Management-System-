"use client";

import { Details, PageHeader, Panel } from "@/components/ui/layout";
import { Pill } from "@/components/ui/pill";
import type { Schemas } from "@/lib/api/client";
import { formatDateTime, formatInstantDate, formatNumber } from "@/lib/format";
import { AdminQuery } from "./admin-shell";
import { useAdminHealth } from "./api";

/** /admin/health: is the service up, configured and being used safely? Refreshes every minute. */
export function HealthPage() {
  const health = useAdminHealth();
  return <AdminQuery query={health}>{(data) => <Health data={data} />}</AdminQuery>;
}

function bytes(value: number) {
  const units = ["bytes", "KB", "MB", "GB", "TB"];
  let size = value;
  let unit = 0;
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024;
    unit += 1;
  }
  return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`;
}

function uptime(seconds: number) {
  const days = Math.floor(seconds / 86_400);
  const hours = Math.floor((seconds % 86_400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  if (days > 0) return `${days} d ${hours} h`;
  if (hours > 0) return `${hours} h ${minutes} min`;
  return `${minutes} min`;
}

function Health({ data }: { data: Schemas["PlatformHealth"] }) {
  const { application: app, database: db, integrations, signIns, accounts } = data;
  return (
    <>
      <PageHeader title="System health" subtitle={`Checked ${formatDateTime(data.checkedAt)}. Refreshes every minute.`} />
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Panel title="Application" bodyClassName="p-5">
          <Details
            items={[
              { label: "Version", value: app.version ?? "Not recorded in this build" },
              { label: "Built", value: app.builtAt ? formatInstantDate(app.builtAt) : "—" },
              { label: "Running for", value: uptime(app.uptimeSeconds) },
              { label: "Memory in use", value: `${bytes(app.heapUsedBytes)} of ${bytes(app.heapMaxBytes)}` },
              { label: "Java", value: `${app.javaVersion}, ${app.processors} processors` },
            ]}
          />
        </Panel>
        <Panel
          title="Database"
          actions={db.migrationsHealthy ? <Pill tone="ok">Schema up to date</Pill> : <Pill tone="bad">A migration failed</Pill>}
          bodyClassName="p-5"
        >
          <Details
            items={[
              { label: "PostgreSQL", value: db.serverVersion },
              { label: "Size", value: bytes(db.sizeBytes) },
              { label: "Schema version", value: db.schemaVersion ?? "—" },
              { label: "Migrations applied", value: formatNumber(db.migrationsApplied) },
            ]}
          />
        </Panel>
        <Panel title="Integrations" bodyClassName="p-5">
          <Details
            items={[
              {
                label: "Email",
                value: integrations.emailDelivery ? (
                  <Pill tone="ok">Sending through Resend</Pill>
                ) : (
                  <Pill tone="due">Not sending</Pill>
                ),
              },
              {
                label: "Google sign-in",
                value: integrations.googleSignIn ? <Pill tone="ok">Configured</Pill> : <Pill tone="neutral">Off</Pill>,
              },
            ]}
          />
          {!integrations.emailDelivery && (
            <p className="mt-4 text-[12.5px] text-muted">
              Emails are only written to the API&apos;s log. Without email, people can&apos;t verify accounts, reset passwords or accept invitations. Set RESEND_API_KEY on the API.
            </p>
          )}
        </Panel>
        <Panel title="Sign-ins, last 24 hours" bodyClassName="p-5">
          <Details
            items={[
              { label: "Succeeded", value: formatNumber(signIns.succeeded24h) },
              {
                label: "Failed",
                value: signIns.failed24h > 0 ? <span className="text-due">{formatNumber(signIns.failed24h)}</span> : "0",
              },
              { label: "Locked out now", value: formatNumber(signIns.lockedAccounts) },
              { label: "Open sessions", value: formatNumber(signIns.openSessions) },
            ]}
          />
        </Panel>
        <Panel title="Accounts and invitations" bodyClassName="p-5" className="lg:col-span-2">
          <Details
            items={[
              { label: "Unverified for over 2 days", value: formatNumber(accounts.unverifiedOver48h) },
              { label: "Invitations waiting", value: formatNumber(accounts.pendingInvitations) },
              { label: "Invitations expired unanswered", value: formatNumber(accounts.expiredInvitations) },
            ]}
          />
        </Panel>
      </div>
    </>
  );
}
