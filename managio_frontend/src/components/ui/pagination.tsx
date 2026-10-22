import { ChevronLeft, ChevronRight } from "lucide-react";
import { formatNumber } from "@/lib/format";
import { Button } from "./button";

/** "41–60 of 212" with previous and next. Pages are zero-based, like the API. */
export function Pagination({ page, size, total, onPage }: { page: number; size: number; total: number; onPage: (page: number) => void }) {
  if (total <= size) return null;
  const from = page * size + 1;
  const to = Math.min(total, (page + 1) * size);
  const last = Math.ceil(total / size) - 1;
  return (
    <div className="flex items-center justify-between gap-3 border-t border-line px-5 py-2.5 text-[13px] text-muted">
      <span className="tabular-nums">
        {formatNumber(from)}–{formatNumber(to)} of {formatNumber(total)}
      </span>
      <div className="flex gap-1">
        <Button size="icon" variant="ghost" aria-label="Previous page" disabled={page === 0} onClick={() => onPage(page - 1)}>
          <ChevronLeft className="h-4 w-4" />
        </Button>
        <Button size="icon" variant="ghost" aria-label="Next page" disabled={page >= last} onClick={() => onPage(page + 1)}>
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
