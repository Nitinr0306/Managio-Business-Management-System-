"use client";

import { createContext, use, useMemo, useState, type ReactNode } from "react";
import { CustomerPanel } from "./customer-panel";

type PanelApi = { open: (customerId: number) => void; close: () => void; customerId: number | null };

const PanelContext = createContext<PanelApi | null>(null);

/** One customer panel for the whole business shell; anything can open it (search, tables, Today). */
export function CustomerPanelProvider({ children }: { children: ReactNode }) {
  const [customerId, setCustomerId] = useState<number | null>(null);
  const value = useMemo<PanelApi>(
    () => ({ open: setCustomerId, close: () => setCustomerId(null), customerId }),
    [customerId],
  );
  return (
    <PanelContext value={value}>
      {children}
      <CustomerPanel customerId={customerId} onClose={() => setCustomerId(null)} />
    </PanelContext>
  );
}

export function useCustomerPanel(): PanelApi {
  const panel = use(PanelContext);
  if (!panel) throw new Error("useCustomerPanel() must be used inside CustomerPanelProvider");
  return panel;
}
