"use client";

import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { ScrollText } from "lucide-react";
import { useState } from "react";
import { EmptyState, PageHeader, Panel, Skeleton } from "@/components/ui/layout";
import { Pagination } from "@/components/ui/pagination";
import { Tag } from "@/components/ui/pill";
import { businessKey, useBusiness } from "@/features/business/context";
import { api, unwrap } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { formatDateTime, humanize } from "@/lib/format";

const PAGE_SIZE = 50;

/** The activity log: who did what and when. Entries are written by the API and never edited. */
export function ActivityPage() {
  const business = useBusiness();
  const [page, setPage] = useState(0);
  const activity = useQuery({
    queryKey: businessKey(business.id, "activity", page),
    queryFn: () => unwrap(api.GET("/api/v1/businesses/{businessId}/activity", { params: { path: { businessId: business.id }, query: { page, size: PAGE_SIZE } } })),
    placeholderData: keepPreviousData,
  });

  return (
    <>
      <PageHeader title="Activity log" subtitle="Every change made in this business, newest first" />
      <Panel>
        {activity.isPending ? (
          <div className="space-y-2 p-5">{Array.from({ length: 8 }, (_, i) => <Skeleton key={i} className="h-8 w-full" />)}</div>
        ) : activity.isError ? (
          <EmptyState title="The activity log couldn't load" body={errorMessage(activity.error)} />
        ) : activity.data.items.length === 0 ? (
          <EmptyState icon={<ScrollText className="h-5 w-5" />} title="Nothing yet" />
        ) : (
          <>
            <ol className="divide-y divide-line">
              {activity.data.items.map((entry) => (
                <li key={entry.id} className="flex flex-wrap items-baseline gap-x-3 gap-y-1 px-5 py-2.5 text-[13.5px]">
                  <span className="w-36 shrink-0 tabular-nums text-faint">{formatDateTime(entry.createdAt)}</span>
                  <span className="font-medium text-ink">{entry.actorName ?? (entry.actorType === "SYSTEM" ? "Managio" : humanize(entry.actorType))}</span>
                  <span className="min-w-0 flex-1 text-muted">{entry.summary}</span>
                  {entry.entityType && <Tag>{humanize(entry.entityType)}</Tag>}
                </li>
              ))}
            </ol>
            <Pagination page={page} size={PAGE_SIZE} total={activity.data.totalItems} onPage={setPage} />
          </>
        )}
      </Panel>
    </>
  );
}
