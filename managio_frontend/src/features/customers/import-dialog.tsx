"use client";

import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Dialog, DialogBody, DialogFooter } from "@/components/ui/dialog";
import { FormError } from "@/components/ui/field";
import { useBusiness } from "@/features/business/context";
import { api, unwrap, type Schemas } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { plural } from "@/lib/format";
import { useRefreshCustomerData } from "./api";

/** Imports customers from a CSV file. Rows with problems are skipped and listed, so the file can be fixed and re-imported. */
export function ImportDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (open: boolean) => void }) {
  const business = useBusiness();
  return (
    <Dialog open={open} onOpenChange={onOpenChange} title={`Import ${business.labels.customers.toLowerCase()}`} description="From a CSV file, for example exported from Excel or Google Sheets.">
      <ImportForm onDone={() => onOpenChange(false)} />
    </Dialog>
  );
}

function ImportForm({ onDone }: { onDone: () => void }) {
  const business = useBusiness();
  const refresh = useRefreshCustomerData();
  const [file, setFile] = useState<File | null>(null);
  const [result, setResult] = useState<Schemas["ImportResult"] | null>(null);
  const upload = useMutation({
    mutationFn: (csv: File) =>
      unwrap(
        api.POST("/api/v1/businesses/{businessId}/customers/import", {
          params: { path: { businessId: business.id } },
          body: { file: csv as unknown as string },
          bodySerializer: () => {
            const form = new FormData();
            form.append("file", csv);
            return form;
          },
        }),
      ),
    onSuccess: async (data) => {
      setResult(data);
      await refresh();
    },
  });

  if (result) {
    return (
      <>
        <DialogBody className="space-y-3 text-[13.5px]">
          <p>
            Added <strong>{plural(result.added, business.labels.customer.toLowerCase(), business.labels.customers.toLowerCase())}</strong>
            {result.skipped > 0 && <>, skipped {plural(result.skipped, "row")}</>}.
          </p>
          {result.errors.length > 0 && (
            <ul className="max-h-[240px] space-y-1 overflow-y-auto rounded-[var(--radius-control)] border border-line bg-canvas p-3 text-[12.5px]">
              {result.errors.map((e) => (
                <li key={`${e.row}-${e.message}`}>
                  <span className="font-medium tabular-nums">Row {e.row}:</span> {e.message}
                </li>
              ))}
            </ul>
          )}
        </DialogBody>
        <DialogFooter>
          <Button variant="primary" onClick={onDone}>Done</Button>
        </DialogFooter>
      </>
    );
  }

  return (
    <>
      <DialogBody className="space-y-4 text-[13.5px]">
        <FormError message={upload.isError ? errorMessage(upload.error) : null} />
        <p className="text-muted">
          Put column names in the first row. <strong className="text-ink">full_name</strong> is required; you can also include phone, email,
          date_of_birth (YYYY-MM-DD), gender, address, guardian_name, guardian_phone, notes and joined_on.
        </p>
        <input
          type="file"
          accept=".csv,text/csv"
          aria-label="CSV file"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          className="block w-full text-[13px] file:mr-3 file:h-9 file:rounded-[var(--radius-control)] file:border file:border-line file:bg-surface file:px-3 file:text-[13px] file:font-medium hover:file:bg-canvas"
        />
      </DialogBody>
      <DialogFooter>
        <Button onClick={onDone}>Cancel</Button>
        <Button variant="primary" disabled={!file} loading={upload.isPending} onClick={() => file && upload.mutate(file)}>
          Import
        </Button>
      </DialogFooter>
    </>
  );
}
